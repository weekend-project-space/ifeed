package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArticleEnrichResponse(String summary, String mindMap) {
}