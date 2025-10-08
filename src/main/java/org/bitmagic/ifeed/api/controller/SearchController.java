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

import java.util.Locale;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private static final String TYPE_KEYWORD = "keyword";
    private static final String TYPE_SEMANTIC = "semantic";
    private static final String SOURCE_OWNER = "owner";
    private static final String SOURCE_GLOBAL = "global";

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<Page<SearchResultResponse>> search(@AuthenticationPrincipal UserPrincipal principal,
                                                             @RequestParam String query,
                                                             @RequestParam(required = false, defaultValue = TYPE_KEYWORD) String type,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size,
                                                             @RequestParam(required = false, defaultValue = SOURCE_OWNER) String source) {
        ensureAuthenticated(principal);
        var normalizedType = type == null ? TYPE_KEYWORD : type.trim().toLowerCase(Locale.ROOT);
        if (!TYPE_KEYWORD.equals(normalizedType) && !TYPE_SEMANTIC.equals(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported search type");
        }

        var normalizedSource = source == null ? SOURCE_OWNER : source.trim().toLowerCase(Locale.ROOT);
        if (!SOURCE_OWNER.equals(normalizedSource) && !SOURCE_GLOBAL.equals(normalizedSource)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported source type");
        }
        var includeGlobal = SOURCE_GLOBAL.equals(normalizedSource);

        var articlePage = articleService.searchArticles(principal.getId(), query, includeGlobal, page, size)
                .map(article -> new SearchResultResponse(
                        article.id().toString(),
                        article.title(),
                        article.summary(),
                        null));
        return ResponseEntity.ok(articlePage);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
