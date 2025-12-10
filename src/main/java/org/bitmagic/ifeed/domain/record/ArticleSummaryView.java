package org.bitmagic.ifeed.domain.record;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import java.time.Instant;
import java.util.UUID;

@RegisterReflectionForBinding(ArticleSummaryView.class)
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
