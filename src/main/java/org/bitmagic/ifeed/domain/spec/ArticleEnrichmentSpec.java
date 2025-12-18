package org.bitmagic.ifeed.domain.spec;

import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;

/**
 * @author yangrd
 * @date 2025/12/17
 **/
public interface ArticleEnrichmentSpec {
    List<ArticleEnrichment.Rating> TOPS = Arrays.asList(ArticleEnrichment.Rating.values()).stream().filter(rating -> !ArticleEnrichment.Rating.D.equals(rating)).toList();

    static Specification<ArticleEnrichment> top() {
        return Spec.<ArticleEnrichment>on()
                .in("rating", TOPS)
                .build();
    }

    static Specification<ArticleEnrichment> top(Long id) {
        return Spec.<ArticleEnrichment>on()
                .in("rating", TOPS)
                .eq("id", id)
                .build();
    }
}
