package org.bitmagic.ifeed.domain.service;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class ArticleEnrichmentServiceTest {

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    ArticleEnrichmentRepository enrichmentRepository;

    @Autowired
    ArticleEnrichmentService articleEnrichmentService;

    @Test
    void enrichArticle() {
        Stream.iterate(0, i -> i + 1).skip(0).limit(30).forEach(i -> {
//            Specification<Article> specification = Spec.<Article>on().and((root, query, criteriaBuilder) -> {
////                return criteriaBuilder.isNull(root.get("feed").get("category"));
////                return criteriaBuilder.equal(root.get("feed").get("id"),74);
////                return criteriaBuilder.equal(root.get("feed").get("category"),"tech");
//
//            }).build();
            List<Article> articles = articleRepository.findAll(PageRequest.of(i, 100, Sort.by(Sort.Direction.DESC, "id"))).getContent();
            articles.forEach(article -> {
                if (articleEnrichmentService.existsById(article.getId())) {
                    return;
                }
                articleEnrichmentService.enrichArticle(article);
            });
        });
    }

    @Test
    void enrichArticle2() {
        enrichmentRepository.findAll(ArticleEnrichmentSpec.toSpec(ArticleEnrichment.Rating.C), PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id"))).forEach(articleEnrichment -> {
            Article article = articleRepository.findById(articleEnrichment.getId()).orElseThrow();
            articleEnrichmentService.enrichArticle(article);
        });
    }
}