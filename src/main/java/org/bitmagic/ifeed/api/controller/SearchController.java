package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.SearchResultResponse;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private static final String TYPE_KEYWORD = "keyword";
    private static final String TYPE_SEMANTIC = "semantic";

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<Page<SearchResultResponse>> search(@AuthenticationPrincipal UserPrincipal principal,
                                                             @RequestParam String query,
                                                             @RequestParam(required = false, defaultValue = TYPE_KEYWORD) String type,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size) {
        ensureAuthenticated(principal);
        var normalizedType = type == null ? TYPE_KEYWORD : type.toLowerCase();
        if (!TYPE_KEYWORD.equals(normalizedType) && !TYPE_SEMANTIC.equals(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported search type");
        }

        var articlePage = articleService.searchArticles(query, page, size)
                .map(article -> new SearchResultResponse(
                        article.getId().toString(),
                        article.getTitle(),
                        article.getSummary(),
                        null));
        return ResponseEntity.ok(articlePage);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
