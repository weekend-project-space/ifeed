package org.bitmagic.ifeed.api.response;

import java.time.Instant;

public record SubscriptionSearchResponse(
        String feedId,
        String title,
        String url,
        String siteUrl,
        String avatar,
        Instant lastFetched,
        Instant lastUpdated,
        Long subscriberCount,
        Boolean subscribed,
        Boolean isRead,
        Integer failureCount,
        String fetchError
) {
}
