package org.bitmagic.ifeed.api.response;

import java.time.Instant;

public record SubscriptionResponse(
        String feedId,
        String title,
        String url,
        String siteUrl,
        Instant lastFetched,
        Instant lastUpdated,
        Boolean isRead
) {
}
