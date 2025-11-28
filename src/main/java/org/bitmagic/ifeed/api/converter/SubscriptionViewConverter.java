package org.bitmagic.ifeed.api.converter;

import org.bitmagic.ifeed.api.response.SubscriptionResponse;
import org.bitmagic.ifeed.api.response.SubscriptionSearchResponse;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.MixFeed;
import org.bitmagic.ifeed.domain.model.SourceType;

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
                failureCount < 3 ? null : feed.getFetchError(),
                SourceType.FEED.name(),
                info.faviconUrl());
    }

    public static SubscriptionResponse toResponse(
            MixFeed mixFeed,
            Map<String, Instant> readTimes,
            boolean hasUnread) {

        var readTime = readTimes.getOrDefault(mixFeed.getUid().toString(), EPOCH);
        var lastUpdated = mixFeed.getUpdatedAt() != null ? mixFeed.getUpdatedAt() : EPOCH;

        return new SubscriptionResponse(
                mixFeed.getUid().toString(),
                mixFeed.getName(),
                null, // URL is not applicable for MixFeed in the same way
                null, // Site URL
                mixFeed.getIcon(), // Avatar/Icon
                null, // Last fetched
                lastUpdated,
                hasUnread || readTime.isAfter(lastUpdated),
                0,
                null,
                SourceType.MIX_FEED.name(),
                mixFeed.getIcon());
    }

    public static SubscriptionSearchResponse toSearchResponse(Feed feed, boolean subscribed, Long subscriberCount) {
        return new SubscriptionSearchResponse(
                feed.getUid().toString(),
                feed.getTitle(),
                feed.getDescription(),
                "https://favicon.im/%s".formatted(extractHost(feed.getSiteUrl())),
                feed.getSiteUrl(),
                subscriberCount != null ? subscriberCount : 0L,
                subscribed,
                feed.getLastUpdated(),
                "FEED");
    }

    public static SubscriptionSearchResponse toSearchResponse(MixFeed mixFeed, boolean subscribed) {
        return new SubscriptionSearchResponse(
                mixFeed.getUid().toString(),
                mixFeed.getName(),
                mixFeed.getDescription(),
                mixFeed.getIcon(),
                null,
                mixFeed.getSubscriberCount() != null ? mixFeed.getSubscriberCount().longValue() : 0L,
                subscribed,
                mixFeed.getLastUpdated(),
                "MIX_FEED");
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
        if (url == null || url.isBlank())
            return null;
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (host != null && !host.isBlank())
                return host;
            String path = uri.getPath();
            if (path != null && !path.isBlank() && path.length() > 1)
                return path.substring(1);
        } catch (Exception ignored) {
        }
        return null;
    }

    private record FeedInfo(String title, String siteUrl, String faviconUrl) {
    }
}
