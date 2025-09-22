package org.bitmagic.ifeed.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ArticleDetailResponse;
import org.bitmagic.ifeed.api.response.ArticleSummaryResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.entity.Feed;
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
                                                                     @RequestParam(required = false) String sort) {
        ensureAuthenticated(principal);
        var articlePage = articleService.listArticles(page, size, sort)
                .map(this::toSummaryResponse);
        return ResponseEntity.ok(articlePage);
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ArticleDetailResponse> getArticle(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable String articleId) {
        ensureAuthenticated(principal);
        var article = articleService.getArticle(IdentifierUtils.parseUuid(articleId, "article id"));
        var tags = extractTags(article);
        var response = new ArticleDetailResponse(
                article.getId().toString(),
                article.getTitle(),
                article.getContent(),
                article.getSummary(),
                article.getLink(),
                resolveFeedTitle(article.getFeed()),
                formatTimestamp(article.getPublishedAt()),
                tags);
        return ResponseEntity.ok(response);
    }

    private ArticleSummaryResponse toSummaryResponse(Article article) {
        var tags = extractTags(article);
        var publishedAt = article.getPublishedAt();
        return new ArticleSummaryResponse(
                article.getId().toString(),
                article.getTitle(),
                article.getLink(),
                article.getSummary(),
                resolveFeedTitle(article.getFeed()),
                formatTimestamp(publishedAt),
                tags,
                formatRelativeTime(publishedAt));
    }

    private List<String> extractTags(Article article) {
        var raw = article.getTags();
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(raw, TAGS_TYPE);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String resolveFeedTitle(Feed feed) {
        if (feed == null || feed.getTitle() == null || feed.getTitle().isBlank()) {
            return "未知来源";
        }
        return feed.getTitle();
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
}
