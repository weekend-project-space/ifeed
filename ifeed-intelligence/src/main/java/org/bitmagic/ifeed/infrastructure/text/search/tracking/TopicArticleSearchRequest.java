package org.bitmagic.ifeed.infrastructure.text.search.tracking;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TopicArticleSearchRequest(
        Set<Integer> feedIds,
        List<String> includeKeywords,
        List<String> blockedKeywords,
        Instant publishedFrom,
        Instant publishedTo,
        Long afterArticleId,
        int limit
) {
    public TopicArticleSearchRequest {
        feedIds = feedIds == null ? Set.of() : Set.copyOf(feedIds);
        includeKeywords = normalize(includeKeywords);
        blockedKeywords = normalize(blockedKeywords);
        Objects.requireNonNull(publishedFrom, "Published from is required");
        Objects.requireNonNull(publishedTo, "Published to is required");
        if (publishedFrom.isAfter(publishedTo)) {
            throw new IllegalArgumentException("Published from must not be after published to");
        }
        if (afterArticleId != null && afterArticleId < 0) {
            throw new IllegalArgumentException("Article cursor must not be negative");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
    }

    private static List<String> normalize(List<String> keywords) {
        return keywords == null ? List.of() : keywords.stream()
                .filter(Objects::nonNull).map(String::trim)
                .filter(keyword -> !keyword.isEmpty()).distinct().toList();
    }
}
