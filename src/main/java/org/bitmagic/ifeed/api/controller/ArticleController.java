package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ArticleDetailResponse;
import org.bitmagic.ifeed.api.response.ArticleSummaryResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.ArticleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<List<ArticleSummaryResponse>> listArticles(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @RequestParam(required = false) Integer page,
                                                                     @RequestParam(required = false) Integer size,
                                                                     @RequestParam(required = false) String sort) {
        ensureAuthenticated(principal);
        var articles = articleService.listArticles(page, size, sort).stream()
                .map(article -> new ArticleSummaryResponse(
                        article.getId().toString(),
                        article.getTitle(),
                        article.getLink(),
                        article.getSummary()))
                .toList();
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ArticleDetailResponse> getArticle(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable String articleId) {
        ensureAuthenticated(principal);
        var article = articleService.getArticle(IdentifierUtils.parseUuid(articleId, "article id"));
        var response = new ArticleDetailResponse(
                article.getId().toString(),
                article.getTitle(),
                article.getContent(),
                article.getSummary());
        return ResponseEntity.ok(response);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
