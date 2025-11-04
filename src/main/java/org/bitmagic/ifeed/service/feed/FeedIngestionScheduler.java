package org.bitmagic.ifeed.service.feed;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.RssFetcherProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

    @Autowired
    public FeedIngestionScheduler(FeedIngestionService ingestionService,
            RssFetcherProperties properties) {
        this.ingestionService = ingestionService;
        AtomicInteger counter = new AtomicInteger(0);
        this.executor = Executors.newFixedThreadPool(properties.getThreadPoolSize(), (runnable) -> {
            Thread t = new Thread(runnable, "feed-fetch-worker-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    @Scheduled(initialDelayString = "${app.rss.fetcher.initial-delay:PT10S}", fixedDelayString = "${app.rss.fetcher.fixed-delay:PT30M}")
    public void refreshFeeds() {
        long start = System.currentTimeMillis();

        var feedIds = ingestionService
                .getFeedIds(feed -> LocalDateTime.now().getHour() == 0 || feed.getFailureCount() < 7);

        if (feedIds.isEmpty()) {
            log.info("No feeds scheduled for ingestion at this time");
            return;
        }

        log.info("Starting scheduled ingestion for {} feeds", feedIds.size());

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(feedIds.size());

        feedIds.forEach(feedId -> {
            executor.submit(() -> {
                try {
                    ingestionService.ingestFeed(feedId);
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    log.error("Failed to ingest feed: {}", feedId, e);
                } finally {
                    latch.countDown();
                }
                log.debug("Feed refresh completed: {} success, {} failed",
                        success.get(), failed.get());
            });
        });

        // 等待所有任务结束（最多 15 分钟）
        try {
            if (!latch.await(15, TimeUnit.MINUTES)) {
                log.warn("Feed ingestion timed out after 15 minutes");
            }
        } catch (InterruptedException e) {
            log.warn("Feed ingestion interrupted", e);
            Thread.currentThread().interrupt();
        }

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
}