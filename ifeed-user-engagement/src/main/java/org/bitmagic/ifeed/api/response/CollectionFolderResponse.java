package org.bitmagic.ifeed.api.response;

import java.time.Instant;
import java.util.UUID;

public record CollectionFolderResponse(UUID folderId, String name, Instant createdAt, Instant updatedAt) {
}
