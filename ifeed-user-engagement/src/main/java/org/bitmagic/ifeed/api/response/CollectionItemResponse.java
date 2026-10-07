package org.bitmagic.ifeed.api.response;

import java.time.Instant;
import java.util.UUID;

public record CollectionItemResponse(String articleId, String title, String feedTitle, String thumbnail, String summary,
                                     Instant collectedAt, UUID folderId) {
}
