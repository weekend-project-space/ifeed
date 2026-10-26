package org.bitmagic.ifeed.api.tracking;

import java.util.List;
import java.util.UUID;

public record TopicTrackSuggestionResponse(
        List<String> includeKeywords,
        List<String> blockedKeywords,
        List<SourceOption> sources,
        List<String> categories,
        String relevancePrompt,
        TopicTrackResponse existingTopic
) {
    public record SourceOption(UUID sourceId, String name, String url) {
    }
}
