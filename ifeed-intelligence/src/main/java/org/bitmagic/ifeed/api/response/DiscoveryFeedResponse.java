package org.bitmagic.ifeed.api.response;

import java.time.Instant;

public record DiscoveryFeedResponse(
        String feedId,
        String name,
        String description,
        String url,
        String siteUrl,
        String favicon,
        String category,
        String categoryName,
        Long subscriberCount,
        Long articleCount,
        Instant lastUpdated,
        String updateFrequency,
        Boolean subscribed,
        Boolean featured) {
}
