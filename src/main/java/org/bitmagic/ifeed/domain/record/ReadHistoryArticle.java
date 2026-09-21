package org.bitmagic.ifeed.domain.record;

import java.time.Instant;

public record ReadHistoryArticle(ArticleSummary article, Instant readAt) {
}
