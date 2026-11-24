package org.bitmagic.ifeed.domain.spec;

import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/12/17
 **/
public interface ArticleEnrichmentSpec {
    List<ArticleEnrichment.Rating> TOPS = List.of(ArticleEnrichment.Rating.A, ArticleEnrichment.Rating.B, ArticleEnrichment.Rating.C);

    static Specification<ArticleEnrichment> toTop() {
        return Spec.<ArticleEnrichment>on()
                .in("rating", TOPS)
                .build();
    }

    static Specification<ArticleEnrichment> toTop(Long id) {
        return Spec.<ArticleEnrichment>on()
                .in("rating", TOPS)
                .eq("id", id)
                .build();
    }

    static Specification<ArticleEnrichment> toSpec(ArticleEnrichment.Rating rating) {
        return Spec.<ArticleEnrichment>on()
                .eq("rating", rating)
                .build();
    }
}
