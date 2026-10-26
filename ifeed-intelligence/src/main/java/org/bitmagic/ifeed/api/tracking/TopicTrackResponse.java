package org.bitmagic.ifeed.api.tracking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopicTrackResponse(
        UUID topicId,
        String name,
        String keyword,
        List<String> includeKeywords,
        List<String> blockedKeywords,
        List<TopicTrackSuggestionResponse.SourceOption> sources,
        List<String> categories,
        String relevancePrompt,
        boolean enabled,
        boolean owner,
        boolean subscribed,
        Instant lastRunAt,
        Instant createdAt,
        Instant updatedAt,
        String scanStatus,
        Instant scanStartedAt,
        Instant scanFinishedAt,
        String scanError,
        int configVersion
) {
}
