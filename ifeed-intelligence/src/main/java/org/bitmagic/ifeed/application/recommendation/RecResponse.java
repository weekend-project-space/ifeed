package org.bitmagic.ifeed.application.recommendation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * @author yangrd
 * @date 2025/11/8
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecResponse(UUID id,
                          String title,
                          String summary,
                          UUID feedId,
                          String feedTitle,
                          String feedAvatar,
                          String publishedAt,
                          String thumbnail,
                          String enclosure,
                          String source,
                          double score,
                          String reason) {
}
