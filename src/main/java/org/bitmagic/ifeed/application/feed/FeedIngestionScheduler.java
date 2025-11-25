package org.bitmagic.ifeed.application.feed;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.RssFetcherProperties;
import org.bitmagic.ifeed.infrastructure.util.TaskUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedIngestionScheduler {

    private final FeedIngestionService ingestionService;

    private final ExecutorService executor;

    private final RssFetcherProperties properties;

    private final CacheManager cacheManager;

    @Autowired
    public FeedIngestionScheduler(FeedIngestionService ingestionService,
                                  RssFetcherProperties properties, CacheManager cacheManager) {
        this.ingestionService = ingestionService;
        AtomicInteger counter = new AtomicInteger(0);
        this.executor = Executors.newFixedThreadPool(properties.getThreadPoolSize(), (runnable) -> {
            Thread t = new Thread(runnable, "feed-fetch-worker-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        this.properties = properties;
        this.cacheManager = cacheManager;
    }

    @Scheduled(initialDelayString = "${app.rss.fetcher.initial-delay:PT10S}",
            fixedDelayString = "${app.rss.fetcher.fixed-delay:PT30M}")
    public void refreshFeeds() {
        long start = System.currentTimeMillis();

        var feedIds = ingestionService.getFeedIds(
                feed -> LocalDateTime.now().getHour() == 0 || feed.getFailureCount() < 7
        );

        if (feedIds.isEmpty()) {
            log.info("No feeds scheduled for ingestion at this time");
            return;
        }

        log.info("Starting scheduled ingestion for {} feeds", feedIds.size());

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(feedIds.size());

        // 构造任务列表
        List<Runnable> tasks = feedIds.stream()
                .map(feedId -> (Runnable) () -> ingestionService.ingestFeed(feedId))
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
                success.get(), failed.get(), duration);
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