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
        String feedTitle,
        String feedAvatar,
        String publishedAt,
        List<String> tags,
        String timeAgo,
        String feedId) {
}
