package org.bitmagic.ifeed.api.response;

import org.bitmagic.ifeed.domain.model.value.FeedFetchStatus;

import java.time.Instant;

public record SubscriptionStateResponse(
        String message,
        String feedId,
        String title,
        String url,
        boolean subscribed,
        boolean changed,
        FeedFetchStatus fetchStatus,
        Instant lastFetched,
        Instant lastUpdated,
        String fetchError) {
}
