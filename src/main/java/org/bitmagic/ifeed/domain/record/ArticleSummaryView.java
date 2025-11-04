package org.bitmagic.ifeed.domain.record;

import java.time.Instant;
import java.util.UUID;

public record ArticleSummaryView(
        UUID id,
        Long articleId,
        String title,
        String link,
        String summary,
        String feedTitle,
        Instant publishedAt,
        String tags,
        String thumbnail,
        String enclosure) {
}
