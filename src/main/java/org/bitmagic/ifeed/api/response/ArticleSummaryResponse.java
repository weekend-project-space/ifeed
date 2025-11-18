package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArticleSummaryResponse(
        String id,
        String title,
        String summary,
        String thumbnail,
        String enclosure,
        @JsonProperty("feedTitle") String feedTitle,
        @JsonProperty("publishedAt") String publishedAt,
        List<String> tags,
        @JsonProperty("timeAgo") String timeAgo) {
}
