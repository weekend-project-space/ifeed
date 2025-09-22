package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ArticleSummaryResponse(
        String id,
        String title,
        String link,
        String summary,
        @JsonProperty("feedTitle") String feedTitle,
        @JsonProperty("publishedAt") String publishedAt,
        List<String> tags,
        @JsonProperty("timeAgo") String timeAgo) {
}
