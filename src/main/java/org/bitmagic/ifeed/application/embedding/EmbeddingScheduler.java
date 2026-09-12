package org.bitmagic.ifeed.application.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.radar.RadarSnapshotBuildService;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.domain.spec.ArticleSpecs;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingScheduler {

    private final ArticleEnhancedService articleEnhancedService;

    private final UserEmbeddingService userEmbeddingService;

    private final UserRepository userRepository;

    private final ArticleRepository articleRepository;

    private final CacheManager cacheManager;

    private final RadarSnapshotBuildService buildService;

    @Scheduled(initialDelayString = "${app.embedding.user.initial-delay:PT10S}",
            fixedDelayString = "${app.embedding.user.fixed-delay:PT30M}")
    public void userEmbedding() {

        try {
            List<User> users = userRepository.findAll();
            log.info("begin gen user total: {} embedding", users.size());
            users.forEach(user -> {
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
            Stream.iterate(0, i -> i + 1).limit(10).forEach(i -> {
                articleRepository.findAll(ArticleSpecs.noEmbeddingSpec(), PageRequest.of(i, 300)).stream().forEach(article -> {
                    try {
                        articleEnhancedService.enhanced(article);
                        log.debug("init embedding :{}", article.getTitle());
                    } catch (RuntimeException e) {
                        log.warn("init article embedding", e);
                    }
                });
            });
        } catch (RuntimeException e) {
            log.warn("article embedding", e);
        }
        log.info("end init article embedding");
//        雷达不需要实时更新
        if (LocalDateTime.now().getHour() == 9 || LocalDateTime.now().getHour() == 6) {
            buildService.build();
        }
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
