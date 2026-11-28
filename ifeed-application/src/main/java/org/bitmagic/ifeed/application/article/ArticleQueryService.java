package org.bitmagic.ifeed.application.article;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.*;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.MixFeedRepository;
import org.bitmagic.ifeed.domain.service.*;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.infrastructure.util.DateUtils;
import org.bitmagic.ifeed.infrastructure.util.FaviconResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleQueryService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {};
    private static final String SOURCE_OWNER = "owner";
    private static final String SOURCE_GLOBAL = "global";
    private final ArticleService articleService;
    private final ArticleEnrichmentService articleEnrichmentService;
    private final UserCollectionService userCollectionService;
    private final UserLikeService userLikeService;
    private final FeedRepository feedRepository;
    private final MixFeedRepository mixFeedRepository;
    private final MixFeedService mixFeedService;
    private final UserArticleStateService articleStates;

    public Page<ArticleWithStateResponse<ArticleSummaryResponse>> listArticles(Integer userId, String feedId, String tags, String category, String source, Pageable pageable) {
        var normalizedTags = parseTags(tags);
        var normalizedCategory = normalizeCategory(category);
        var normalizedSource = normalizeSource(source);
        var includeGlobal = SOURCE_GLOBAL.equals(normalizedSource);

        UUID feedUuid = parseFeedId(feedId);
        Page<ArticleSummaryView> articlePage;

        // If feedId is provided, auto-detect Feed or MixFeed
        userId = userId == null ? 0 : userId;
        if (feedUuid != null) {
            // Try Feed first
            var feedOpt = feedRepository.findByUid(feedUuid);
            if (feedOpt.isPresent()) {
                // Regular Feed - use existing logic
                articlePage = articleService.listArticles(userId,
                        feedUuid, normalizedTags, normalizedCategory, includeGlobal, pageable);
            } else {
                // Try MixFeed
                var mixFeedOpt = mixFeedRepository.findByUid(feedUuid);
                if (mixFeedOpt.isPresent()) {
                    // MixFeed - use MixFeedService filtered articles
                    articlePage = mixFeedService.getFilteredArticles(feedUuid, normalizedTags, userId, pageable);
                } else {
                    throw new ApiException(HttpStatus.NOT_FOUND, "Feed or MixFeed not found");
                }
            }
        } else {
            // No feedId - use existing logic
            articlePage = articleService.listArticles(userId,
                    null, normalizedTags, normalizedCategory, includeGlobal, pageable);
        }

        return articleStates.attach(userId, articlePage.map(this::toSummaryResponse),
                article -> UUID.fromString(article.id()));
    }

    public ArticleDetailResponse getArticle(Integer userId, UUID articleId) {
        var article = articleService.getArticle(articleId);
        var tags = extractTags(article.getTags());
        ArticleEnrichment enrichment = articleEnrichmentService.getEnrichment(article.getId());
        var collection = userId == null || userId == 0 ? null
                : userCollectionService.findState(userId, article.getId()).orElse(null);
        var liked = userId != null && userId > 0 && userLikeService.isLiked(userId, article.getId());
        Feed feed = article.getFeed();
        var response = new ArticleDetailResponse(
                article.getUid().toString(),
                article.getTitle(),
                article.getContent(),
                Objects.nonNull(enrichment) ? enrichment.getAiSummary() : null,
                Objects.nonNull(enrichment) ? enrichment.getMindMap() : null,
                articleEnrichmentService.requiresUpgrade(article.getId()),
                article.getLink(),
                article.getThumbnail(),
                article.getEnclosure(),
                article.getEnclosureType(),
                feed.getUid().toString(),
                resolveFeedTitle(feed.getTitle()),
                FaviconResolver.resolve(feed.getSiteUrl(), feed.getUrl()),
                formatTimestamp(article.getPublishedAt()),
                tags,
                collection != null,
                liked,
                collection == null ? null : collection.folderId());
        return response;
    }

    public UserSubscriptionInsightResponse insights(Integer userId, String from, String to, Integer top) {
        userId = userId == null ? 0 : userId;
        var toTs = (to == null || to.isBlank()) ? Instant.now() : Instant.parse(to.trim());
        var fromTs = (from == null || from.isBlank()) ? toTs.minus(Duration.ofDays(30)) : Instant.parse(from.trim());
        var resp = articleService.insights(userId, fromTs, toTs, top);
        return resp;
    }

    private ArticleSummaryResponse toSummaryResponse(ArticleSummaryView article) {
        var tags = extractTags(article.tags());
        var publishedAt = article.publishedAt();
        return new ArticleSummaryResponse(
                article.id().toString(),
                article.title(),
                article.summary(),
                article.thumbnail(),
                article.enclosure(),
                resolveFeedTitle(article.feedTitle()),
                article.feedAvatar(),
                formatTimestamp(article.publishedAt()),
                tags,
                DateUtils.formatRelativeTime(publishedAt),
                article.feedId() != null ? article.feedId().toString() : null);
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
