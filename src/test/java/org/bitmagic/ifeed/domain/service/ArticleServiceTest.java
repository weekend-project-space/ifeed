package org.bitmagic.ifeed.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.spec.ArticleSpecs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class ArticleServiceTest {

    @Autowired
    ArticleRepository articleRepository;
    @Autowired
    ArticleService articleService;

    @Test
    void saveAllAndFlush() {

        Stream.iterate(0, i -> i + 1).limit(1000).forEach(i -> {
            articleService.saveAllAndFlush(articleRepository.findAll(PageRequest.of(i, 10, Sort.by(Sort.Direction.DESC, "id"))).getContent());
            log.info("save all and flush");
        });
    }
}