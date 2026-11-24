package org.bitmagic.ifeed.api.request;

import java.time.Instant;

public record ReadHistoryRequest(String articleId, Instant readAt) {
}
