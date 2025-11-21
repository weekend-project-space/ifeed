package org.bitmagic.ifeed.application.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.domain.spec.ArticleSpecs;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingScheduler {

    private final ArticleEmbeddingService articleEmbeddingService;

    private final UserEmbeddingService userEmbeddingService;

    private final UserRepository userRepository;

    private final ArticleRepository articleRepository;

    private final CacheManager cacheManager;

    @Scheduled(initialDelayString = "${app.embedding.user.initial-delay:PT10S}",
            fixedDelayString = "${app.embedding.user.fixed-delay:PT30M}")
    public void userEmbedding() {
        log.info("begin gen user embedding");
        try {
            userRepository.findAll().forEach(user -> {
                log.info("init user embedding :{}", user.getUsername());
                try {
                    userEmbeddingService.rebuildUserEmbedding(user.getId()).ifPresent(userEmbedding -> {
                        evictU2I2ICache(user.getId());
                        evictU2ICache(user.getId());
                    });
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
            Stream.iterate(0, i -> i + 1).skip(50).forEach(i -> {
                articleRepository.findAll(ArticleSpecs.noEmbeddingSpec(), Pageable.ofSize(10)).stream().parallel().forEach(article -> {
                    try {
                        articleEmbeddingService.buildArticleEmbedding(article);
                        log.info("init embedding :{}", article.getTitle());
                    } catch (RuntimeException e) {
                        log.warn("init article embedding", e);
                    }
                });
            });
        } catch (RuntimeException e) {
            log.warn("article embedding", e);
        }
        log.info("end init article embedding");
    }


    public void evictU2I2ICache(Integer userId) {
        var cache = cacheManager.getCache("U2I2I");
        if (cache != null) {
            cache.evict(userId);
        }
    }

    public void evictU2ICache(Integer userId) {
        var cache = cacheManager.getCache("U2I");
        if (cache != null) {
            cache.evict(userId);
        }
    }
}
