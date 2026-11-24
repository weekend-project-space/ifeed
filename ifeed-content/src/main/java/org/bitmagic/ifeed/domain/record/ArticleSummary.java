package org.bitmagic.ifeed.domain.record;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import java.time.Instant;
import java.util.UUID;

@RegisterReflectionForBinding(ArticleSummary.class)
public record ArticleSummary(
        UUID id,
        Long articleId,
        String title,
        String summary,
        String thumbnail,
        String feedTitle,
        Instant publishedAt,
        String tags, String category) {
}
