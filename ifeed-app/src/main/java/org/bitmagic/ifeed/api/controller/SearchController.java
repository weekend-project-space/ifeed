package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.SearchResultResponse;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.bitmagic.ifeed.application.search.SearchRetrievalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import java.util.Set;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

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
                                                             @RequestParam(required = false, defaultValue = TYPE_SEMANTIC) String type,
                                                             @RequestParam(required = false, defaultValue = SOURCE_OWNER) String source,
                                                             @RequestParam(required = false) String feedId,
                                                             @RequestParam(required = false) String tags,
                                                             @RequestParam(required = false) String category,
                                                             @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        ensureAuthenticated(principal);
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Query must not be blank");
        }
        var normalizedType = type == null ? TYPE_SEMANTIC : type.trim().toLowerCase(Locale.ROOT);
        if (!TYPE_KEYWORD.equals(normalizedType) && !TYPE_SEMANTIC.equals(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported search type");
        }

        var normalizedSource = source == null ? SOURCE_OWNER : source.trim().toLowerCase(Locale.ROOT);
        if (!SOURCE_OWNER.equals(normalizedSource) && !SOURCE_GLOBAL.equals(normalizedSource)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported source type");
        }
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();
        var includeGlobal = SOURCE_GLOBAL.equals(normalizedSource);
        UUID feedUid = feedId == null || feedId.isBlank() ? null : IdentifierUtils.parseUuid(feedId, "feed id");
        Set<String> tagSet = parseTags(tags);

        if (TYPE_SEMANTIC.equals(normalizedType)) {
            return ResponseEntity.ok(searchRetrievalService.hybridSearch(principal.getId(), query, includeGlobal,
                    feedUid, tagSet, category, pageNumber, pageSize).map(article -> new SearchResultResponse(
                    article.id().toString(),
                    article.title(),
                    article.summary(),
                    article.thumbnail(),
                    article.feedTitle(),
                    formatRelativeTime(article.publishedAt()),
                    null,
                    article.feedId() != null ? article.feedId().toString() : null
            )));
        }

        var articlePage = articleService.searchArticles(principal.getId(), query, includeGlobal, feedUid, tagSet, category, pageable)
                .map(article -> new SearchResultResponse(
                        article.id().toString(),
                        article.title(),
                        article.summary(),
                        article.thumbnail(),
                        article.feedTitle(),
                        formatRelativeTime(article.publishedAt()),
                        null,
                        article.feedId() != null ? article.feedId().toString() : null));
        return ResponseEntity.ok(articlePage);
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) return Set.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim).filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
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
