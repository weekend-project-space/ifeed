package org.bitmagic.ifeed.infrastructure.recall;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户兴趣画像服务
 * 基于阅读历史和收藏行为，提取用户对 feedId、category、tag 的偏好权重
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserInterestProfileService implements UserPreferenceService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleRepository articleRepository;

    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };

    @Value("${recall.preference.window-days:30}")
    private int windowDays;

    @Value("${recall.preference.lookback:200}")
    private int lookback;

    @Value("${recall.preference.collection-bonus:2.0}")
    private double collectionBonus;

    @Override
    public List<AttributePreference> topAttributes(Integer userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }

        return userBehaviorRepository.findById(userId.toString())
                .map(doc -> computeTopPreferences(doc, limit))
                .orElse(List.of());
    }

    private List<AttributePreference> computeTopPreferences(UserBehaviorDocument document, int limit) {
        Instant cutoff = resolveCutoff();
        int fetchLimit = Math.max(Math.max(limit, lookback), limit * 5);

        // 获取阅读历史
        List<UserBehaviorDocument.ArticleRef> readHistory = filterAndSort(
                document.getReadHistory(), cutoff, fetchLimit);

        // 获取收藏历史
        List<UserBehaviorDocument.ArticleRef> collections = filterAndSort(
                document.getCollections(), cutoff, fetchLimit);

        if (readHistory.isEmpty() && collections.isEmpty()) {
            return List.of();
        }

        // 收集所有文章ID
        Set<UUID> allIds = new HashSet<>();
        readHistory.stream()
                .map(ref -> safeUuid(ref.getArticleId()))
                .filter(Objects::nonNull)
                .forEach(allIds::add);
        collections.stream()
                .map(ref -> safeUuid(ref.getArticleId()))
                .filter(Objects::nonNull)
                .forEach(allIds::add);

        if (allIds.isEmpty()) {
            return List.of();
        }

        // 收藏的文章ID集合，用于加权
        Set<UUID> collectedIds = collections.stream()
                .map(ref -> safeUuid(ref.getArticleId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 查询文章（带 fetch join）
        Map<UUID, ArticleSummaryView> articleMap = articleRepository
                .listArticleSummaries(new ArrayList<>(allIds)).stream()
                .collect(Collectors.toMap(ArticleSummaryView::id, a -> a));

        // 统计各维度权重
        Map<String, Double> feedScores = new HashMap<>();
        Map<String, Double> tagScores = new HashMap<>();

        // 处理阅读历史
        for (UserBehaviorDocument.ArticleRef ref : readHistory) {
            UUID uuid = safeUuid(ref.getArticleId());
            if (uuid == null) continue;

            ArticleSummaryView article = articleMap.get(uuid);
            if (article == null) continue;

            // 基础分数 1.0，收藏额外加分
            double score = collectedIds.contains(uuid) ? 1.0 + collectionBonus : 1.0;

            accumulateFeedScore(feedScores, article, score);
            accumulateTagScores(tagScores, article, score);
        }

        // 处理仅收藏但未在阅读历史中的文章
        for (UserBehaviorDocument.ArticleRef ref : collections) {
            UUID uuid = safeUuid(ref.getArticleId());
            if (uuid == null) continue;

            // 已在阅读历史中处理过，跳过
            boolean inReadHistory = readHistory.stream()
                    .anyMatch(r -> uuid.toString().equals(r.getArticleId()));
            if (inReadHistory) continue;

            ArticleSummaryView article = articleMap.get(uuid);
            if (article == null) continue;

            // 收藏但未阅读，给予收藏加成分数
            double score = 1.0 + collectionBonus;

            accumulateFeedScore(feedScores, article, score);
            accumulateTagScores(tagScores, article, score);
        }

        if (feedScores.isEmpty() && tagScores.isEmpty()) {
            return List.of();
        }

        // 归一化并合并排序
        List<AttributePreference> result = new ArrayList<>();
        result.addAll(normalizeAndConvert(feedScores, "feedTitle"));
        result.addAll(normalizeAndConvert(tagScores, "tag"));

        return result.stream()
                .sorted(Comparator.comparingDouble(AttributePreference::weight).reversed())
                .limit(limit)
                .toList();
    }

    private List<UserBehaviorDocument.ArticleRef> filterAndSort(
            List<UserBehaviorDocument.ArticleRef> refs, Instant cutoff, int limit) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }

        return refs.stream()
                .filter(ref -> ref.getArticleId() != null)
                .filter(ref -> cutoff == null || ref.getTimestamp() == null
                        || !ref.getTimestamp().isBefore(cutoff))
                .sorted(Comparator.comparing(
                        UserBehaviorDocument.ArticleRef::getTimestamp,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .toList();
    }

    private void accumulateFeedScore(Map<String, Double> scores, ArticleSummaryView article, double score) {
        if (article.feedTitle() != null) {
            scores.merge(article.feedTitle(), score, Double::sum);
        }
    }

    private void accumulateTagScores(Map<String, Double> scores, ArticleSummaryView article, double score) {
        String tags = article.tags();
        if (tags == null || tags.isBlank()) {
            return;
        }
        JSON.fromJson(tags, TAGS_TYPE).stream()
                .filter(tag -> !tag.isEmpty())
                .forEach(tag -> scores.merge(tag, score, Double::sum));
    }

    /**
     * 归一化：将原始分数转换为 0-1 的比例权重
     */
    private List<AttributePreference> normalizeAndConvert(Map<String, Double> scores, String attrKey) {
        if (scores.isEmpty()) {
            return List.of();
        }

        double total = scores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            return List.of();
        }

        return scores.entrySet().stream()
                .map(e -> new AttributePreference(attrKey, e.getKey(), e.getValue() / total))
                .toList();
    }

    private Instant resolveCutoff() {
        if (windowDays <= 0) {
            return null;
        }
        return Instant.now().minus(Duration.ofDays(windowDays));
    }

    private UUID safeUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            log.debug("Ignore invalid articleId {} in read history", value);
            return null;
        }
    }
}