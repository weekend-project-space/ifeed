package org.bitmagic.ifeed.domain.service;

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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class ArticleEnrichmentServiceTest {

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    ArticleEnrichmentService articleEnrichmentService;

    @Test
    void enrichArticle() {
        Stream.iterate(0, i -> i + 1).skip(0).limit(1).forEach(i -> {
            Specification<Article> specification = Spec.<Article>on().and((root, query, criteriaBuilder) -> {
                return criteriaBuilder.equal(root.get("feed").get("id"), 74);
            }).build();
            List<Article> articles = articleRepository.findAll(specification,PageRequest.of(i, 10, Sort.by(Sort.Direction.DESC, "id"))).getContent();
            articles.forEach(article -> {
                articleEnrichmentService.enrichArticle(article);
            });
        });
    }
}