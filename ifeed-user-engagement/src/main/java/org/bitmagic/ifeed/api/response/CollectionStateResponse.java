package org.bitmagic.ifeed.api.response;

import java.time.Instant;
import java.util.UUID;

public record CollectionStateResponse(UUID articleId, UUID folderId, Instant collectedAt) {
}
