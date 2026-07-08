package org.bitmagic.ifeed.application.embedding;

import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Stream;

@SpringBootTest
@Slf4j
class ArticleEmbeddingServiceTest {

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    ArticleEnhancedService articleEnhancedService;

    @Test
    void buildArticleEmbedding() {
        Stream.iterate(0, i -> i + 1).skip(0).limit(1).forEach(i -> {
            Specification<Article> specification = Spec.<Article>on().and((root, query, criteriaBuilder) -> {
                return criteriaBuilder.isNotNull(root.get("feed").get("category"));
            }).build();
            List<Article> articles = articleRepository.findAll(specification, PageRequest.of(i, 10, Sort.by(Sort.Direction.DESC, "id"))).getContent();
            articles.parallelStream().forEach(article -> {
                articleEnhancedService.enhanced(article);
            });
        });
    }
}