package org.bitmagic.ifeed.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record ArticleSummaryView(
        UUID id,
        String title,
        String link,
        String summary,
        String feedTitle,
        Instant publishedAt,
        String tags,
        String thumbnail,
        String enclosure) {
}
