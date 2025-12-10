package org.bitmagic.ifeed.infrastructure.recall;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.record.ArticleSummary;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.infrastructure.recall.data.UserBehaviorDataAccessor;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    private final UserBehaviorDataAccessor dataAccessor;
    private final ArticleRepository articleRepository;
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
    public List<AttributePreference> topAttributes(Integer userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }

        return dataAccessor.getUserBehavior(userId)
                .map(doc -> computeTopPreferences(doc, limit))
                .orElseGet(() -> {
                    log.debug("No user behavior found for userId: {}", userId);
                    return List.of();
                });
    }

    private List<AttributePreference> computeTopPreferences(
            UserBehaviorDocument document, int limit) {

        // 使用共享的过滤和排序逻辑
        List<UserBehaviorDocument.ArticleRef> readHistory = dataAccessor.filterAndSortRefs(
                document.getReadHistory(),
                readWindowDays,
                lookback);

        List<UserBehaviorDocument.ArticleRef> collections = dataAccessor.filterAndSortRefs(
                document.getCollections(),
                collectionWindowDays,
                lookback);

        if (readHistory.isEmpty() && collections.isEmpty()) {
            log.debug("No valid read history or collections for user {}", document.getId());
            return List.of();
        }

        // 收集所有文章 UUID
        Set<String> allArticleIds = new HashSet<>();
        allArticleIds.addAll(dataAccessor.extractArticleIds(readHistory));
        allArticleIds.addAll(dataAccessor.extractArticleIds(collections));

        if (allArticleIds.isEmpty()) {
            log.debug("No valid article IDs found for user {}", document.getId());
            return List.of();
        }

        // 转换为 UUID 列表
        List<UUID> uuids = allArticleIds.stream()
                .map(dataAccessor::safeUuid)
                .filter(Objects::nonNull)
                .toList();

        if (uuids.isEmpty()) {
            log.warn("No valid UUIDs parsed for user {}", document.getId());
            return List.of();
        }

        // 直接用 UUID 查询文章详情并建立映射
        Map<UUID, ArticleSummary> articleMap = articleRepository
                .listArticleSummaries(uuids).stream()
                .collect(Collectors.toMap(ArticleSummary::id, a -> a));

        if (articleMap.isEmpty()) {
            log.warn("No articles found in repository for user {} with {} UUIDs",
                    document.getId(), uuids.size());
            return List.of();
        }

        // 收藏的文章ID集合
        Set<String> collectedIds = dataAccessor.extractArticleIds(collections);

        // 统计各维度权重
        Map<String, Double> feedScores = new HashMap<>();
        Map<String, Double> tagScores = new HashMap<>();
        Map<String, Double> categoryScores = new HashMap<>();
        Map<String, Double> keywordScores = new HashMap<>();
        Map<String, Double> entityScores = new HashMap<>();
        Set<String> processedIds = new HashSet<>();

        // 处理阅读历史
        processArticleRefs(readHistory, articleMap, collectedIds,
                feedScores, tagScores, categoryScores, keywordScores, entityScores, processedIds);

        // 处理仅收藏但未阅读的文章
        processCollectionOnlyRefs(collections, articleMap, processedIds,
                feedScores, tagScores, categoryScores, keywordScores, entityScores);

        if (feedScores.isEmpty() && tagScores.isEmpty() && categoryScores.isEmpty()
                && keywordScores.isEmpty() && entityScores.isEmpty()) {
            log.debug("No valid scores computed for user {}", document.getId());
            return List.of();
        }

        // 归一化并合并排序
        List<AttributePreference> result = new ArrayList<>();
        result.addAll(normalizeAndConvert(feedScores, "feedTitle"));
        result.addAll(normalizeAndConvert(tagScores, "tag"));
        result.addAll(normalizeAndConvert(categoryScores, "category"));
        result.addAll(normalizeAndConvert(keywordScores, "keyword"));
        result.addAll(normalizeAndConvert(entityScores, "entity"));

        log.info("Computed {} attribute preferences for user {} (read:{}, collection:{})",
                result.size(), document.getId(), readHistory.size(), collections.size());

        return result.stream()
                .sorted(Comparator.comparingDouble(AttributePreference::weight).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 处理阅读历史中的文章
     */
    private void processArticleRefs(
            List<UserBehaviorDocument.ArticleRef> refs,
            Map<UUID, ArticleSummary> articleMap,
            Set<String> collectedIds,
            Map<String, Double> feedScores,
            Map<String, Double> tagScores,
            Map<String, Double> categoryScores,
            Map<String, Double> keywordScores,
            Map<String, Double> entityScores,
            Set<String> processedIds) {

        for (UserBehaviorDocument.ArticleRef ref : refs) {
            String articleId = ref.getArticleId();
            if (articleId == null)
                continue;

            UUID uuid = dataAccessor.safeUuid(articleId);
            if (uuid == null)
                continue;

            ArticleSummary article = articleMap.get(uuid);
            if (article == null)
                continue;

            // 基础分数 1.0，如果被收藏则额外加分
            double score = collectedIds.contains(articleId)
                    ? 1.0 + collectionBonus
                    : 1.0;

            accumulateFeedScore(feedScores, article, score);
            accumulateTagScores(tagScores, article, score);
            accumulateCategoryScore(categoryScores, article, score);
            accumulateKeywordScores(keywordScores, article, score);
            accumulateEntityScores(entityScores, article, score);

            processedIds.add(articleId);
        }
    }

    /**
     * 处理仅收藏但未阅读的文章
     */
    private void processCollectionOnlyRefs(
            List<UserBehaviorDocument.ArticleRef> collections,
            Map<UUID, ArticleSummary> articleMap,
            Set<String> processedIds,
            Map<String, Double> feedScores,
            Map<String, Double> tagScores,
            Map<String, Double> categoryScores,
            Map<String, Double> keywordScores,
            Map<String, Double> entityScores) {

        for (UserBehaviorDocument.ArticleRef ref : collections) {
            String articleId = ref.getArticleId();

            // 已在阅读历史中处理过，跳过
            if (articleId == null || processedIds.contains(articleId)) {
                continue;
            }

            UUID uuid = dataAccessor.safeUuid(articleId);
            if (uuid == null)
                continue;

            ArticleSummary article = articleMap.get(uuid);
            if (article == null)
                continue;

            // 收藏但未阅读，给予收藏加成分数
            double score = 1.0 + collectionBonus;

            accumulateFeedScore(feedScores, article, score);
            accumulateTagScores(tagScores, article, score);
            accumulateCategoryScore(categoryScores, article, score);
        }
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
                    article.title(), article.summary());

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