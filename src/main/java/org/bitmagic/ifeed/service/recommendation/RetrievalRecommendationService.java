package org.bitmagic.ifeed.service.recommendation;

import com.rometools.utils.Strings;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.RecommendationProperties;
import org.bitmagic.ifeed.domain.entity.UserEmbedding;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
import org.bitmagic.ifeed.service.ArticleService;
import org.bitmagic.ifeed.service.retrieval.SearchRetrievalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangrd
 * @date 2025/10/28
 **/
@Slf4j
@Service
public class RetrievalRecommendationService implements RecommendationService {

    private final SearchRetrievalService searchRetrievalService;
    private final ArticleService articleService;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final Map<UUID, List<UUID>> user2Items = new ConcurrentHashMap<>();
    private final UserEmbedding defaultUserEmbedding;

    public RetrievalRecommendationService(RecommendationProperties properties, SearchRetrievalService searchRetrievalService, ArticleService articleService, UserEmbeddingRepository userEmbeddingRepository) {
        this.searchRetrievalService = searchRetrievalService;
        this.articleService = articleService;
        this.userEmbeddingRepository = userEmbeddingRepository;
        defaultUserEmbedding = new UserEmbedding(null, null, properties.getDefaultProfile(), null);
    }


    @Override
    public Page<ArticleSummaryView> recommend(UUID userId, int page, int size) {
        long start = System.currentTimeMillis();
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        return userEmbeddingRepository.findById(userId).or(() -> Optional.of(defaultUserEmbedding)).map(userEmbedding -> {
            if (Strings.isBlank(userEmbedding.getContent())) {
                return new PageImpl<ArticleSummaryView>(Collections.emptyList());
            } else {
                List<UUID> cachedIds = user2Items.get(userId);
                if (safePage == 0 || cachedIds == null) {
                    cachedIds = searchRetrievalService.hybridSearch(userId, userEmbedding.getEmbedding(), userEmbedding.getContent(), true, -1);
                    user2Items.put(userId, cachedIds);
                }
                log.info("recall:{}ms", System.currentTimeMillis() - start);
                return articleService.findIds2Article(cachedIds, safePage, safeSize);
            }

        }).orElse(new PageImpl<>(Collections.emptyList()));
    }
}
