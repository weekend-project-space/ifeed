package org.bitmagic.ifeed.api.request;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequest(
        @NotBlank String feedUrl,
        String siteUrl,
        String title
) {
}
