package org.bitmagic.ifeed.api.response;

import java.time.Instant;

public record FeedDetailResponse(
        String feedId,
        String title,
        String url,
        String siteUrl,
        Instant lastFetched,
        Instant lastUpdated,
        Instant latestPublishedAt,
        long articleCount,
        long subscriberCount,
        boolean subscribed
) {
}
