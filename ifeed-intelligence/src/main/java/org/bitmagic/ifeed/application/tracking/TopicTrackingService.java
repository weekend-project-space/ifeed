package org.bitmagic.ifeed.application.tracking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.api.tracking.TopicTrackConfigRequest;
import org.bitmagic.ifeed.api.tracking.TopicTrackCursorPage;
import org.bitmagic.ifeed.api.tracking.TopicTrackResponse;
import org.bitmagic.ifeed.api.tracking.TopicTrackScanResponse;
import org.bitmagic.ifeed.api.tracking.TopicTrackSuggestionResponse;
import org.bitmagic.ifeed.api.tracking.TopicTrackSummaryResponse;
import org.bitmagic.ifeed.config.properties.TopicTrackingProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.tracking.TopicKeyword;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrack;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrackMatch;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrackSummary;
import org.bitmagic.ifeed.domain.model.value.FeedCategory;
import org.bitmagic.ifeed.domain.record.ArticleTitle;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.tracking.TopicTrackMatchRepository;
import org.bitmagic.ifeed.domain.repository.tracking.TopicTrackRepository;
import org.bitmagic.ifeed.domain.repository.tracking.TopicTrackSubscriptionRepository;
import org.bitmagic.ifeed.domain.repository.tracking.TopicTrackSummaryRepository;
import org.bitmagic.ifeed.domain.service.UserService;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.infrastructure.text.search.tracking.TopicArticleCandidate;
import org.bitmagic.ifeed.infrastructure.text.search.tracking.TopicArticleSearchRequest;
import org.bitmagic.ifeed.infrastructure.text.search.tracking.TopicArticleSearchStore;
import org.bitmagic.ifeed.infrastructure.tracking.TopicTrackEvaluationStore;
import org.bitmagic.ifeed.infrastructure.tracking.TopicTrackKeywordLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 话题跟踪核心应用服务
 * <p>
 * 负责话题的智能配置建议、话题生命周期管理（创建/修改/启停/关注）、
 * 异步定时扫描编排、全文检索候选过滤、向量聚类以及大模型总结持久化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicTrackingService {

    /** 单个话题允许关联的最大订阅源数量 */
    private static final int MAX_SOURCES = 30;
    /** 包含词或屏蔽词单组最大词条数量 */
    private static final int MAX_TERMS = 30;
    /** 单个关键词词条的最大字符长度 */
    private static final int MAX_TERM_LENGTH = 80;
    /** 总结卡片默认内嵌展示的关联文章预览数量 */
    private static final int PREVIEW_ARTICLE_LIMIT = 6;
    /** 扫描任务分布式租约时长（秒），防止任务异常挂死后无法重入 */
    private static final long LEASE_SECONDS = 900L;

    private final TopicTrackRepository topicTrackRepository;
    private final TopicTrackKeywordLock topicTrackKeywordLock;
    private final TopicTrackMatchRepository matchRepository;
    private final TopicTrackSummaryRepository summaryRepository;
    private final TopicTrackSubscriptionRepository subscriptionRepository;
    private final ArticleRepository articleRepository;
    private final TopicTrackEmbeddingService embeddingService;
    private final TopicArticleSearchStore topicArticleSearchStore;
    private final EntityManager entityManager;
    private final FeedRepository feedRepository;
    private final UserService userService;
    private final TopicTrackAiService aiService;
    private final ObjectMapper objectMapper;
    private final TopicTrackingProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final TopicTrackEvaluationStore evaluationStore;

    /**
     * 话题后台扫描专用线程池（2 核心线程，队列容量 64）
     * 队列满时抛出 RejectedExecutionException，交由定时调度器在下一周期补偿重试
     */
    private final ThreadPoolExecutor scanExecutor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64), runnable -> {
        Thread thread = new Thread(runnable, "topic-scan");
        thread.setDaemon(true);
        return thread;
    });

    @PreDestroy
    public void shutdown() {
        scanExecutor.shutdownNow();
    }

    /**
     * 根据用户输入的需求生成话题跟踪配置建议
     */
    public TopicTrackSuggestionResponse suggest(Integer userId, String keyword) {
        // 1. 校验需求并标准化关键词，相同需求优先复用已有公用话题
        String normalizedKeyword = validateKeyword(keyword);
        Optional<TopicTrack> existing = topicTrackRepository.findByNormalizedKeyword(normalizedKeyword);
        if (existing.isPresent()) {
            // 2. 已有话题直接返回配置和关注状态，不调用 AI，也不覆盖原配置
            TopicTrackResponse topic = toResponse(existing.get(), userId);
            return new TopicTrackSuggestionResponse(topic.includeKeywords(), topic.blockedKeywords(), topic.sources(),
                    topic.categories(), topic.relevancePrompt(), topic);
        }

        // 3. 从数据库获取当前有效站点分类，排除无效分类和 "all"
        List<String> availableSourceCategories = feedRepository.countByCategory().stream()
                .map(row -> (String) row[0]).filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(FeedCategory::isValidCode).filter(code -> !code.equals("all")).distinct().sorted().toList();

        // 4. 调用 AI 生成扩展关键词、屏蔽词、站点分类和总结要求
        TopicTrackAiService.Suggestion suggestion = aiService.suggest(keyword.trim(), availableSourceCategories);
        Pageable sourcePage = PageRequest.of(0, properties.getMaxSourcesPerCategory(),
                Sort.by(Sort.Order.desc("featured"), Sort.Order.asc("id")));

        // 5. 按 AI 推荐的站点分类查询真实订阅源，优先推荐 featured 站点，总数不超过上限
        List<Feed> recommendedFeeds = suggestion.sourceCategories().stream()
                .filter(availableSourceCategories::contains)
                .distinct().limit(properties.getMaxCategories())
                .flatMap(category -> feedRepository.searchByQueryAndCategory("", category, sourcePage).stream())
                .distinct().limit(MAX_SOURCES).toList();

        // 6. 返回可编辑的跟踪配置方案，待用户在前端确认后再正式创建
        return new TopicTrackSuggestionResponse(suggestion.includeKeywords(), suggestion.blockedKeywords(),
                recommendedFeeds.stream().map(this::sourceOption).toList(),
                suggestion.categories(), suggestion.relevancePrompt(), null);
    }

    /**
     * 创建新的跟踪话题（若并发创建了相同关键词的话题，则自动转为关注已有话题）
     */
    @Transactional
    public CreationResult create(Integer userId, TopicTrackConfigRequest request) {
        User user = ensureUser(userId);
        String normalizedKeyword = validateKeyword(request == null ? null : request.keyword());
        topicTrackKeywordLock.lockUser(userId);
        topicTrackKeywordLock.lock(normalizedKeyword);

        Optional<TopicTrack> existing = topicTrackRepository.findByNormalizedKeyword(normalizedKeyword);
        if (existing.isPresent()) {
            TopicTrack track = existing.get();
            if (!track.isEnabled()) throw new ApiException(HttpStatus.CONFLICT, "TOPIC_TRACK_DISABLED");
            addSubscription(userId, track, user);
            return new CreationResult(toResponse(track, userId), false);
        }

        int maxCreatedTopics = user.getCurrentPlan().getMaxCreatedMixFeed();
        if (topicTrackRepository.countByOwnerId(userId) >= maxCreatedTopics) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOPIC_CREATE_LIMIT_EXCEEDED");
        }
        checkSubscriptionLimit(userId, user);

        TopicTrack track = new TopicTrack();
        track.setOwnerId(userId);
        apply(track, request);
        track.setScanStatus(track.isEnabled() ? "QUEUED" : "DISABLED");
        TopicTrack saved = topicTrackRepository.save(track);
        subscriptionRepository.subscribe(saved.getId(), userId);

        if (saved.isEnabled()) triggerScanAfterCommit(saved.getId());
        return new CreationResult(toResponse(saved, userId), true);
    }

    /**
     * 更新话题配置（仅话题创建者可操作）
     */
    @Transactional
    public TopicTrackResponse update(Integer userId, UUID topicId, TopicTrackConfigRequest request) {
        String normalizedKeyword = validateKeyword(request == null ? null : request.keyword());
        topicTrackKeywordLock.lock(normalizedKeyword);

        TopicTrack track = topicTrackRepository.findByTopicUidAndOwnerId(topicId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOPIC_TRACK_NOT_FOUND"));

        topicTrackRepository.findByNormalizedKeyword(normalizedKeyword)
                .filter(existing -> !existing.getId().equals(track.getId()))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "TOPIC_KEYWORD_EXISTS");
                });

        ScanConfig previous = scanConfig(track);
        boolean previouslyEnabled = track.isEnabled();
        apply(track, request);
        boolean changed = !previous.equals(scanConfig(track));

        if (changed) track.setConfigVersion(track.getConfigVersion() + 1);

        boolean scanRequired = track.isEnabled() && (changed || !previouslyEnabled);
        if (!track.isEnabled()) {
            track.setScanStatus("DISABLED");
        } else if (scanRequired) {
            track.setScanStatus("QUEUED");
            track.setScanError(null);
        }

        TopicTrack saved = topicTrackRepository.save(track);
        if (scanRequired) triggerScanAfterCommit(saved.getId());
        return toResponse(saved, userId);
    }

    /**
     * 分页查询公共话题列表
     */
    @Transactional(readOnly = true)
    public Page<TopicTrackResponse> listTracks(Integer userId, Pageable pageable) {
        return toPageResponse(topicTrackRepository.findAll(stablePage(pageable, "createdAt")), userId);
    }

    /**
     * 分页查询当前用户已关注的话题列表
     */
    @Transactional(readOnly = true)
    public Page<TopicTrackResponse> listFollowedTracks(Integer userId, Pageable pageable) {
        ensureUser(userId);
        return toPageResponse(subscriptionRepository.findTopicsByUserId(userId, stablePage(pageable, "createdAt")), userId);
    }

    /**
     * 游标分页查询话题总结流
     */
    @Transactional(readOnly = true)
    public TopicTrackCursorPage<TopicTrackSummaryResponse> listSummaries(Integer userId, UUID topicUid,
                                                                         boolean following, UUID cursor, int size) {
        if (topicUid != null && topicTrackRepository.findByTopicUid(topicUid).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TOPIC_TRACK_NOT_FOUND");
        }
        if (following) {
            ensureUser(userId);
        }
        var anchor = cursor == null ? null : summaryRepository.findActiveSummary(cursor)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_CURSOR"));
        if (anchor != null && topicUid != null && !topicUid.equals(anchor.getTopicUid())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_CURSOR");
        }
        int limit = Math.clamp(size, 1, 100);
        var rows = summaryRepository.findSummaries(topicUid, following ? userId : null,
                anchor == null ? null : anchor.getCreatedAt(), anchor == null ? null : anchor.getId(),
                PageRequest.of(0, limit + 1));
        boolean hasNext = rows.size() > limit;
        var content = rows.stream().limit(limit).map(this::toSummaryResponse).toList();
        return new TopicTrackCursorPage<>(content, hasNext ? content.getLast().summaryId() : null, hasNext);
    }

    /**
     * 游标分页加载某条总结下关联的全部文章列表
     */
    @Transactional(readOnly = true)
    public TopicTrackCursorPage<TopicTrackSummaryResponse.ArticlePreview> listSummaryArticles(UUID summaryUid,
                                                                                              UUID cursor, int size) {
        var summary = summaryRepository.findActiveSummary(summaryUid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOPIC_SUMMARY_NOT_FOUND"));
        Long afterId = cursor == null ? 0L : summaryRepository.findArticleCursor(summary.getId(), cursor)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_CURSOR"));
        int limit = Math.clamp(size, 1, 100);
        var rows = summaryRepository.findArticles(summary.getId(), afterId, PageRequest.of(0, limit + 1));
        boolean hasNext = rows.size() > limit;
        var content = rows.stream().limit(limit).map(this::toArticlePreview).toList();
        return new TopicTrackCursorPage<>(content, hasNext ? content.getLast().articleId() : null, hasNext);
    }

    /**
     * 获取单个话题详情
     */
    @Transactional(readOnly = true)
    public TopicTrackResponse getTopic(Integer userId, UUID topicUid) {
        return toResponse(findTopic(topicUid), userId);
    }

    /**
     * 返回前端表单的配置上限参数
     */
    public Map<String, Integer> limits() {
        return Map.of(
                "maxCategories", properties.getMaxCategories(),
                "maxSources", MAX_SOURCES,
                "maxTerms", MAX_TERMS,
                "maxTermLength", MAX_TERM_LENGTH
        );
    }

    /**
     * 批量轮询话题的扫描运行状态
     */
    @Transactional(readOnly = true)
    public List<TopicTrackScanResponse> scanStates(List<UUID> topicIds) {
        if (topicIds == null || topicIds.isEmpty() || topicIds.size() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_IDS");
        }
        return topicTrackRepository.scanStates(topicIds);
    }

    private Pageable stablePage(Pageable pageable, String order) {
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100),
                Sort.by(Sort.Order.desc(order), Sort.Order.desc("id")));
    }

    /**
     * 关注指定话题
     */
    @Transactional
    public TopicTrackResponse subscribe(Integer userId, UUID topicUid) {
        User user = ensureUser(userId);
        topicTrackKeywordLock.lockUser(userId);
        TopicTrack track = findTopic(topicUid);
        if (!track.isEnabled()) throw new ApiException(HttpStatus.CONFLICT, "TOPIC_TRACK_DISABLED");
        addSubscription(userId, track, user);
        return toResponse(track, userId);
    }

    /**
     * 取消关注指定话题
     */
    @Transactional
    public TopicTrackResponse unsubscribe(Integer userId, UUID topicUid) {
        ensureUser(userId);
        TopicTrack track = findTopic(topicUid);
        subscriptionRepository.deleteByTopicTrack_IdAndUserId(track.getId(), userId);
        return toResponse(track, userId);
    }

    /**
     * 停用话题定时跟踪
     */
    @Transactional
    public TopicTrackResponse disable(Integer userId, UUID topicUid) {
        TopicTrack track = topicTrackRepository.findByTopicUidAndOwnerId(topicUid, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOPIC_TRACK_NOT_FOUND"));
        track.setEnabled(false);
        track.setScanStatus("DISABLED");
        return toResponse(topicTrackRepository.save(track), userId);
    }

    /**
     * 查询所有启用中的话题主键 ID
     */
    @Transactional(readOnly = true)
    public List<Long> findEnabledTopicIds() {
        return topicTrackRepository.findEnabledTopicIds();
    }

    /**
     * 执行单个话题的完整扫描生命周期
     */
    public void scanTopic(Long topicId) {
        UUID token = UUID.randomUUID();
        long startTimeMs = System.currentTimeMillis();

        // 第一阶段：抢占扫描租约
        TopicTrack track = transactionTemplate.execute(status -> {
            TopicTrack current = topicTrackRepository.findForScan(topicId).orElse(null);
            Instant now = Instant.now();
            if (current == null) {
                log.debug("[TopicScan] 跳过扫描: 话题不存在, topicId={}", topicId);
                return null;
            }
            if (!current.isEnabled()) {
                log.debug("[TopicScan] 跳过扫描: 话题已停用, topicId={}, name='{}'", topicId, current.getName());
                return null;
            }
            if (current.getScanToken() != null && current.getScanLeaseUntil() != null
                    && current.getScanLeaseUntil().isAfter(now)) {
                log.info("[TopicScan] 跳过扫描: 话题已有运行中的任务, topicId={}, name='{}', activeToken={}, leaseUntil={}",
                        topicId, current.getName(), current.getScanToken(), current.getScanLeaseUntil());
                return null;
            }
            current.setScanToken(token);
            current.setScanLeaseUntil(now.plusSeconds(LEASE_SECONDS));
            current.setScanStatus("RUNNING");
            current.setScanStartedAt(now);
            current.setScanFinishedAt(null);
            current.setScanError(null);
            topicTrackRepository.saveAndFlush(current);
            entityManager.detach(current);
            return current;
        });
        if (track == null) return;

        log.info("[TopicScan] >>> 开始扫描话题: topicId={}, uid={}, name='{}', keyword='{}', configVer={}, sources={}, token={}",
                track.getId(), track.getTopicUid(), track.getName(), track.getKeyword(),
                track.getConfigVersion(), track.getSourceIds().size(), token);

        boolean succeeded = false;
        try {
            // 第二阶段：执行核心扫描、聚类与总结
            succeeded = scanInternal(track, token);
        } catch (RuntimeException exception) {
            log.error("[TopicScan] 扫描过程发生未捕获异常: topicId={}, name='{}', token={}",
                    track.getId(), track.getName(), token, exception);
            throw exception;
        } finally {
            // 第三阶段：核对租约令牌、结算扫描状态，并检查期间是否发生过配置变更
            boolean completed = succeeded;
            Boolean rescan = transactionTemplate.execute(status -> {
                TopicTrack current = topicTrackRepository.findForScan(topicId).orElse(null);
                if (current == null || !token.equals(current.getScanToken())) {
                    log.warn("[TopicScan] 扫描结束时租约已变更或话题已删除: topicId={}, token={}", topicId, token);
                    return false;
                }
                boolean changed = current.getConfigVersion() != track.getConfigVersion();
                String finalStatus = !current.isEnabled() ? "DISABLED" : changed ? "QUEUED" : completed ? "SUCCEEDED" : "FAILED";
                current.setScanToken(null);
                current.setScanLeaseUntil(null);
                current.setScanFinishedAt(Instant.now());
                current.setScanStatus(finalStatus);
                current.setScanError(completed || changed || !current.isEnabled() ? null : "部分内容处理失败，系统将在下次扫描重试");
                if (completed && !changed && current.isEnabled()) current.setLastRunAt(track.getScanStartedAt());
                topicTrackRepository.save(current);

                long elapsedMs = System.currentTimeMillis() - startTimeMs;
                log.info("[TopicScan] <<< 扫描结算完成: topicId={}, name='{}', finalStatus={}, configChanged={}, 总耗时={}ms",
                        topicId, current.getName(), finalStatus, changed, elapsedMs);
                return changed && current.isEnabled();
            });
            // 若扫描执行期间用户修改了话题配置，立即追加一轮新扫描
            if (Boolean.TRUE.equals(rescan)) {
                log.info("[TopicScan] 检测到扫描期间配置版本升级，立即触发新一轮重扫: topicId={}", topicId);
                triggerScanAfterCommit(topicId);
            }
        }
    }

    /**
     * 在当前事务提交后异步投递扫描任务
     */
    public void triggerScanAfterCommit(Long topicId) {
        Runnable task = () -> {
            try {
                log.debug("[TopicScan] 投递异步扫描任务至线程池: topicId={}, activeThreads={}, queueSize={}",
                        topicId, scanExecutor.getActiveCount(), scanExecutor.getQueue().size());
                CompletableFuture.runAsync(() -> {
                    try {
                        scanTopic(topicId);
                    } catch (RuntimeException exception) {
                        log.warn("[TopicScan] 异步扫描任务执行异常: topicId={}", topicId, exception);
                    }
                }, scanExecutor);
            } catch (RejectedExecutionException exception) {
                log.warn("[TopicScan] 扫描线程池队列已满，等待下次定时调度重试: topicId={}", topicId);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * 校验当前扫描任务租约是否依然有效且配置版本未变，校验通过后自动续期租约
     */
    private TopicTrack activeScan(TopicTrack snapshot, UUID token) {
        TopicTrack current = topicTrackRepository.findForScan(snapshot.getId()).orElseThrow();
        if (!current.isEnabled() || current.getConfigVersion() != snapshot.getConfigVersion()
                || !token.equals(current.getScanToken()) || current.getScanLeaseUntil() == null
                || !current.getScanLeaseUntil().isAfter(Instant.now())) {
            log.warn("[TopicScan] 扫描租约失效或配置已变更，中止当前任务: topicId={}, name='{}', snapshotVer={}, currentVer={}, enabled={}",
                    snapshot.getId(), snapshot.getName(), snapshot.getConfigVersion(), current.getConfigVersion(), current.isEnabled());
            throw new IllegalStateException("Topic scan expired or configuration changed");
        }
        current.setScanLeaseUntil(Instant.now().plusSeconds(LEASE_SECONDS));
        return current;
    }

    /**
     * 发送扫描心跳（续期租约并快速失败中止过期任务）
     */
    private void heartbeat(TopicTrack track, UUID token) {
        transactionTemplate.executeWithoutResult(status -> activeScan(track, token));
    }

    /**
     * 在独立短事务中按传入 ID 顺序加载文章实体
     */
    private List<Article> loadArticles(List<Long> ids) {
        return transactionTemplate.execute(status -> findOrderedArticles(ids));
    }

    /**
     * 批量查询文章并严格保持传入的 ID 顺序
     */
    private List<Article> findOrderedArticles(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        Map<Long, Article> byId = articleRepository.findAllWithFeedByIdIn(ids).stream()
                .collect(Collectors.toMap(Article::getId, article -> article));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    /**
     * 扫描核心流程：全文检索候选集 -> 过滤已评估文章 -> 向量化与在线聚类 -> 按簇生成总结
     */
    private boolean scanInternal(TopicTrack track, UUID token) {
        if (track.getSourceIds().isEmpty()) {
            log.info("[TopicScan] 话题未配置任何站点来源，直接结束扫描: topicId={}, name='{}'", track.getId(), track.getName());
            return true;
        }
        Instant now = track.getScanStartedAt();
        Instant after = now.minus(Duration.ofDays(properties.getHistoryDays()));

        // 合并核心词 keyword 与扩展词 includeKeywords
        List<String> includeKeywords = Stream.concat(
                        Stream.of(track.getKeyword()),
                        readArray(track.getIncludeKeywords()).stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        List<String> blockedKeywords = readArray(track.getBlockedKeywords());
        int pageSize = properties.getCandidatePageSize();

        log.info("[TopicScan] 检索参数就绪: topicId={}, 时间窗口=[{} ~ {}], 包含词={}, 屏蔽词={}, 相似度阈值={}, 分页大小={}",
                track.getId(), after, now, includeKeywords, blockedKeywords,
                properties.getClusterSimilarityThreshold(), pageSize);

        TopicArticleClusters clusters = new TopicArticleClusters(properties.getClusterSimilarityThreshold());
        int embeddingDimensions = 0;
        Long afterArticleId = null;

        int pageIndex = 0;
        int totalCandidates = 0;
        int totalSkippedEvaluated = 0;
        int totalEmbedded = 0;
        long searchStartMs = System.currentTimeMillis();

        while (true) {
            heartbeat(track, token);
            pageIndex++;
            long pageStartMs = System.currentTimeMillis();

            // 按文章 ID 游标从 PostgreSQL 检索命中关键词的候选文章
            List<TopicArticleCandidate> page = topicArticleSearchStore.search(new TopicArticleSearchRequest(
                    track.getSourceIds(), includeKeywords, blockedKeywords, after, now, afterArticleId, pageSize));
            if (page.isEmpty()) {
                log.debug("[TopicScan] 第 {} 页检索返回为空，候选集拉取完毕: topicId={}, cursor={}",
                        pageIndex, track.getId(), afterArticleId);
                break;
            }
            Long previousCursor = afterArticleId;
            afterArticleId = page.getLast().articleId();

            // 过滤掉在当前 configVersion 下已经评估处理过的文章
            List<Long> ids = page.stream().map(TopicArticleCandidate::articleId).toList();
            Set<Long> evaluated = new HashSet<>(evaluationStore.evaluated(track.getId(), track.getConfigVersion(), ids));
            List<Long> unseen = ids.stream().filter(id -> !evaluated.contains(id)).toList();

            totalCandidates += page.size();
            totalSkippedEvaluated += evaluated.size();

            log.info("[TopicScan] 第 {} 页候选检索完成: topicId={}, cursor=[{} -> {}], 命中={}篇, 已评估跳过={}篇, 新增待处理={}篇, 检索耗时={}ms",
                    pageIndex, track.getId(), previousCursor, afterArticleId,
                    page.size(), evaluated.size(), unseen.size(), System.currentTimeMillis() - pageStartMs);

            // 加载未处理文章，计算 Embedding 向量并加入聚类器
            if (!unseen.isEmpty()) {
                List<Article> articles = loadArticles(unseen);
                if (!articles.isEmpty()) {
                    long embedStartMs = System.currentTimeMillis();
                    var embeddings = embeddingService.embed(articles);
                    long embedCostMs = System.currentTimeMillis() - embedStartMs;

                    if (!embeddings.isEmpty()) {
                        int dimensions = embeddings.getFirst().embedding().length;
                        if (embeddingDimensions != 0 && embeddingDimensions != dimensions) {
                            throw new IllegalStateException("Topic embedding dimensions changed during scan");
                        }
                        embeddingDimensions = dimensions;
                    }
                    clusters.add(articles.stream().map(Article::getId).toList(), embeddings);
                    totalEmbedded += articles.size();

                    log.info("[TopicScan] 第 {} 页向量化与入簇完成: topicId={}, 加载文章={}篇, 向量条数={}, 维度={}, 向量耗时={}ms",
                            pageIndex, track.getId(), articles.size(), embeddings.size(), embeddingDimensions, embedCostMs);
                }
            }
            if (page.size() < pageSize) break;
        }

        List<List<Long>> articleGroups = clusters.articleGroups();
        List<Integer> groupSizes = articleGroups.stream().map(List::size).toList();
        log.info("[TopicScan] 候选检索与聚类阶段汇总: topicId={}, name='{}', 累计命中={}篇, 已评估跳过={}篇, 本轮入簇={}篇, 生成聚类簇={}个 (各簇文章数={}), 阶段耗时={}ms",
                track.getId(), track.getName(), totalCandidates, totalSkippedEvaluated,
                totalEmbedded, articleGroups.size(), groupSizes, System.currentTimeMillis() - searchStartMs);

        if (articleGroups.isEmpty()) {
            log.info("[TopicScan] 本轮无新增待总结簇，扫描结束: topicId={}, name='{}'", track.getId(), track.getName());
            return true;
        }

        // 遍历聚类结果，每个文章簇独立生成一份总结并落库
        boolean succeeded = true;
        int clusterIndex = 0;
        for (List<Long> clusterIds : articleGroups) {
            clusterIndex++;
            heartbeat(track, token);
            try {
                log.info("[TopicScan] 开始处理聚类簇 [{}/{}]: topicId={}, 簇内文章数={}, articleIds={}",
                        clusterIndex, articleGroups.size(), track.getId(), clusterIds.size(), clusterIds);
                summarizeCluster(track, token, clusterIds, now, clusterIndex, articleGroups.size());
            } catch (RuntimeException exception) {
                succeeded = false;
                log.warn("[TopicScan] 聚类簇 [{}/{}] 总结处理失败: topicId={}, uid={}, 簇内文章数={}",
                        clusterIndex, articleGroups.size(), track.getId(), track.getTopicUid(), clusterIds.size(), exception);
            }
        }
        return succeeded;
    }

    /**
     * 对单个聚类簇生成 AI 总结并在短事务中保存匹配记录与总结实体
     */
    private void summarizeCluster(TopicTrack track, UUID token, List<Long> clusterIds, Instant now,
                                  int clusterIndex, int totalClusters) {
        boolean titlesOnly = clusterIds.size() > properties.getSummaryContentArticleLimit();
        List<Long> summarizedIds = new ArrayList<>();
        List<Article> preloadedArticles = null;
        String summaryText;

        long aiStartMs = System.currentTimeMillis();
        // 1. 文章数量超过阈值时仅按标题生成速览总结；否则加载全文生成精读总结
        if (titlesOnly) {
            List<String> titles = new ArrayList<>();
            int pageSize = properties.getCandidatePageSize();
            for (int start = 0; start < clusterIds.size(); start += pageSize) {
                heartbeat(track, token);
                List<Long> batchIds = clusterIds.subList(start, Math.min(start + pageSize, clusterIds.size()));
                Map<Long, ArticleTitle> rows = articleRepository.findTitlesByIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ArticleTitle::id, row -> row));
                for (Long id : batchIds) {
                    ArticleTitle row = rows.get(id);
                    if (row != null) {
                        summarizedIds.add(id);
                        titles.add(row.title());
                    }
                }
            }
            log.info("[TopicScan] 聚类簇 [{}/{}] 调用 AI 标题速览总结: topicId={}, 标题数={}",
                    clusterIndex, totalClusters, track.getId(), titles.size());
            summaryText = titles.isEmpty() ? null : aiService.summarizeTitles(track, titles);
        } else {
            heartbeat(track, token);
            preloadedArticles = loadArticles(clusterIds);
            summarizedIds.addAll(preloadedArticles.stream().map(Article::getId).toList());
            log.info("[TopicScan] 聚类簇 [{}/{}] 调用 AI 全文精读总结: topicId={}, 文章数={}",
                    clusterIndex, totalClusters, track.getId(), preloadedArticles.size());
            summaryText = preloadedArticles.isEmpty() ? null : aiService.summarize(track, preloadedArticles);
        }
        long aiCostMs = System.currentTimeMillis() - aiStartMs;
        log.info("[TopicScan] 聚类簇 [{}/{}] AI 总结生成完毕: topicId={}, 模式={}, 总结字数={}, AI耗时={}ms",
                clusterIndex, totalClusters, track.getId(), titlesOnly ? "TITLES_ONLY" : "FULL_CONTENT",
                summaryText == null ? 0 : summaryText.length(), aiCostMs);

        // 2. 在短事务中保存匹配明细、总结卡片、总结与文章关联表，并标记文章为已评估
        List<Article> cachedArticles = preloadedArticles;
        long dbStartMs = System.currentTimeMillis();
        transactionTemplate.executeWithoutResult(status -> {
            TopicTrack current = activeScan(track, token);
            if (!evaluationStore.evaluated(current.getId(), current.getConfigVersion(), clusterIds).isEmpty()) {
                throw new IllegalStateException("Topic cluster was already processed");
            }
            TopicTrack trackRef = entityManager.getReference(TopicTrack.class, current.getId());
            List<TopicTrackSummaryResponse.ArticlePreview> previews = new ArrayList<>(PREVIEW_ARTICLE_LIMIT);
            List<Long> savedArticleIds = new ArrayList<>(summarizedIds.size());
            int pageSize = properties.getCandidatePageSize();

            for (int start = 0; start < summarizedIds.size(); start += pageSize) {
                List<Long> batchIds = summarizedIds.subList(start, Math.min(start + pageSize, summarizedIds.size()));
                List<Article> batchArticles = cachedArticles != null
                        ? cachedArticles.subList(start, Math.min(start + pageSize, cachedArticles.size()))
                        : findOrderedArticles(batchIds);

                List<TopicTrackMatch> matches = new ArrayList<>(batchArticles.size());
                for (Article article : batchArticles) {
                    TopicTrackMatch match = new TopicTrackMatch();
                    match.setTopicTrack(trackRef);
                    match.setArticleId(article.getId());
                    match.setConfigVersion(current.getConfigVersion());
                    match.setMatchedAt(now);
                    matches.add(match);
                    savedArticleIds.add(article.getId());
                    if (previews.size() < PREVIEW_ARTICLE_LIMIT) {
                        previews.add(toArticlePreview(article));
                    }
                }
                matchRepository.saveAll(matches);
                entityManager.flush();
            }

            UUID savedSummaryUid = null;
            if (summaryText != null && !savedArticleIds.isEmpty()) {
                TopicTrackSummary summary = new TopicTrackSummary();
                summary.setTopicTrack(trackRef);
                summary.setConfigVersion(current.getConfigVersion());
                summary.setTopicName(track.getName());
                summary.setKeyword(track.getKeyword());
                summary.setSummary(summaryText);
                summary.setTitlesOnly(titlesOnly);
                summary.setArticleCount(savedArticleIds.size());
                summary.setArticlePreviews(writeJson(previews));
                TopicTrackSummary saved = summaryRepository.save(summary);
                savedSummaryUid = saved.getSummaryUid();

                for (int start = 0; start < savedArticleIds.size(); start += pageSize) {
                    summaryRepository.linkArticles(saved.getId(),
                            savedArticleIds.subList(start, Math.min(start + pageSize, savedArticleIds.size())));
                }
            }
            evaluationStore.record(current.getId(), current.getConfigVersion(), clusterIds, now);
            log.info("[TopicScan] 聚类簇 [{}/{}] 落库完成: topicId={}, summaryUid={}, 关联文章={}篇, 已标记评估={}篇, 事务耗时={}ms",
                    clusterIndex, totalClusters, track.getId(), savedSummaryUid,
                    savedArticleIds.size(), clusterIds.size(), System.currentTimeMillis() - dbStartMs);
        });
    }

    /**
     * 校验请求参数并赋值到 TopicTrack 实体
     */
    private void apply(TopicTrack track, TopicTrackConfigRequest request) {
        if (request == null || !StringUtils.hasText(request.name()) || !StringUtils.hasText(request.keyword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_TRACK");
        }
        List<UUID> sourceUids = request.sourceIds() == null ? List.of() : request.sourceIds().stream().distinct().toList();
        if (sourceUids.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "TOPIC_SOURCE_REQUIRED");
        if (sourceUids.size() > MAX_SOURCES || request.name().trim().length() > 120
                || request.relevancePrompt() != null && request.relevancePrompt().length() > 3000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_TRACK");
        }
        List<Feed> feeds = sourceUids.stream().map(uid -> feedRepository.findByUid(uid)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "TOPIC_SOURCE_NOT_FOUND"))).toList();

        track.setName(request.name().trim());
        track.setKeyword(request.keyword().trim());
        track.setNormalizedKeyword(validateKeyword(request.keyword()));
        track.setIncludeKeywords(writeJson(normalize(request.includeKeywords())));
        track.setBlockedKeywords(writeJson(normalize(request.blockedKeywords())));
        track.setCategories("[]");
        track.setRelevancePrompt(request.relevancePrompt() == null ? "" : request.relevancePrompt().trim());
        track.setEnabled(request.enabled() == null || request.enabled());
        track.setSourceIds(feeds.stream().map(Feed::getId).collect(Collectors.toSet()));
    }

    private TopicTrackResponse toResponse(TopicTrack track, Integer viewerId) {
        Map<Integer, Feed> feedById = feedRepository.findAllById(track.getSourceIds()).stream()
                .collect(Collectors.toMap(Feed::getId, feed -> feed));
        boolean subscribed = viewerId != null && subscriptionRepository.existsByTopicTrack_IdAndUserId(track.getId(), viewerId);
        return toResponse(track, viewerId, feedById, subscribed);
    }

    /**
     * 提取用于比对配置是否发生实质变更的快照对象
     */
    private ScanConfig scanConfig(TopicTrack track) {
        return new ScanConfig(
                TopicKeyword.normalize(track.getKeyword()),
                readArray(track.getIncludeKeywords()).stream().sorted().toList(),
                readArray(track.getBlockedKeywords()).stream().sorted().toList(),
                Set.copyOf(track.getSourceIds()),
                track.getRelevancePrompt()
        );
    }

    private record ScanConfig(String keyword, List<String> include, List<String> blocked, Set<Integer> sources,
                              String prompt) {}

    /**
     * 批量组装分页话题响应，合并查询订阅源与关注状态以避免 N+1 查询
     */
    private Page<TopicTrackResponse> toPageResponse(Page<TopicTrack> page, Integer viewerId) {
        Set<Integer> sourceIds = page.stream().flatMap(track -> track.getSourceIds().stream()).collect(Collectors.toSet());
        Map<Integer, Feed> feeds = sourceIds.isEmpty() ? Map.of() : feedRepository.findAllById(sourceIds).stream()
                .collect(Collectors.toMap(Feed::getId, feed -> feed));
        List<Long> topicIds = page.stream().map(TopicTrack::getId).toList();
        Set<Long> followed = viewerId == null || topicIds.isEmpty() ? Set.of()
                : new HashSet<>(subscriptionRepository.findSubscribedTopicIds(viewerId, topicIds));
        return page.map(track -> toResponse(track, viewerId, feeds, followed.contains(track.getId())));
    }

    private TopicTrackResponse toResponse(TopicTrack track, Integer viewerId, Map<Integer, Feed> feedById, boolean subscribed) {
        List<TopicTrackSuggestionResponse.SourceOption> sources = track.getSourceIds().stream().sorted().map(feedById::get)
                .filter(Objects::nonNull).map(this::sourceOption).toList();
        boolean owner = viewerId != null && viewerId.equals(track.getOwnerId());
        String status = !track.isEnabled() ? "DISABLED"
                : track.getScanToken() != null && track.getScanLeaseUntil() != null
                && !track.getScanLeaseUntil().isAfter(Instant.now()) ? "FAILED"
                : track.getScanStatus();
        return new TopicTrackResponse(
                track.getTopicUid(), track.getName(), track.getKeyword(),
                readArray(track.getIncludeKeywords()), readArray(track.getBlockedKeywords()), sources,
                readArray(track.getCategories()), track.getRelevancePrompt(), track.isEnabled(), owner, subscribed,
                track.getLastRunAt(), track.getCreatedAt(), track.getUpdatedAt(), status,
                track.getScanStartedAt(), track.getScanFinishedAt(), track.getScanError(), track.getConfigVersion()
        );
    }

    private TopicTrackSummaryResponse toSummaryResponse(TopicTrackSummaryRepository.SummaryRow summary) {
        var previews = summaryRepository.findArticles(summary.getId(), 0L, PageRequest.of(0, PREVIEW_ARTICLE_LIMIT))
                .stream().map(this::toArticlePreview).toList();
        return new TopicTrackSummaryResponse(
                summary.getSummaryUid(), summary.getTopicUid(), summary.getTopicName(), summary.getKeyword(),
                summary.getSummary(), summary.getArticleCount(), previews, summary.getCreatedAt(), summary.getTitlesOnly()
        );
    }

    private TopicTrackSummaryResponse.ArticlePreview toArticlePreview(Article article) {
        Feed feed = article.getFeed();
        return new TopicTrackSummaryResponse.ArticlePreview(
                article.getUid(), article.getTitle(), article.getLink(),
                feed == null ? "" : feed.getTitle(),
                feed == null ? null : feed.getIcon(),
                article.getPublishedAt()
        );
    }

    private TopicTrackSummaryResponse.ArticlePreview toArticlePreview(TopicTrackSummaryRepository.ArticleRow article) {
        return new TopicTrackSummaryResponse.ArticlePreview(
                article.getArticleId(), article.getTitle(), article.getUrl(),
                article.getFeedName(), article.getFeedIcon(), article.getPublishedAt()
        );
    }

    private TopicTrackSuggestionResponse.SourceOption sourceOption(Feed feed) {
        return new TopicTrackSuggestionResponse.SourceOption(
                feed.getUid(),
                StringUtils.hasText(feed.getTitle()) ? feed.getTitle() : feed.getSiteUrl(),
                feed.getSiteUrl()
        );
    }

    private User ensureUser(Integer userId) {
        if (userId == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        return userService.findUserById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
    }

    private String validateKeyword(String keyword) {
        String normalizedKeyword = TopicKeyword.normalize(keyword);
        if (!StringUtils.hasText(normalizedKeyword) || keyword.length() > 240) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_KEYWORD");
        }
        return normalizedKeyword;
    }

    public record CreationResult(TopicTrackResponse topic, boolean created) {}

    private TopicTrack findTopic(UUID topicUid) {
        return topicTrackRepository.findByTopicUid(topicUid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOPIC_TRACK_NOT_FOUND"));
    }

    private void addSubscription(Integer userId, TopicTrack track, User user) {
        if (subscriptionRepository.existsByTopicTrack_IdAndUserId(track.getId(), userId)) return;
        checkSubscriptionLimit(userId, user);
        subscriptionRepository.subscribe(track.getId(), userId);
    }

    private void checkSubscriptionLimit(Integer userId, User user) {
        if (subscriptionRepository.countByUserId(userId) >= user.getCurrentPlan().getMaxSubscriptions()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOPIC_SUBSCRIPTION_LIMIT_EXCEEDED");
        }
    }

    /**
     * 校验并清洗扩展关键词/屏蔽词列表（去空、trim、去重、校验数量与单条长度）
     */
    private List<String> normalize(List<String> values) {
        if (values == null) return List.of();
        if (values.size() > MAX_TERMS || values.stream().anyMatch(value -> value == null || value.trim().length() > MAX_TERM_LENGTH)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_TERMS");
        }
        return values.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
    }

    private List<String> readArray(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize topic tracking data", exception);
        }
    }
}
