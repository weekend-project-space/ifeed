package org.bitmagic.ifeed.api.tracking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record TopicTrackConfigRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 240) String keyword,
        @Size(max = 30) List<@Size(max = 80) String> includeKeywords,
        @Size(max = 30) List<@Size(max = 80) String> blockedKeywords,
        @Size(max = 30) List<UUID> sourceIds,
        @Size(max = 3) List<String> categories,
        @Size(max = 3000) String relevancePrompt,
        Boolean enabled
) {
}
