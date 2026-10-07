package org.bitmagic.ifeed.application.embedding;

import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.bitmagic.ifeed.domain.repository.ArticleEnrichmentRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.spec.ArticleEnrichmentSpec;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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

    @Autowired
    ArticleEnrichmentRepository articleEnrichmentRepository;

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

    @Test
    public void embedding() {
        Stream.iterate(0, i -> i + 1).skip(0).limit(280).forEach(i -> {
            Page<ArticleEnrichment> enrichments = articleEnrichmentRepository.findAll(ArticleEnrichmentSpec.toTop(), PageRequest.of(i, 100, Sort.by(Sort.Direction.DESC, "id")));
            List<Article> articles = articleRepository.findAllById(enrichments.stream().map(ArticleEnrichment::getId).toList());
            articles.forEach(article -> {
                articleEnhancedService.enhanced(article);
            });
        });
    }
}