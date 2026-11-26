package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.UUID;

/** Adds account-specific state at the API boundary, outside shared article/recommendation caches. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ArticleWithStateResponse<T>(@JsonUnwrapped T article, boolean collected, boolean liked, UUID folderId) {
}
