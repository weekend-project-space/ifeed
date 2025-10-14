package org.bitmagic.ifeed.api.response;

import java.util.List;

/**
 * Aggregated insights for a user's subscribed content within a time window.
 */
public record UserSubscriptionInsightResponse(
        List<CategoryCount> categories,
        List<TagCount> hotTags
) {
    public record CategoryCount(String category, long count) {}
    public record TagCount(String tag, long count) {}
}

