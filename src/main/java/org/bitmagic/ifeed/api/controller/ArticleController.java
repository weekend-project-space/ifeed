package org.bitmagic.ifeed.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ArticleDetailResponse;
import org.bitmagic.ifeed.api.response.ArticleSummaryResponse;
import org.bitmagic.ifeed.api.response.UserSubscriptionInsightResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.bitmagic.ifeed.domain.service.UserCollectionService;
import org.bitmagic.ifeed.application.recommendation.RecommendationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };
    private static final String SOURCE_OWNER = "owner";
    private static final String SOURCE_GLOBAL = "global";

    private final ArticleService articleService;

    private final UserCollectionService userCollectionService;

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<Page<ArticleSummaryResponse>> listArticles(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @RequestParam(required = false) Integer page,
                                                                     @RequestParam(required = false) Integer size,
                                                                     @RequestParam(required = false) String sort,
                                                                     @RequestParam(required = false) String feedId,
                                                                     @RequestParam(required = false, name = "tags") String tags,
                                                                     @RequestParam(required = false, name = "category") String category,
                                                                     @RequestParam(required = false, defaultValue = SOURCE_OWNER) String source) {
        ensureAuthenticated(principal);
        var normalizedTags = parseTags(tags);
        var normalizedCategory = normalizeCategory(category);
        var normalizedSource = normalizeSource(source);
        var includeGlobal = SOURCE_GLOBAL.equals(normalizedSource);
        var articlePage = articleService.listArticles(principal.getId(),
                        parseFeedId(feedId), normalizedTags, normalizedCategory, includeGlobal, page, size, sort)
                .map(this::toSummaryResponse);
        return ResponseEntity.ok(articlePage);
    }


    @GetMapping("/recommendations")
    public ResponseEntity<Page<ArticleSummaryResponse>> searchArticles(@AuthenticationPrincipal UserPrincipal principal,
                                                                       @RequestParam(defaultValue = "0") Integer page,
                                                                       @RequestParam(required = false) Integer size) {
        ensureAuthenticated(principal);

        return ResponseEntity.ok(recommendationService.recommend(principal.getId(), page, size).map(this::toSummaryResponse2));
    }

    @GetMapping("/insights")
    public ResponseEntity<UserSubscriptionInsightResponse> insights(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "20") Integer top) {
        ensureAuthenticated(principal);
        var toTs = (to == null || to.isBlank()) ? Instant.now() : Instant.parse(to.trim());
        var fromTs = (from == null || from.isBlank()) ? toTs.minus(Duration.ofDays(30)) : Instant.parse(from.trim());
        var resp = articleService.insights(principal.getId(), fromTs, toTs, top);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ArticleDetailResponse> getArticle(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable String articleId) {
        ensureAuthenticated(principal);
        var article = articleService.getArticle(IdentifierUtils.parseUuid(articleId, "article id"));
        var tags = extractTags(article.getTags());
        var collected = userCollectionService.isCollected(principal.getId(), article.getUid());
        var response = new ArticleDetailResponse(
                article.getUid().toString(),
                article.getTitle(),
                article.getContent(),
                article.getSummary(),
                article.getLink(),
                article.getThumbnail(),
                article.getEnclosure(),
                article.getFeed().getUid().toString(),
                resolveFeedTitle(article.getFeed() == null ? null : article.getFeed().getTitle()),
                formatTimestamp(article.getPublishedAt()),
                tags,
                collected);
        return ResponseEntity.ok(response);
    }

    private ArticleSummaryResponse toSummaryResponse(ArticleSummaryView article) {
        var tags = extractTags(article.tags());
        var publishedAt = article.publishedAt();
        return new ArticleSummaryResponse(
                article.id().toString(),
                article.title(),
                article.link(),
                article.summary(),
                article.thumbnail(),
                article.enclosure(),
                resolveFeedTitle(article.feedTitle()),
                formatTimestamp(publishedAt),
                tags,
                formatRelativeTime(publishedAt));
    }

    private ArticleSummaryResponse toSummaryResponse2(ArticleSummaryView article) {
        var publishedAt = article.publishedAt();
        return new ArticleSummaryResponse(
                article.id().toString(),
                article.title(),
                article.link(),
                article.summary(),
                article.thumbnail(),
                article.enclosure(),
                resolveFeedTitle(article.feedTitle()),
                formatTimestamp(publishedAt),
                null,
                formatRelativeTime(publishedAt));
    }

    private List<String> extractTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(raw, TAGS_TYPE);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String resolveFeedTitle(String feedTitle) {
        if (feedTitle == null || feedTitle.isBlank()) {
            return "未知来源";
        }
        return feedTitle;
    }

    private String formatTimestamp(Instant instant) {
        return instant == null ? null : instant.toString();
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

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private UUID parseFeedId(String feedId) {
        if (feedId == null || feedId.isBlank()) {
            return null;
        }
        return IdentifierUtils.parseUuid(feedId, "feed id");
    }

    private java.util.Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptySet();
        }
        var normalized = Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(TreeSet::new));
        return normalized.isEmpty() ? Collections.emptySet() : normalized;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return SOURCE_OWNER;
        }
        var normalized = source.trim().toLowerCase(Locale.ROOT);
        if (!SOURCE_OWNER.equals(normalized) && !SOURCE_GLOBAL.equals(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported source type");
        }
        return normalized;
    }
}
