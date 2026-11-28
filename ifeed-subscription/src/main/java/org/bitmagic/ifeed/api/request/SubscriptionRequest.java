package org.bitmagic.ifeed.api.request;

public record SubscriptionRequest(
        String feedUrl,
        String siteUrl,
        String title,
        String feedId // Feed UUID
) {

    public static SubscriptionRequest of(String feedId) {
        return new SubscriptionRequest(null, null, null, feedId);
    }
}
