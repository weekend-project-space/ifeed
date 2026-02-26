package org.bitmagic.ifeed.application.radar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.RadarProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.radar.RadarSnapshot;
import org.bitmagic.ifeed.domain.model.radar.RadarTopic;
import org.bitmagic.ifeed.domain.model.radar.RadarTopicArticle;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.service.radar.RadarTopicNamingService;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.bitmagic.ifeed.infrastructure.vector.SearchRequestTurbo;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Periodically builds a radar snapshot by clustering recent article embeddings.
 *
 * <p>整体思路：
 * <ol>
 *   <li>选取时间窗口内的候选文章（并且 embedding 已生成）</li>
 *   <li>对每篇文章在向量库里检索近邻（topK + 相似度阈值），收集邻居表</li>
 *   <li>用 DBSCAN 对邻居表聚类：核心点扩展簇，边缘点只加入不扩展，噪声点丢弃</li>
 *   <li>把 snapshot + topics + topic-article 映射落库，保证分页一致性</li>
 * </ol>
 *
 * <p>DBSCAN 参数映射：
 * <ul>
 *   <li>epsilon（邻域半径） → similarityThreshold（向量相似度阈值）</li>
 *   <li>minPts（核心点最小邻居数） → minClusterSize</li>
 * </ul>
 *
 * <p>MVP 取舍/注意点：
 * <ul>
 *   <li>候选集目前最多取 10000 篇文章。</li>
 *   <li>邻居关系为有向图（A 的 ANN 结果包含 B，不代表 B 的 ANN 结果包含 A）。</li>
 *   <li>向量检索和 AI 命名均在事务外执行，避免长时间占用数据库连接。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RadarSnapshotBuildScheduler {

    private final RadarProperties properties;
    private final VectorStoreTurbo vectorStore;
    private final ArticleRepository articleRepository;
    private final ArticleEmbeddingRepository articleEmbeddingRepository;
    private final RadarSnapshotSaveService saveService;
    private final RadarTopicNamingService topicNamingService;

    @Scheduled(
            initialDelayString = "${app.radar.initial-delay:PT1M}",
            fixedDelayString = "${app.radar.fixed-delay:PT1H}"
    )
    public void build() {
        // 注意：本方法不加 @Transactional；外部 IO（向量检索、AI 命名）在事务外完成，
        // 最终统一通过 saveService 在单次短事务中落库。
        log.info("Starting scheduled radar snapshot build");
        Instant now = Instant.now();

        // 1) 拉取候选文章。
        // 只选择 embedding 已生成的文章，因为后续要做向量近邻检索。
        int windowHours = properties.getWindowHours();
        Instant from = now.minus(Duration.ofHours(windowHours));
        List<Article> recent = articleRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.greaterThan(root.get("publishedAt"), from),
                        cb.isTrue(root.get("embeddingGenerated"))
                ),
                PageRequest.of(0, 10000)
        ).getContent();

        if (recent.isEmpty()) {
            log.info("Radar snapshot build skipped: no recent embedded articles");
            return;
        }

        // 2) 批量加载候选文章的 embedding。
        Map<Long, ArticleEmbeddingRecord> embeddingById = articleEmbeddingRepository.findAllByIds(
                        recent.stream().map(Article::getId).filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(ArticleEmbeddingRecord::id, r -> r));

        // 3) 准备 snapshotId。
        Map<Long, Article> byId = recent.stream().collect(Collectors.toMap(Article::getId, a -> a));
        String snapshotId = "radar_%s_w%dh".formatted(
                now.toString().replace(":", "").replace(".", "").replace("-", ""),
                windowHours);

        // 4) 对每篇文章做 ANN 近邻检索，收集邻居表。
        // 向量检索在事务外执行；单篇失败时跳过该文章，不中止整个 build。
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op timeFilter = b.and(
                b.gte("publishedAt", from.getEpochSecond()),
                b.lte("publishedAt", now.getEpochSecond())
        );

        Map<Long, Set<Long>> neighborMap = new HashMap<>();
        int vectorSearchErrors = 0;

        for (Long id : byId.keySet()) {
            ArticleEmbeddingRecord emb = embeddingById.get(id);
            if (emb == null || emb.embedding() == null || emb.embedding().length == 0) {
                continue;
            }

            SearchRequestTurbo req = SearchRequestTurbo.builder()
                    .embedding(emb.embedding())
                    .topK(properties.getNeighbors())
                    .similarityThreshold(properties.getSimilarityThreshold())
                    .filterExpression(timeFilter.build())
                    .build();

            List<Document> nn;
            try {
                nn = vectorStore.similaritySearch(req);
            } catch (Exception e) {
                vectorSearchErrors++;
                log.warn("Vector search failed for article id={}, skipping: {}", id, e.getMessage());
                continue;
            }

            if (nn == null || nn.isEmpty()) {
                continue;
            }

            Set<Long> neighbors = new HashSet<>();
            for (Document d : nn) {
                try {
                    long otherId = Long.parseLong(d.getId());
                    if (otherId != id && byId.containsKey(otherId)) {
                        neighbors.add(otherId);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            if (!neighbors.isEmpty()) {
                neighborMap.put(id, neighbors);
            }
        }

        if (vectorSearchErrors > 0) {
            log.warn("Vector search had {} errors during radar build (articles skipped)", vectorSearchErrors);
        }

        // 5) DBSCAN 聚类。
        //
        // 核心点（core point）：自身的 ANN 近邻数 >= minClusterSize。
        //   - 可以扩展簇（向外传播）。
        // 边缘点（border point）：是某个核心点的近邻，但自身近邻数不足。
        //   - 只能被并入簇，不能继续扩展。
        // 噪声点（noise）：无法从任何核心点到达。
        //   - 直接丢弃，不进入任何簇。
        //
        // 这样彻底避免了单链（single-linkage）的链式合并问题：
        // 两个不相关话题只有通过"核心点路径"才能连通，孤立的桥接文章无法传播。
        int minPts = properties.getMinClusterSize();
        Set<Long> corePoints = neighborMap.entrySet().stream()
                .filter(e -> e.getValue().size() >= minPts)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<Long, Integer> clusterLabel = new HashMap<>();
        int nextClusterId = 0;

        for (Long seed : corePoints) {
            if (clusterLabel.containsKey(seed)) {
                continue;
            }
            int cid = ++nextClusterId;
            Deque<Long> queue = new ArrayDeque<>();
            queue.push(seed);
            clusterLabel.put(seed, cid);

            while (!queue.isEmpty()) {
                Long cur = queue.pop();
                for (Long nbr : neighborMap.getOrDefault(cur, Set.of())) {
                    if (clusterLabel.containsKey(nbr)) {
                        continue;
                    }
                    clusterLabel.put(nbr, cid);
                    // 只有核心点才能继续扩展，边缘点加入后不入队
                    if (corePoints.contains(nbr)) {
                        queue.push(nbr);
                    }
                }
            }
        }

        // 按 clusterId 分组，过滤小簇，按簇大小降序取前 maxTopics 个
        Map<Integer, List<Long>> grouped = new HashMap<>();
        clusterLabel.forEach((aid, cid) ->
                grouped.computeIfAbsent(cid, k -> new ArrayList<>()).add(aid));

        List<List<Long>> clusterList = grouped.values().stream()
                .filter(list -> list.size() >= minPts)
                .sorted((x, y) -> Integer.compare(y.size(), x.size()))
                .limit(properties.getMaxTopics())
                .toList();

        log.info("DBSCAN result: candidates={}, corePoints={}, clusters={}, noise={}",
                byId.size(), corePoints.size(), clusterList.size(),
                byId.size() - clusterLabel.size());

        if (clusterList.isEmpty()) {
            log.info("Radar snapshot build produced no clusters meeting minClusterSize={}", minPts);
            return;
        }

        // 6) 在内存中构建 snapshot、topic、topic-article 对象。
        // AI 命名也在事务外完成（RadarTopicNamingService 内部已有降级逻辑）。
        RadarSnapshot snapshot = new RadarSnapshot();
        snapshot.setSnapshotId(snapshotId);
        snapshot.setGeneratedAt(now);
        snapshot.setWindowHours(windowHours);
        snapshot.setExpiresAt(now.plus(properties.getSnapshotTtl()));

        List<RadarTopic> topics = new ArrayList<>();
        List<RadarTopicArticle> articles = new ArrayList<>();

        for (List<Long> cluster : clusterList) {
            UUID topicId = UUID.randomUUID();

            List<String> titles = cluster.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .map(Article::getTitle)
                    .toList();
            var named = topicNamingService.nameTopic(titles);

            RadarTopic topic = new RadarTopic();
            topic.setTopicId(topicId);
            topic.setSnapshotId(snapshotId);
            topic.setTitle(named.title());
            topic.setDescription(named.description());
            topic.setCreatedAt(now);
            topic.setUpdatedAt(now);
            topic.setArticleCount(cluster.size());
            topic.setTopKeywords(CollectionUtils.isEmpty(named.keywords()) ? null : JSON.toJson(named.keywords()));
            topic.setScore((double) cluster.size());
            topics.add(topic);

            // 对 topic 内文章按"新→旧"排序，生成 rank（用于稳定分页）。
            List<Long> ordered = cluster.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Article::getPublishedAt).reversed())
                    .map(Article::getId)
                    .toList();

            int rank = 0;
            for (Long articleId : ordered) {
                rank++;
                RadarTopicArticle ta = new RadarTopicArticle();
                ta.setSnapshotId(snapshotId);
                ta.setTopicId(topicId);
                ta.setArticleId(articleId);
                ta.setRank(rank);
                ta.setScore(1.0);
                ta.setPublishedAt(byId.get(articleId).getPublishedAt());
                articles.add(ta);
            }
        }

        // 7) 单次短事务落库：清理过期 + 批量写入 snapshot/topics/articles。
        saveService.save(snapshot, topics, articles);

        log.info("Radar snapshot built: snapshotId={}, topics={}, articles={}, windowHours={}",
                snapshotId, topics.size(), articles.size(), windowHours);
    }
}
