package org.bitmagic.ifeed.api.converter;

import org.bitmagic.ifeed.api.response.SubscriptionResponse;
import org.bitmagic.ifeed.api.response.SubscriptionSearchResponse;
import org.bitmagic.ifeed.domain.model.Feed;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * @author yangrd
 * @date 2025/11/8
 **/
public class SubscriptionViewConverter {

    private static final String DEFAULT_FAVICON = "https://favicon.im/";
    private static final Instant EPOCH = Instant.EPOCH;

    public static SubscriptionResponse toResponse(
            Feed feed,
            Map<String, Instant> readTimes,
            boolean hasUnread) {

        var info = resolveFeedInfo(feed);
        var readTime = readTimes.getOrDefault(feed.getUid().toString(), EPOCH);
        var lastUpdated = feed.getLastUpdated() != null ? feed.getLastUpdated() : EPOCH;
        var failureCount = feed.getFailureCount() != null ? feed.getFailureCount() : 0;

        return new SubscriptionResponse(
                feed.getUid().toString(),
                info.title(),
                feed.getUrl(),
                info.siteUrl(),
                info.faviconUrl(),
                feed.getLastFetched(),
                feed.getLastUpdated(),
                hasUnread || readTime.isAfter(lastUpdated),
                failureCount < 3 ? 0 : failureCount,
                failureCount < 3 ? null : feed.getFetchError()
        );
    }

    public static SubscriptionSearchResponse toSearchResponse(
            Feed feed,
            Map<String, Instant> readTimes,
            boolean subscribed,
            long subscriberCount,
            boolean hasUnread) {

        var info = resolveFeedInfo(feed);
        var readTime = readTimes.getOrDefault(feed.getUid().toString(), EPOCH);
        var lastUpdated = feed.getLastUpdated() != null ? feed.getLastUpdated() : EPOCH;
        var failureCount = feed.getFailureCount() != null ? feed.getFailureCount() : 0;

        return new SubscriptionSearchResponse(
                feed.getUid().toString(),
                info.title(),
                feed.getUrl(),
                info.siteUrl(),
                info.faviconUrl(),
                feed.getLastFetched(),
                feed.getLastUpdated(),
                subscriberCount,
                subscribed,
                hasUnread || readTime.isAfter(lastUpdated),
                failureCount < 3 ? 0 : failureCount,
                failureCount < 3 ? null : feed.getFetchError()
        );
    }

    private static FeedInfo resolveFeedInfo(Feed feed) {
        if (feed == null) {
            return new FeedInfo("未命名订阅", null, DEFAULT_FAVICON);
        }

        String title = feed.getTitle();
        if (title == null || title.isBlank()) {
            title = extractHost(feed.getSiteUrl());
            if (title == null || title.isBlank()) {
                title = extractHost(feed.getUrl());
            }
            if (title == null || title.isBlank()) {
                title = "未命名订阅";
            }
        }

        String siteUrl = feed.getSiteUrl();
        if (siteUrl == null || siteUrl.isBlank()) {
            siteUrl = feed.getUrl();
        }

        String host = extractHost(siteUrl);
        if (host == null || host.isBlank()) {
            host = extractHost(feed.getUrl());
        }
        String favicon = host != null ? "https://favicon.im/%s".formatted(host) : DEFAULT_FAVICON;

        return new FeedInfo(title, siteUrl, favicon);
    }

    private static String extractHost(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (host != null && !host.isBlank()) return host;
            String path = uri.getPath();
            if (path != null && !path.isBlank() && path.length() > 1) return path.substring(1);
        } catch (Exception ignored) {}
        return null;
    }

    private record FeedInfo(String title, String siteUrl, String faviconUrl) {}
}
