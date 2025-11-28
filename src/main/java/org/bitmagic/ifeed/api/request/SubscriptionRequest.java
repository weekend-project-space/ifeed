package org.bitmagic.ifeed.api.request;

public record SubscriptionRequest(
        String feedUrl,
        String siteUrl,
        String title,
        String feedId // UUID for Feed or MixFeed
) {

    public static SubscriptionRequest of(String feedId) {
        return new SubscriptionRequest(null, null, null, feedId);
    }
}
