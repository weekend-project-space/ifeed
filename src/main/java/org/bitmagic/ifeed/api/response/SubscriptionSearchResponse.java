package org.bitmagic.ifeed.api.response;

import java.time.Instant;

public record SubscriptionSearchResponse(
        String feedId,
        String title,
        String description,
        String avatar,
        String siteUrl,
        Long subscriberCount,
        Boolean subscribed,
        Instant lastUpdated,
        String type // "FEED" or "MIX_FEED"
) {
}
