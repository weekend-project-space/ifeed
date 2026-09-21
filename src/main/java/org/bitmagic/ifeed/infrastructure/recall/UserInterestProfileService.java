package org.bitmagic.ifeed.infrastructure.recall;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.domain.record.ArticleSummary;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.bitmagic.ifeed.domain.record.ReadHistoryArticle;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户兴趣画像服务
 * 基于阅读历史和收藏行为，提取用户对 feedTitle、tag、category、keyword、entity 的偏好权重
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserInterestProfileService implements UserPreferenceService {

    private final UserCollectionRepository collectionRepository;
    private final UserReadHistoryRepository historyRepository;
    private final KeywordExtractor keywordExtractor;

    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };

    @Value("${recall.preference.read-window-days:30}")
    private int readWindowDays;

    @Value("${recall.preference.collection-window-days:90}")
    private int collectionWindowDays;

    @Value("${recall.preference.lookback:200}")
    private int lookback;

    @Value("${recall.preference.collection-bonus:2.0}")
    private double collectionBonus;

    @Value("${recall.preference.min-total-score:0.1}")
    private double minTotalScore;

    @Value("${recall.preference.extract-keywords:true}")
    private boolean extractKeywords;

    @Value("${recall.preference.extract-entities:true}")
    private boolean extractEntities;

    @Override
    public List<AttributePreference> topAttributes(Integer userId, int lookback, int limit) {
        if (userId == null || userId <= 0 || limit <= 0) {
            return List.of();
        }

        int historyLimit = lookback < 1 ? this.lookback : lookback;
        Instant cutoff = readWindowDays > 0 ? Instant.now().minus(Duration.ofDays(readWindowDays)) : null;
        var readHistory = historyRepository.findRecent(userId.longValue(), cutoff, historyLimit).stream()
                .map(ReadHistoryArticle::article)
                .toList();
        Instant collectionCutoff = collectionWindowDays > 0
                ? Instant.now().minus(Duration.ofDays(collectionWindowDays)) : null;
        var collectedArticles = historyLimit < 10 ? List.<ArticleSummary>of()
                : collectionRepository.findRecent(userId.longValue(), collectionCutoff, historyLimit);
        return computeTopPreferences(readHistory, collectedArticles, limit);
    }

    private List<AttributePreference> computeTopPreferences(
            List<ArticleSummary> readHistory, List<ArticleSummary> collections, int limit) {
        Set<Long> collectedIds = collections.stream()
                .map(ArticleSummary::articleId)
                .collect(Collectors.toSet());
        Map<String, Double> feedScores = new HashMap<>();
        Map<String, Double> tagScores = new HashMap<>();
        Map<String, Double> categoryScores = new HashMap<>();
        Map<String, Double> keywordScores = new HashMap<>();
        Map<String, Double> entityScores = new HashMap<>();
        Set<Long> processedIds = new HashSet<>();

        for (ArticleSummary article : readHistory) {
            double score = collectedIds.contains(article.articleId()) ? 1.0 + collectionBonus : 1.0;
            accumulateFeedScore(feedScores, article, score);
            accumulateTagScores(tagScores, article, score);
            accumulateCategoryScore(categoryScores, article, score);
            accumulateKeywordScores(keywordScores, article, score);
            accumulateEntityScores(entityScores, article, score);
            processedIds.add(article.articleId());
        }

        for (ArticleSummary article : collections) {
            if (processedIds.contains(article.articleId())) {
                continue;
            }
            double score = 1.0 + collectionBonus;
            accumulateFeedScore(feedScores, article, score);
            accumulateTagScores(tagScores, article, score);
            accumulateCategoryScore(categoryScores, article, score);
        }

        List<AttributePreference> result = new ArrayList<>();
        result.addAll(normalizeAndConvert(feedScores, "feedTitle"));
        result.addAll(normalizeAndConvert(tagScores, "tag"));
        result.addAll(normalizeAndConvert(categoryScores, "category"));
        result.addAll(normalizeAndConvert(keywordScores, "keyword"));
        result.addAll(normalizeAndConvert(entityScores, "entity"));
        return result.stream()
                .sorted(Comparator.comparingDouble(AttributePreference::weight).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 累加 Feed 分数
     */
    private void accumulateFeedScore(
            Map<String, Double> scores,
            ArticleSummary article,
            double score) {

        if (article.feedTitle() != null && !article.feedTitle().isBlank()) {
            scores.merge(article.feedTitle(), score, Double::sum);
        }
    }

    private static final Set<String> STOP_WORDS = Set.of("HTTPS", "HTTP", "COM");

    /**
     * 累加 Tag 分数
     */
    private void accumulateTagScores(
            Map<String, Double> scores,
            ArticleSummary article,
            double score) {

        String tags = article.tags();
        if (tags == null || tags.isBlank()) {
            return;
        }

        try {
            List<String> tagList = JSON.fromJson(tags, TAGS_TYPE);
            if (tagList != null) {
                tagList.stream()
                        .filter(tag -> tag != null && !tag.isEmpty())
                        .filter(tag -> !STOP_WORDS.contains(tag.toUpperCase()))
                        .forEach(tag -> scores.merge(tag, score, Double::sum));
            }
        } catch (Exception e) {
            log.debug("Failed to parse tags for article {}: {}",
                    article.id(), e.getMessage());
        }
    }

    /**
     * 累加 Category 分数
     */
    private void accumulateCategoryScore(
            Map<String, Double> scores,
            ArticleSummary article,
            double score) {

        if (article.category() != null && !article.category().isBlank()) {
            scores.merge(article.category(), score, Double::sum);
        }
    }

    /**
     * 累加 Keyword 分数
     */
    private void accumulateKeywordScores(
            Map<String, Double> scores,
            ArticleSummary article,
            double score) {

        if (!extractKeywords) {
            return;
        }

        try {
            List<String> keywords = keywordExtractor.extractKeywords(
                    article.title(), article.summary());

            for (String keyword : keywords) {
                if (keyword != null && !keyword.isBlank()) {
                    scores.merge(keyword, score, Double::sum);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract keywords for article {}: {}",
                    article.id(), e.getMessage());
        }
    }

    /**
     * 累加 Entity 分数
     */
    private void accumulateEntityScores(
            Map<String, Double> scores,
            ArticleSummary article,
            double score) {

        if (!extractEntities) {
            return;
        }

        try {
            List<String> entities = keywordExtractor.extractEntities(
                    article.title(), article.summary()).stream().filter(tag -> !STOP_WORDS.contains(tag.toUpperCase())).toList();

            for (String entity : entities) {
                if (entity != null && !entity.isBlank()) {
                    scores.merge(entity, score, Double::sum);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract entities for article {}: {}",
                    article.id(), e.getMessage());
        }
    }

    /**
     * 归一化：将原始分数转换为 0-1 的比例权重
     * 如果总分太低，保留原始分数以避免信息丢失
     */
    private List<AttributePreference> normalizeAndConvert(
            Map<String, Double> scores,
            String attrKey) {

        if (scores.isEmpty()) {
            return List.of();
        }

        double total = scores.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (total <= 0) {
            return List.of();
        }

        boolean shouldNormalize = total >= minTotalScore;

        return scores.entrySet().stream()
                .map(e -> {
                    double weight = shouldNormalize ? e.getValue() / total : e.getValue();
                    return new AttributePreference(attrKey, e.getKey(), weight);
                })
                .toList();
    }
}
