package org.bitmagic.ifeed.application.feed;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.FeedFetchStatus;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.infrastructure.feed.fetch.FeedFetcher;
import org.bitmagic.ifeed.application.feed.info.FeedInfoService;
import org.bitmagic.ifeed.infrastructure.feed.parse.FeedParser;
import org.bitmagic.ifeed.application.feed.process.ArticleCollector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedIngestionService {
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2048;

    private final FeedRepository feedRepository;
    private final ArticleRepository articleRepository;
    private final FeedFetcher feedFetcher;
    private final FeedParser feedParser;
    private final ArticleCollector articleCollector;
    private final FeedInfoService feedInfoService;

    @Transactional(readOnly = true)
    public List<UUID> getFeedIds(Predicate<Feed> predicate) {
        return feedRepository.findAll().stream()
                .filter(predicate)
                .map(Feed::getUid)
                .toList();
    }

    @Transactional
    public Optional<Boolean> ingestFeed(UUID feedUid) {
        return feedRepository.findByUid(feedUid).map(this::fetchFeedSafely);
    }

    private boolean fetchFeedSafely(Feed feed) {
        try {
            var latestContentUpdate = fetchAndProcessFeed(feed);
            applySuccessfulFetchState(feed, latestContentUpdate);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to ingest feed: {}", feed.getUrl(), ex);
            applyFailedFetchState(feed, ex);
            return false;
        }
    }

    private Instant fetchAndProcessFeed(Feed feed) throws IOException, InterruptedException, FeedException {
        byte[] bodyBytes = feedFetcher.fetch(feed.getUrl());
        SyndFeed syndFeed = feedParser.parse(bodyBytes, feed.getUrl());
        log.debug("Successfully fetched feed: {}", feed.getUrl());

        Instant latestContentUpdate = processEntries(feed, syndFeed.getEntries());
        feedInfoService.update(feed, syndFeed);
        return latestContentUpdate;
    }

    private void applySuccessfulFetchState(Feed feed, Instant latestContentUpdate) {
        if (latestContentUpdate != null) {
            var current = feed.getLastUpdated();
            if (current == null || latestContentUpdate.isAfter(current)) {
                feed.setLastUpdated(latestContentUpdate);
            }
        }

        feed.setLastFetched(Instant.now());
        feed.setLastFetchStatus(FeedFetchStatus.SUCCEEDED);
        feed.setFetchErrorAt(null);
        feed.setFetchError(null);
        feed.setFailureCount(0);
        feedRepository.save(feed);
    }

    private void applyFailedFetchState(Feed feed, Exception exception) {
        feed.setLastFetchStatus(FeedFetchStatus.FAILED);
        feed.setFetchErrorAt(Instant.now());
        feed.setFetchError(truncate(resolveErrorMessage(exception), MAX_ERROR_MESSAGE_LENGTH));
        feed.setFailureCount(Optional.ofNullable(feed.getFailureCount()).orElse(0) + 1);
        feedRepository.save(feed);
    }

    private Instant processEntries(Feed feed, List<SyndEntry> entries) {
        var articles = articleCollector.collect(feed, entries);
        if (articles.isEmpty()) {
            return null;
        }

        var latest = resolveLatestPublishedAt(articles);
        articleRepository.saveAllAndFlush(articles);
        return latest;
    }



    private Instant resolveLatestPublishedAt(List<Article> articles) {
        return articles.stream()
                .map(Article::getPublishedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (!StringUtils.hasText(throwable.getMessage()) && throwable.getCause() != null) {
            return resolveErrorMessage(throwable.getCause());
        }
        var message = StringUtils.hasText(throwable.getMessage())
                ? throwable.getMessage()
                : throwable.getClass().getSimpleName();
        if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
            return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        return message;
    }
}
