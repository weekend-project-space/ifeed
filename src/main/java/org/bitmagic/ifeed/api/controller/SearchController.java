package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.SearchResultResponse;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.bitmagic.ifeed.application.search.SearchRetrievalService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
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
    private final SearchRetrievalService searchRetrievalService;

    @GetMapping
    public ResponseEntity<Page<SearchResultResponse>> search(@AuthenticationPrincipal UserPrincipal principal,
                                                             @RequestParam String query,
                                                             @RequestParam(required = false, defaultValue = TYPE_KEYWORD) String type,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size,
                                                             @RequestParam(required = false, defaultValue = SOURCE_OWNER) String source) {
        ensureAuthenticated(principal);
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Query must not be blank");
        }
        var normalizedType = type == null ? TYPE_KEYWORD : type.trim().toLowerCase(Locale.ROOT);
        if (!TYPE_KEYWORD.equals(normalizedType) && !TYPE_SEMANTIC.equals(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported search type");
        }

        var normalizedSource = source == null ? SOURCE_OWNER : source.trim().toLowerCase(Locale.ROOT);
        if (!SOURCE_OWNER.equals(normalizedSource) && !SOURCE_GLOBAL.equals(normalizedSource)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported source type");
        }
        var pageNumber = page == null || page < 0 ? 0 : page;
        var pageSize = size == null || size <= 0 ? 10 : Math.min(size, 100);
        var includeGlobal = SOURCE_GLOBAL.equals(normalizedSource);

        if (TYPE_SEMANTIC.equals(normalizedType)) {
            return ResponseEntity.ok(searchRetrievalService.hybridSearch(principal.getId(), query, includeGlobal, pageNumber, pageSize).map(article -> new SearchResultResponse(
                    article.id().toString(),
                    article.title(),
                    article.summary(),
                    article.thumbnail(),
                    article.feedTitle(),
                    formatRelativeTime(article.publishedAt()),
                    null
            )));
        }

        var articlePage = articleService.searchArticles(principal.getId(), query, includeGlobal, pageNumber, pageSize)
                .map(article -> new SearchResultResponse(
                        article.id().toString(),
                        article.title(),
                        article.summary(),
                        article.thumbnail(),
                        article.feedTitle(),
                        formatRelativeTime(article.publishedAt()),
                        null));
        return ResponseEntity.ok(articlePage);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private String formatRelativeTime(Instant instant) {
        if (instant == null) {
            return "刚刚";
        }
        var now = Instant.now();
        if (instant.isAfter(now)) {
            return "刚刚";
        }
        var duration = Duration.between(instant, now);
        if (duration.toMinutes() < 1) {
            return "刚刚";
        }
        if (duration.toMinutes() < 60) {
            return duration.toMinutes() + " 分钟前";
        }
        if (duration.toHours() < 24) {
            return duration.toHours() + " 小时前";
        }
        if (duration.toDays() < 7) {
            return duration.toDays() + " 天前";
        }
        return instant.toString().substring(0, Math.min(10, instant.toString().length()));
    }
}
