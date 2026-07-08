package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ArticleDetailResponse(
        String id,
        String title,
        String content,
        String summary,
        String mindMap,
        boolean requiresUpgrade, //内容访问是否被会员等级限制
        String link,
        String thumbnail,
        String enclosure,
        String enclosureType,
        @JsonProperty("feedId") String feedId,
        @JsonProperty("feedTitle") String feedTitle,
        String feedAvatar,
        @JsonProperty("publishedAt") String publishedAt,
        List<String> tags,
        boolean collected) {
}
