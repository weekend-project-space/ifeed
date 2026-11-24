package org.bitmagic.ifeed.api.response;

import org.bitmagic.ifeed.domain.model.value.MixFeedFilterConfig;

import java.time.Instant;

public record MixFeedDetailResponse(
        String id,
        String name,
        String description,
        String icon,
        Long subscriberCount,
        Long articleCount,
        MixFeedFilterConfig filterConfig,
        Boolean subscribed,
        Boolean isPublic,
        Instant createdAt,
        Instant updatedAt) {
}
