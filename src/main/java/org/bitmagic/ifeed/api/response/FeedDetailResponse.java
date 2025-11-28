package org.bitmagic.ifeed.api.response;

import java.time.Instant;
import java.util.Collection;

public record FeedDetailResponse(
        String feedId,
        String title,
        String description,
        String url,
        String siteUrl,
        String avatar,
        Instant lastFetched,
        Instant lastUpdated,
        Instant latestPublishedAt,
        long articleCount,
        long subscriberCount,
        boolean subscribed,
        Integer failureCount,
        String fetchError,
        String type, // "FEED" or "MIX_FEED"
        Collection<String> sources
) {
}
