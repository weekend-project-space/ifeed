package org.bitmagic.ifeed.api.response;

import java.time.Instant;
import java.util.UUID;

public record LikeStateResponse(UUID articleId, boolean liked, Instant likedAt) {
}
