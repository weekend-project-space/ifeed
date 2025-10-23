package org.bitmagic.ifeed.domain.spec;

import org.bitmagic.ifeed.domain.entity.Article;
import org.springframework.data.jpa.domain.Specification;
import space.bitmagic.data.jpa.Specifications;

/**
 * @author yangrd
 * @date 2025/10/23
 **/
public interface ArticleSpec {

    static Specification<Article> noEmbeddingSpec() {
        return Specifications.<Article>and()
                .predicate((root, query, criteriaBuilder) -> {
                    return criteriaBuilder.isNull(root.get("embedding"));
                }).build();
    }
}
