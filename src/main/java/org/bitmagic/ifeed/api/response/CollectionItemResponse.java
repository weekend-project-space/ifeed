package org.bitmagic.ifeed.api.response;

import java.time.Instant;

public record CollectionItemResponse(String articleId, String title, Instant collectedAt) {
}
