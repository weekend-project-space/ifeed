package org.bitmagic.ifeed.service.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.api.response.ArticleRecommendationResponse;
import org.bitmagic.ifeed.config.RecommendationProperties;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleRecommendationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };
    private static final int EXTRA_FETCH_MULTIPLIER = 3;
    private static final UUID GLOBAL_CACHE_KEY = UUID.nameUUIDFromBytes("GLOBAL_RECOMMENDATION".getBytes(StandardCharsets.UTF_8));

    private final ArticleRecommendationRecallService recallService;
    private final ArticleRepository articleRepository;
    private final RecommendationProperties recommendationProperties;

    private final ConcurrentMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<CacheKey, AtomicBoolean> refreshGuards = new ConcurrentHashMap<>();

    public List<ArticleRecommendationResponse> getRecommendations(UUID userId,
                                                                  Integer requestedSize,
                                                                  RecommendationSource source) {
        var targetSize = resolveSize(requestedSize);
        var key = new CacheKey(normalizeUserKey(userId, source), targetSize, source);
        var now = Instant.now();
        var entry = cache.get(key);
        if (entry != null && !entry.isExpired(now, cacheTtl())) {
            triggerAsyncRefresh(userId, targetSize, source, key);
            return entry.items();
        }

        var items = computeRecommendations(userId, targetSize, source);
        cache.put(key, new CacheEntry(items, now));
        return items;
    }

    private void triggerAsyncRefresh(UUID userId,
                                     int targetSize,
                                     RecommendationSource source,
                                     CacheKey key) {
        refreshGuards.computeIfAbsent(key, ignored -> new AtomicBoolean(false));
        var guard = refreshGuards.get(key);
        if (!guard.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                var refreshed = computeRecommendations(userId, targetSize, source);
                cache.put(key, new CacheEntry(refreshed, Instant.now()));
            } catch (Exception ex) {
                log.warn("Failed to refresh recommendation cache for userId={} source={} size={}",
                        userId, source, targetSize, ex);
            } finally {
                guard.set(false);
            }
        });
    }

    @Transactional(readOnly = true)
    protected List<ArticleRecommendationResponse> computeRecommendations(UUID userId,
                                                                         int targetSize,
                                                                         RecommendationSource source) {
        if (source == RecommendationSource.OWNER && userId == null) {
            throw new IllegalArgumentException("User id is required for owner recommendations");
        }
        if (source == RecommendationSource.GLOBAL) {
            return buildResponses(targetSize, recallService.recallGlobal(), userId);
        }
        var candidates = recallService.recall(userId);
        return buildResponses(targetSize, candidates, userId);
    }

    private List<ArticleRecommendationResponse> buildResponses(int targetSize,
                                                               ArticleRecommendationRecallService.RecallCandidates candidates,
                                                               UUID userId) {
        if ((candidates.collaborative().isEmpty()
                && candidates.content().isEmpty()
                && candidates.popular().isEmpty())) {
            log.debug("No recall candidates available for userId={}", userId);
            return Collections.emptyList();
        }

        var aggregated = aggregateCandidates(candidates);
        if (aggregated.isEmpty()) {
            return Collections.emptyList();
        }

        normalizeAndScore(aggregated);
        var sortedEntries = aggregated.entrySet().stream()
                .sorted((left, right) -> {
                    var scoreCompare = Double.compare(right.getValue().finalScore, left.getValue().finalScore);
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return left.getKey().compareTo(right.getKey());
                })
                .collect(Collectors.toList());

        var fetchLimit = Math.min(aggregated.size(),
                Math.max(targetSize * EXTRA_FETCH_MULTIPLIER, targetSize));
        var candidateIds = sortedEntries.stream()
                .map(Map.Entry::getKey)
                .limit(fetchLimit)
                .toList();

        if (candidateIds.isEmpty()) {
            return Collections.emptyList();
        }

        var articles = articleRepository.findByIdIn(candidateIds).stream()
                .collect(Collectors.toMap(Article::getId, article -> article));

        if (articles.isEmpty()) {
            return Collections.emptyList();
        }

        var feedLimit = Math.max(1, recommendationProperties.getFeedMaxPerFeed());
        var feedCounters = new ConcurrentHashMap<UUID, Integer>();
        var responses = new ArrayList<ArticleRecommendationResponse>();

        for (var entry : sortedEntries) {
            if (responses.size() >= targetSize) {
                break;
            }
            var article = articles.get(entry.getKey());
            if (article == null) {
                continue;
            }
            var feedId = Optional.ofNullable(article.getFeed())
                    .map(feed -> feed.getId())
                    .orElse(null);
            if (feedId != null) {
                var current = feedCounters.getOrDefault(feedId, 0);
                if (current >= feedLimit) {
                    continue;
                }
                feedCounters.put(feedId, current + 1);
            }
            responses.add(toResponse(article, entry.getValue()));
        }

        return responses;
    }

    private Map<UUID, CandidateScore> aggregateCandidates(ArticleRecommendationRecallService.RecallCandidates candidates) {
        var aggregated = new ConcurrentHashMap<UUID, CandidateScore>();
        addCandidates(aggregated, candidates.collaborative(), ArticleRecommendationRecallService.RecallSource.COLLABORATIVE);
        addCandidates(aggregated, candidates.content(), ArticleRecommendationRecallService.RecallSource.CONTENT);
        addCandidates(aggregated, candidates.popular(), ArticleRecommendationRecallService.RecallSource.POPULAR);
        return aggregated;
    }

    private void addCandidates(Map<UUID, CandidateScore> aggregated,
                               List<ArticleRecommendationRecallService.ArticleRecallCandidate> candidates,
                               ArticleRecommendationRecallService.RecallSource source) {
        if (CollectionUtils.isEmpty(candidates)) {
            return;
        }
        candidates.stream()
                .filter(candidate -> candidate.score() > 0)
                .forEach(candidate -> {
                    var holder = aggregated.computeIfAbsent(candidate.articleId(), id -> new CandidateScore());
                    switch (source) {
                        case COLLABORATIVE ->
                                holder.collaborativeScore = Math.max(holder.collaborativeScore, candidate.score());
                        case CONTENT -> holder.contentScore = Math.max(holder.contentScore, candidate.score());
                        case POPULAR -> holder.popularityScore = Math.max(holder.popularityScore, candidate.score());
                    }
                });
    }

    private void normalizeAndScore(Map<UUID, CandidateScore> aggregated) {
        double maxCollaborative = aggregated.values().stream()
                .mapToDouble(value -> value.collaborativeScore)
                .max()
                .orElse(0.0d);
        double maxContent = aggregated.values().stream()
                .mapToDouble(value -> value.contentScore)
                .max()
                .orElse(0.0d);
        double maxPopularity = aggregated.values().stream()
                .mapToDouble(value -> value.popularityScore)
                .max()
                .orElse(0.0d);

        var alpha = recommendationProperties.getWeightCollaborative();
        var beta = recommendationProperties.getWeightContent();
        var gamma = recommendationProperties.getWeightPopularity();

        aggregated.values().forEach(value -> {
            value.normalizedCollaborative = maxCollaborative > 0 ? value.collaborativeScore / maxCollaborative : 0;
            value.normalizedContent = maxContent > 0 ? value.contentScore / maxContent : 0;
            value.normalizedPopularity = maxPopularity > 0 ? value.popularityScore / maxPopularity : 0;

            value.finalScore = value.normalizedCollaborative * alpha
                    + value.normalizedContent * beta
                    + value.normalizedPopularity * gamma;
            value.reason = resolveReason(value.normalizedCollaborative, value.normalizedContent, value.normalizedPopularity);
        });
    }

    private ArticleRecommendationResponse toResponse(Article article, CandidateScore score) {
        var tags = parseTags(article.getTags());
        var publishedAt = article.getPublishedAt();
        return new ArticleRecommendationResponse(
                article.getId().toString(),
                article.getTitle(),
                article.getLink(),
                article.getSummary(),
                article.getThumbnail(),
                article.getEnclosure(),
                resolveFeedTitle(article),
                publishedAt == null ? null : publishedAt.toString(),
                tags,
                formatRelativeTime(publishedAt),
                score.reason);
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(raw, TAGS_TYPE);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String resolveFeedTitle(Article article) {
        if (article.getFeed() == null || article.getFeed().getTitle() == null || article.getFeed().getTitle().isBlank()) {
            return "未知来源";
        }
        return article.getFeed().getTitle();
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

    private String resolveReason(double collaborative, double content, double popularity) {
        if (collaborative >= content && collaborative >= popularity && collaborative > 0) {
            return "collaborative";
        }
        if (content >= popularity && content > 0) {
            return "content";
        }
        if (popularity > 0) {
            return "popular";
        }
        return "popular";
    }

    private int resolveSize(Integer requestedSize) {
        var defaultLimit = recommendationProperties.getFinalCandidateLimit();
        if (requestedSize == null || requestedSize <= 0) {
            return defaultLimit;
        }
        return Math.min(requestedSize, defaultLimit);
    }

    private Duration cacheTtl() {
        var seconds = Math.max(60L, recommendationProperties.getCacheTtlSeconds());
        return Duration.ofSeconds(seconds);
    }

    public void prewarmGlobalCache() {
        getRecommendations(null, recommendationProperties.getFinalCandidateLimit(), RecommendationSource.GLOBAL);
    }

    private UUID normalizeUserKey(UUID userId, RecommendationSource source) {
        if (source == RecommendationSource.GLOBAL) {
            return GLOBAL_CACHE_KEY;
        }
        if (userId == null) {
            throw new IllegalArgumentException("User id is required for owner recommendations");
        }
        return userId;
    }

    public enum RecommendationSource {
        OWNER,
        GLOBAL;

        public static RecommendationSource from(String source) {
            if (source == null || source.isBlank()) {
                return OWNER;
            }
            var normalized = source.trim().toLowerCase();
            return switch (normalized) {
                case "global" -> GLOBAL;
                default -> OWNER;
            };
        }
    }

    record CacheKey(UUID userKey, int size, RecommendationSource source) {
    }

    record CacheEntry(List<ArticleRecommendationResponse> items, Instant createdAt) {
        boolean isExpired(Instant now, Duration ttl) {
            return createdAt.plus(ttl).isBefore(now);
        }
    }

    private static final class CandidateScore {
        private double collaborativeScore;
        private double contentScore;
        private double popularityScore;
        private double normalizedCollaborative;
        private double normalizedContent;
        private double normalizedPopularity;
        private double finalScore;
        private String reason = "popular";

        private CandidateScore() {
        }
    }
}
