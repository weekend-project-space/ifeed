package org.bitmagic.ifeed.api.response.radar;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RadarItemResponse(
        String articleId,
        String title,
        String summary,
        String thumbnail,
        String enclosure,
        @JsonProperty("feedTitle") String feedTitle,
        @JsonProperty("publishedAt") String publishedAt,
        List<String> tags,
        double score,
        @JsonProperty("timeAgo") String timeAgo
) {
}

