package org.bitmagic.ifeed.api.request;

import jakarta.validation.constraints.NotBlank;
import org.bitmagic.ifeed.domain.model.value.MixFeedFilterConfig;

public record MixFeedRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        String icon,
        Boolean isPublic,
        MixFeedFilterConfig filterConfig) {
}
