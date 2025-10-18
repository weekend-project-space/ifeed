package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ArticleRecommendationResponse;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.recommendation.ArticleRecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles/recommendations")
@RequiredArgsConstructor
public class ArticleRecommendationController {

    private final ArticleRecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<ArticleRecommendationResponse>> recommendations(@AuthenticationPrincipal UserPrincipal principal,
                                                                               @RequestParam(required = false) Integer size,
                                                                               @RequestParam(required = false, defaultValue = "owner") String source) {
        ensureAuthenticated(principal);
        var parsedSource = ArticleRecommendationService.RecommendationSource.from(source);
        var items = recommendationService.getRecommendations(principal.getId(), size, parsedSource);
        return ResponseEntity.ok(items);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
