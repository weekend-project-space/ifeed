package org.bitmagic.ifeed.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ArticleDetailResponse;
import org.bitmagic.ifeed.api.response.ArticleSummaryResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<Page<ArticleSummaryResponse>> listArticles(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @RequestParam(required = false) Integer page,
                                                                     @RequestParam(required = false) Integer size,
                                                                     @RequestParam(required = false) String sort,
                                                                     @RequestParam(required = false) String feedId) {
        ensureAuthenticated(principal);
        var articlePage = articleService.listArticles(page, size, sort, parseFeedId(feedId))
                .map(this::toSummaryResponse);
        return ResponseEntity.ok(articlePage);
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ArticleDetailResponse> getArticle(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable String articleId) {
        ensureAuthenticated(principal);
        var article = articleService.getArticle(IdentifierUtils.parseUuid(articleId, "article id"));
        var tags = extractTags(article.getTags());
        var response = new ArticleDetailResponse(
                article.getId().toString(),
                article.getTitle(),
                article.getContent(),
                article.getSummary(),
                article.getLink(),
                article.getThumbnail(),
                article.getEnclosure(),
                resolveFeedTitle(article.getFeed() == null ? null : article.getFeed().getTitle()),
                formatTimestamp(article.getPublishedAt()),
                tags);
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
}
