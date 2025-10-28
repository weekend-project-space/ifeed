package org.bitmagic.ifeed.service.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
import org.bitmagic.ifeed.service.retrieval.SearchRetrievalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

/**
 * @author yangrd
 * @date 2025/10/28
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalRecommendationService implements RecommendationService {

    private final SearchRetrievalService searchRetrievalService;

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserEmbeddingRepository userEmbeddingRepository;


    @Override
    public Page<ArticleSummaryView> recommend(UUID userId, int page, int size) {
        long start = System.currentTimeMillis();
        return userEmbeddingRepository.findById(userId).map(userEmbedding -> {
            Page<ArticleSummaryView> data = searchRetrievalService.hybridSearch(userId, userEmbedding.getEmbedding(), userEmbedding.getContent(), true, page, size);
            log.info("recall:{}ms", System.currentTimeMillis() - start);
            return data;
        }).orElse(new PageImpl<>(Collections.emptyList()));
    }
}
