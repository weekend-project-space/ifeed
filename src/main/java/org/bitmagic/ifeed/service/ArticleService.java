package org.bitmagic.ifeed.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.utils.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.api.response.UserSubscriptionInsightResponse;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final ArticleRepository articleRepository;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };

    public Page<ArticleSummaryView> listArticles(Integer ownerId,
                                                 UUID feedUid,
                                                 Set<String> tags,
                                                 String category,
                                                 boolean includeGlobal, Integer page,
                                                 Integer size,
                                                 String sort) {
        var pageable = buildPageable(page, size, sort);
        var tagPattern = buildTagPattern(tags);
        var scopeOwnerId = Objects.nonNull(feedUid) || includeGlobal ? null : ownerId;
        return articleRepository.findArticleSummaries(feedUid, tagPattern, category, scopeOwnerId, pageable);
    }


    /**
     * 对查询执行混合检索：BM25 + 向量召回，并融合得分返回分页结果。
     */
    public Page<ArticleSummaryView> findIds2Article(List<UUID> artIds,
                                                    int page,
                                                    int size) {

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        if (artIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(safePage, safeSize), 0);
        }

        int fromIndex = Math.min(safePage * safeSize, artIds.size());
        int toIndex = Math.min(fromIndex + safeSize, artIds.size());
        if (fromIndex >= artIds.size()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(safePage, safeSize), artIds.size());
        }

        List<UUID> pageIds = artIds.subList(fromIndex, toIndex);

        Map<UUID, ArticleSummaryView> summaries = articleRepository.findArticleSummariesByUids(pageIds).stream()
                .collect(Collectors.toMap(ArticleSummaryView::id, summary -> summary));

        List<ArticleSummaryView> ordered = pageIds.stream()
                .map(summaries::get)
                .filter(Objects::nonNull)
                .toList();
        return new PageImpl<>(ordered, PageRequest.of(safePage, safeSize), artIds.size());
    }

    public Article getArticle(UUID articleUid) {
        return articleRepository.findOne((root, query, cb) -> cb.equal(root.get("uid"), articleUid))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    public Page<ArticleSummaryView> searchArticles(Integer ownerId,
                                                   String query,
                                                   boolean includeGlobal,
                                                   Integer page,
                                                   Integer size) {
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Query must not be blank");
        }
        var pageable = buildPageable(page, size, "publishedAt,desc");
        var term = "%" + query.trim().toLowerCase() + "%";
        var scopeOwnerId = includeGlobal ? null : ownerId;
        return articleRepository.searchArticleSummaries(term, scopeOwnerId, pageable);
    }

    private Pageable buildPageable(Integer page, Integer size, String sort) {
        int pageNumber = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Sort sortObject = parseSort(sort);
        return PageRequest.of(pageNumber, pageSize, sortObject);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "publishedAt");
        }

        var normalized = sort.trim();
        if (!normalized.contains(",")) {
            return switch (normalized.toLowerCase()) {
                case "latest" -> Sort.by(Sort.Direction.DESC, "publishedAt");
                case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt");
                case "title" -> Sort.by(Sort.Direction.ASC, "title");
                default -> Sort.by(Sort.Direction.DESC, "publishedAt");
            };
        }

        var parts = normalized.split(",");
        var property = parts[0].trim();
        if (property.isEmpty()) {
            property = "publishedAt";
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ignored) {
                direction = Sort.Direction.DESC;
            }
        }

        return Sort.by(direction, property);
    }

    private String buildTagPattern(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        var patterns = tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(String::toLowerCase)
                .map(tag -> "%\"" + tag.replace("\"", "\"\"") + "\",").collect(Collectors.joining("%"));
        if (Strings.isNotEmpty(patterns)) {
            return patterns.substring(0, patterns.length() - 1) + "%";
        } else {
            return patterns;
        }
    }

    /**
     * Aggregate categories and tags for user's visible articles within window.
     */
    public UserSubscriptionInsightResponse insights(Integer ownerId,
                                                    Instant fromTs,
                                                    Instant toTs,
                                                    Integer topN) {
        var rows = articleRepository.countCategoriesForOwnerWithin(ownerId, fromTs, toTs);
        var categories = new ArrayList<UserSubscriptionInsightResponse.CategoryCount>();
        for (var row : rows) {
            var category = (String) row[0];
            var cnt = (Long) row[1];
            categories.add(new UserSubscriptionInsightResponse.CategoryCount(category, cnt));
        }

        var tagJsonList = articleRepository.findTagJsonForOwnerWithin(ownerId, fromTs, toTs);
        var tagCounter = new HashMap<String, Long>();
        for (var raw : tagJsonList) {
            try {
                var tags = OBJECT_MAPPER.readValue(raw, TAGS_TYPE);
                if (tags != null) {
                    for (var t : tags) {
                        if (t != null) {
                            var key = t.trim().toLowerCase();
                            if (!key.isEmpty()) {
                                tagCounter.merge(key, 1L, Long::sum);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        var hotTags = tagCounter.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN == null || topN <= 0 ? 20 : topN)
                .map(e -> new UserSubscriptionInsightResponse.TagCount(e.getKey(), e.getValue()))
                .collect(java.util.stream.Collectors.toList());

        return new UserSubscriptionInsightResponse(categories.subList(0, Math.min(categories.size(), topN == null || topN <= 0 ? 20 : topN)), hotTags);
    }
}
