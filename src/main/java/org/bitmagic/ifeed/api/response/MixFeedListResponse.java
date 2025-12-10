package org.bitmagic.ifeed.api.response;

import java.time.Instant;

public record MixFeedListResponse(
        String id,
        String name,
        String description,
        String icon,
        Integer subscriberCount,
        Boolean isPublic,
        Instant createdAt) {
}
