package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.*;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.application.article.ArticleQueryService;
import org.bitmagic.ifeed.application.article.ArticleRecommendationQueryService;
import org.bitmagic.ifeed.application.article.ArticleEnrichmentApplicationService;
import org.bitmagic.ifeed.application.recommendation.RecResponse;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleQueryService articles;
    private final ArticleRecommendationQueryService recommendations;
    private final ArticleEnrichmentApplicationService enrichment;

    @GetMapping
    public ResponseEntity<Page<ArticleWithStateResponse<ArticleSummaryResponse>>> listArticles(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String feedId,
            @RequestParam(required = false, name = "tags") String tags,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "owner") String source,
            @PageableDefault(sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(articles.listArticles(userId(principal), feedId, tags, category, source, pageable));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<Page<ArticleWithStateResponse<RecResponse>>> rec(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(recommendations.recommendations(userId(principal), page, size));
    }

    @GetMapping("/{articleId}/recommendations")
    public ResponseEntity<List<ArticleWithStateResponse<RecResponse>>> rec(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable String articleId,
            @RequestParam(required = false, defaultValue = "6") Integer topK) {
        return ResponseEntity.ok(recommendations.related(userId(principal), IdentifierUtils.parseUuid(articleId, "article id"), topK));
    }

    @GetMapping("/insights")
    public ResponseEntity<UserSubscriptionInsightResponse> insights(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "20") Integer top) {
        return ResponseEntity.ok(articles.insights(userId(principal), from, to, top));
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ArticleDetailResponse> getArticle(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable String articleId) {
        return ResponseEntity.ok(articles.getArticle(userId(principal), IdentifierUtils.parseUuid(articleId, "article id")));
    }

    @PostMapping("/{articleId}/enrich")
    public ResponseEntity<ArticleEnrichResponse> enrichOnDemand(@AuthenticationPrincipal UserPrincipal principal,
                                                               @PathVariable String articleId) {
        if (principal == null || User.Plan.FREE.equals(principal.getCurrentPlan())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(enrichment.enrich(IdentifierUtils.parseUuid(articleId, "article id")));
    }

    private Integer userId(UserPrincipal principal) { return principal == null ? null : principal.getId(); }
}
