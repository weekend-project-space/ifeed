package org.bitmagic.ifeed.service.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedIngestionScheduler {

    private final FeedIngestionService ingestionService;


    @Scheduled(initialDelayString = "${rss.fetcher.initial-delay:PT10S}",
            fixedDelayString = "${rss.fetcher.fixed-delay:PT30M}")
    public void refreshFeeds() {
        try {
            var feedIds = ingestionService.getFeedIds(feed -> LocalDate.now().getDayOfWeek().getValue() == 6 || feed.getFailureCount() < 7);
            log.info("Starting scheduled ingestion for {} feeds", feedIds.size());
            AtomicInteger counter = new AtomicInteger(0);
            feedIds.parallelStream().forEach(feedId -> {
                try {
                    ingestionService.ingestFeed(feedId);
                    counter.incrementAndGet();
                } catch (RuntimeException e) {
                    log.error("fetch err", e);
                }
            });
            log.info("Starting scheduled ingestion for {} feeds success :{}", feedIds.size(), counter.get());
        } catch (RuntimeException e) {
            log.warn("refreshFeeds", e);
        }

    }

}
