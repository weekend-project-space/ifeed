package org.bitmagic.ifeed.application.feed;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.RssFetcherProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.value.MixFeedFilterConfig;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.MixFeedRepository;
import org.bitmagic.ifeed.domain.spec.ArticleSpecs;
import org.bitmagic.ifeed.domain.spec.MixFeedSpecs;
import org.bitmagic.ifeed.infrastructure.util.TaskUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedIngestionScheduler {

    private final FeedIngestionService ingestionService;

    private final MixFeedRepository mixFeedRepository;

    private final ArticleRepository articleRepository;

    private final ExecutorService executor;

    private final RssFetcherProperties properties;

    private final CacheManager cacheManager;

    @Autowired
    public FeedIngestionScheduler(FeedIngestionService ingestionService, MixFeedRepository mixFeedRepository, ArticleRepository articleRepository,
                                  RssFetcherProperties properties, CacheManager cacheManager) {
        this.ingestionService = ingestionService;
        AtomicInteger counter = new AtomicInteger(0);
        this.executor = Executors.newFixedThreadPool(properties.getThreadPoolSize(), (runnable) -> {
            Thread t = new Thread(runnable, "feed-fetch-worker-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        this.properties = properties;
        this.mixFeedRepository = mixFeedRepository;
        this.articleRepository = articleRepository;
        this.cacheManager = cacheManager;
    }

    @Scheduled(initialDelayString = "${app.rss.fetcher.initial-delay:PT10S}",
            fixedDelayString = "${app.rss.fetcher.fixed-delay:PT30M}")
    public void refreshFeeds() {
        long start = System.currentTimeMillis();

        var feedIds = ingestionService.getFeedIds(
                feed -> LocalDateTime.now().getHour() == 0 || feed.getFailureCount() < 12
        );

        if (feedIds.isEmpty()) {
            log.info("No feeds scheduled for ingestion at this time");
            return;
        }

        log.info("Starting scheduled ingestion for {} feeds", feedIds.size());

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fetchSuccess = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(feedIds.size());

        // 构造任务列表
        List<Runnable> tasks = feedIds.stream()
                .map(feedId -> (Runnable) () -> {
                    if (ingestionService.ingestFeed(feedId).orElse(false)) {
                        fetchSuccess.incrementAndGet();
                    }
                })
                .toList();

        // 封装执行 + 超时控制
        TaskUtils.executeWithTimeout(executor, tasks, feedIds.size() / properties.getThreadPoolSize(), TimeUnit.MINUTES, latch, success, failed);

        // 等待完成（稍长于超时，防止竞争）
        try {
            latch.await(feedIds.size() / properties.getThreadPoolSize() + 1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.warn("Feed ingestion interrupted", e);
            Thread.currentThread().interrupt();
        }
        evictItemCache();
        long duration = (System.currentTimeMillis() - start) / 1000;
        log.info("Feed refresh completed: {} success, {} failed, {}s",
                fetchSuccess.get(), feedIds.size() - fetchSuccess.get(), duration);
        refreshMixFeeds();
    }


    public void refreshMixFeeds() {
        log.info("start refreshMixFeeds...");
        long limit = mixFeedRepository.count() / 10 + 1;
        Stream.iterate(0, i -> i + 1).limit(limit).forEach(i -> {
            mixFeedRepository.findAll(PageRequest.of(i, 10)).stream().parallel().forEach(mixFeed -> {
                try {
                    MixFeedFilterConfig config = mixFeed.config();
                    // Extract filter parameters
                    Set<UUID> sourceFeedIds = null;
                    if (config.getSourceFeeds() != null && !config.getSourceFeeds().isEmpty()) {
                        sourceFeedIds = config.getSourceFeeds().keySet().stream()
                                .map(UUID::fromString)
                                .collect(java.util.stream.Collectors.toSet());
                    }
                    List<String> includeKeywords = config.getKeywords().getInclude();
                    List<String> excludeKeywords = config.getKeywords().getExclude();
                    Instant fromDate = config.getDateRange() != null ? config.getDateRange().getFrom() : null;
                    Instant toDate = config.getDateRange() != null ? config.getDateRange().getTo() : null;

                    // Build Specification
                    Specification<Article> spec = MixFeedSpecs.mixFeedArticles(sourceFeedIds, fromDate, toDate, includeKeywords, excludeKeywords);
                    List<Article> articles = articleRepository.findAll(spec, PageRequest.of(0, 1, Sort.by(Sort.Order.desc("publishedAt")))).getContent();
                    if (!articles.isEmpty()) {
                        mixFeed.setLastFetched(Instant.now());
                        mixFeed.setLastUpdated(articles.iterator().next().getPublishedAt());
                    }
                    mixFeedRepository.save(mixFeed);
                    log.debug("refresh MixFeed :{}", mixFeed.getName());
                } catch (RuntimeException e) {
                    log.warn("refresh MixFeed", e);
                }
            });
        });

        log.info("end refreshMixFeeds...");
    }

    /**
     * 应用关闭时优雅关闭线程池
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down feed ingestion thread pool...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Thread pool did not terminate in time, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while shutting down thread pool", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Feed ingestion thread pool stopped");
    }

    public void evictItemCache() {
        var cache = cacheManager.getCache("ITEMS");
        if (cache != null) {
            cache.clear();
        }
    }
}