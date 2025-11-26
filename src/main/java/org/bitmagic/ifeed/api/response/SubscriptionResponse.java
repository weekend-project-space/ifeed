package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionResponse(
        String feedId,
        String title,
        String url,
        String siteUrl,
        String avatar,
        Instant lastFetched,
        Instant lastUpdated,
        Boolean isRead,
        Integer failureCount,
        String fetchError
) {
}
