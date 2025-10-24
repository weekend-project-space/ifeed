package org.bitmagic.ifeed.service.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.domain.spec.ArticleSpec;
import org.bitmagic.ifeed.service.feed.FeedIngestionService;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingScheduler {

    private final ArticleEmbeddingService articleEmbeddingService;

    private final UserEmbeddingService userEmbeddingService;

    private final UserRepository userRepository;

    private final ArticleRepository articleRepository;

    @Scheduled(initialDelayString = "${rss.fetcher.initial-delay:PT10S}",
            fixedDelayString = "${rss.fetcher.fixed-delay:PT30M}")
    public void embedding() {
        log.info("init embedding");
        articleRepository.findAll(ArticleSpec.noEmbeddingSpec(), Pageable.ofSize(100)).forEach(article -> {
            log.info("init embedding :{}", article.getTitle());
            articleEmbeddingService.buildArticleEmbedding(article);
            article.setEmbedding("1");
            articleRepository.save(article);
        });
        log.info("init user embedding");
        userRepository.findAll().forEach(user -> {
            log.info("init user embedding :{}", user.getUsername());
            userEmbeddingService.rebuildUserEmbedding(user.getId());
        });

    }
}
