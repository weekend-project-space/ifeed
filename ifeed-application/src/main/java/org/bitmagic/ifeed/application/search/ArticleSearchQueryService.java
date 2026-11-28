package org.bitmagic.ifeed.application.search;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ArticleWithStateResponse;
import org.bitmagic.ifeed.api.response.SearchResultResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.bitmagic.ifeed.domain.service.UserArticleStateService;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleSearchQueryService {
    private static final String TYPE_KEYWORD = "keyword";
    private static final String TYPE_SEMANTIC = "semantic";
    private static final String SOURCE_OWNER = "owner";
    private static final String SOURCE_GLOBAL = "global";
    private final ArticleService articleService;
    private final SearchRetrievalService searchRetrievalService;
    private final UserArticleStateService articleStates;

    public Page<ArticleWithStateResponse<SearchResultResponse>> search(Integer userId, String query, String type, String source, String feedId, String tags, String category, Pageable pageable) {
        if (userId == null || userId <= 0) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
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
            var results = searchRetrievalService.hybridSearch(userId, query, includeGlobal,
                    feedUid, tagSet, category, pageNumber, pageSize).map(article -> new SearchResultResponse(
                    article.id().toString(),
                    article.title(),
                    article.summary(),
                    article.thumbnail(),
                    article.feedTitle(),
                    formatRelativeTime(article.publishedAt()),
                    null,
                    article.feedId() != null ? article.feedId().toString() : null
            ));
            return articleStates.attach(userId, results,
                    article -> UUID.fromString(article.id()));
        }

        var articlePage = articleService.searchArticles(userId, query, includeGlobal, feedUid, tagSet, category, pageable)
                .map(article -> new SearchResultResponse(
                        article.id().toString(),
                        article.title(),
                        article.summary(),
                        article.thumbnail(),
                        article.feedTitle(),
                        formatRelativeTime(article.publishedAt()),
                        null,
                        article.feedId() != null ? article.feedId().toString() : null));
        return articleStates.attach(userId, articlePage,
                article -> UUID.fromString(article.id()));
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) return Set.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim).filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
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
