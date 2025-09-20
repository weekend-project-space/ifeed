package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CollectionItemResponse(@JsonProperty("article_id") String articleId,
                                      String title) {
}
