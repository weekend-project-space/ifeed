package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ArticleDetailResponse(
        String id,
        String title,
        String content,
        String summary,
        String link,
        String thumbnail,
        String enclosure,
        @JsonProperty("feedId") String feedId,
        @JsonProperty("feedTitle") String feedTitle,
        @JsonProperty("publishedAt") String publishedAt,
        List<String> tags) {
}
