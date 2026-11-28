package org.bitmagic.ifeed.application.article;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ArticleWithStateResponse;
import org.bitmagic.ifeed.application.recommendation.RecRequest;
import org.bitmagic.ifeed.application.recommendation.RecResponse;
import org.bitmagic.ifeed.application.recommendation.RecommendationService;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.bitmagic.ifeed.domain.service.UserArticleStateService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleRecommendationQueryService {
    private final RecommendationService recommendationService;
    private final ArticleService articleService;
    private final UserArticleStateService articleStates;

    public Page<ArticleWithStateResponse<RecResponse>> recommendations(Integer userId, int page, int size) {
        int currentUserId = userId == null ? 0 : userId;
        return articleStates.attach(currentUserId, recommendationService.recommend(
                new RecRequest(currentUserId, "home", Map.of(), Map.of()), page, size), RecResponse::id);
    }

    public List<ArticleWithStateResponse<RecResponse>> related(Integer userId, UUID articleId, int topK) {
        int currentUserId = userId == null ? 0 : userId;
        Long itemId = articleService.getArticle(articleId).getId();
        return articleStates.attach(currentUserId, recommendationService.recommend(
                new RecRequest(currentUserId, "details", Map.of(), Map.of("itemId", itemId)), topK), RecResponse::id);
    }

}
