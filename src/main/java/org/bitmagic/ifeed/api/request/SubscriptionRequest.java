package org.bitmagic.ifeed.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequest(
        @NotBlank @JsonProperty("feed_url") String feedUrl,
        @JsonProperty("site_url") String siteUrl,
        String title
) {
}
