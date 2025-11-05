package org.bitmagic.ifeed.application.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.domain.spec.ArticleSpec;
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

    @Scheduled(initialDelayString = "${app.embedding.user.initial-delay:PT10S}",
            fixedDelayString = "${app.embedding.user.fixed-delay:PT30M}")
    public void userEmbedding() {
        log.info("begin gen user embedding");
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
        log.info("end gen user embedding");
    }


    @Scheduled(initialDelayString = "${app.embedding.document.initial-delay:PT10S}",
            fixedDelayString = "${app.embedding.document.fixed-delay:PT30M}")
    public void documentEmbedding() {
        log.info("begin init article embedding");
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
        log.info("end init article embedding");
    }
}
