package org.bitmagic.ifeed.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.spec.ArticleSpecs;
import org.bitmagic.ifeed.infrastructure.ai.AiContent;
import org.bitmagic.ifeed.infrastructure.ai.AiContentService;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.bitmagic.ifeed.infrastructure.util.ContentCleaner;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class ArticleServiceTest {

    @Autowired
    ArticleRepository articleRepository;
    @Autowired
    ArticleService articleService;

    @Autowired
    AiContentService aiContentService;

    @Test
    void saveAllAndFlush() {

        Stream.iterate(0, i -> i + 1).limit(4060).forEach(i -> {
//            Specification<Article> specification = Spec.<Article>on().and((root, query, criteriaBuilder) -> {
//                return criteriaBuilder.equal(root.get("feed").get("id"), 107);
//            }).build();
            List<Article> articles = articleRepository.findAll( PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"))).getContent();
            articles.forEach(article -> {
                ContentCleaner.Content content = ContentCleaner.clean(article.getContent());
                if(StringUtils.isNotBlank(content.textContent())) {
//                    article.setContent(content.mdContent());
//                    log.info(content.mdContent());
                    AiContent aiContent = aiContentService.analyze(article.getTitle(), content.textContent());
                    article.setTags(JSON.toJson(new TreeSet<>(aiContent.tags())));
                    article.setCategory(aiContent.category());
                    article.setSummary(aiContent.summary());

                }

            });
            articleService.saveAllAndFlush(articles);
            log.info("save all and flush");
        });
    }
}