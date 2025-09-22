package org.bitmagic.ifeed.api.response;

import java.time.Instant;

public record ReadHistoryItemResponse(String articleId, String title, Instant readAt) {
}
