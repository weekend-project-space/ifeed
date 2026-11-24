package org.bitmagic.ifeed.api.response.radar;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RadarTopicResponse(
        String topicId,
        String title,
        String description,
        Instant createdAt,
        Instant updatedAt,
        int articleCount,
        List<String> topKeywords
) {
}

