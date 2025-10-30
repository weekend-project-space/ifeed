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

    @Scheduled(initialDelayString = "${rss.user.initial-delay:PT10S}",
            fixedDelayString = "${rss.user.fixed-delay:PT30M}")
    public void userEmbedding() {
        log.info("init user embedding");
        try {
            userRepository.findAll().forEach(user -> {
                log.info("init user embedding :{}", user.getUsername());
                try {
                    userEmbeddingService.rebuildUserEmbedding(user.getId());
                } catch (RuntimeException e) {
                    log.warn("init user embedding", e);
                }
            });
        } catch (RuntimeException e) {
            log.warn("user embedding", e);
        }

    }


    @Scheduled(initialDelayString = "${rss.document.initial-delay:PT10S}",
            fixedDelayString = "${rss.document.fixed-delay:PT30M}")
    public void docEmbedding() {
        log.info("init article embedding");
        try {
            articleRepository.findAll(ArticleSpec.noEmbeddingSpec(), Pageable.ofSize(100)).stream().parallel().forEach(article -> {
                try {
                    log.info("init embedding :{}", article.getTitle());
                    articleEmbeddingService.buildArticleEmbedding(article);
                    article.setEmbedding("1");
                    articleRepository.save(article);
                } catch (RuntimeException e) {
                    log.warn("init article embedding", e);
                }
            });
        } catch (RuntimeException e) {
            log.warn("article embedding", e);
        }
    }
}
