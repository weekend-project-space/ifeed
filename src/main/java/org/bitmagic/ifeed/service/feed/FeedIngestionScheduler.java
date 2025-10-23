package org.bitmagic.ifeed.service.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.service.embedding.UserEmbeddingService;
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
        log.info("Starting scheduled ingestion for {} feeds", feedIds.size());
        feedIds.parallelStream().forEach(ingestionService::ingestFeed);
    }

}
