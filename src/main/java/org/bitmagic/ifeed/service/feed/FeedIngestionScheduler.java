package org.bitmagic.ifeed.service.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedIngestionScheduler {

    private final FeedIngestionService ingestionService;

    @Scheduled(initialDelayString = "${rss.fetcher.initial-delay:PT10S}",
               fixedDelayString = "${rss.fetcher.fixed-delay:PT30M}")
    public void refreshFeeds() {
        var feedIds = ingestionService.getFeedIds();
        log.debug("Starting scheduled ingestion for {} feeds", feedIds.size());
        feedIds.forEach(ingestionService::ingestFeed);
    }
}
