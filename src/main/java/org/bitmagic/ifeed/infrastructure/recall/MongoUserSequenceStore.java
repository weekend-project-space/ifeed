package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.infrastructure.recall.data.UserBehaviorDataAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 Mongo 用户行为数据的序列存储
 * 从阅读历史提取最近的文章交互序列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoUserSequenceStore implements SequenceStore {

    private final UserBehaviorDataAccessor dataAccessor;

    @Value("${recall.sequence.window-days:30}")
    private int windowDays;

    @Value("${recall.sequence.lookback:200}")
    private int lookback;

    @Value("${recall.sequence.fetch-multiplier:2}")
    private int fetchMultiplier;

    @Value("${recall.sequence.max-fetch-limit:1000}")
    private int maxFetchLimit;

    @Value("${recall.sequence.recency-decay-factor:1.0}")
    private double recencyDecayFactor;

    @Value("${recall.sequence.interaction-weight-base:0.1}")
    private double interactionWeightBase;

    @Value("${recall.sequence.estimated-interaction-duration:30.0}")
    private double estimatedInteractionDuration;

    @Value("${recall.sequence.use-logarithmic-interaction:true}")
    private boolean useLogarithmicInteraction;

    @Override
    public List<UserInteraction> recentInteractions(Integer userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }

        return dataAccessor.getUserBehavior(userId)
                .map(doc -> extractRecentInteractions(doc, limit))
                .orElseGet(() -> {
                    log.debug("No user behavior found for userId: {}", userId);
                    return List.of();
                });
    }

    private List<UserInteraction> extractRecentInteractions(
            UserBehaviorDocument document, int limit) {

        if (document.getReadHistory() == null || document.getReadHistory().isEmpty()) {
            log.debug("No read history found for user {}", document.getId());
            return List.of();
        }

        // 使用共享的过滤和排序逻辑
        int fetchLimit = Math.min(
                Math.max(limit * fetchMultiplier, lookback),
                maxFetchLimit);

        List<UserBehaviorDocument.ArticleRef> sorted =
                dataAccessor.filterAndSortRefs(
                        document.getReadHistory(),
                        windowDays,
                        fetchLimit);

        if (sorted.isEmpty()) {
            log.debug("No valid timestamped articles in read history for user {}",
                    document.getId());
            return List.of();
        }

        // 提取文章ID列表（String UUID）
        List<String> articleIds = sorted.stream()
                .map(UserBehaviorDocument.ArticleRef::getArticleId)
                .toList();

        // 【关键步骤】批量映射 UUID → Long (数据库主键)
        // 这一步是必需的，因为 UserInteraction 需要 Long articleId
        Map<UUID, Long> idMapping = dataAccessor.batchMapArticleIds(articleIds);

        if (idMapping.isEmpty()) {
            log.warn("No article ID mappings found for user {} with {} UUIDs",
                    document.getId(), articleIds.size());
            return List.of();
        }

        // 获取互动次数映射
        Map<String, Long> interactionCounts =
                dataAccessor.buildInteractionCountMap(document);

        // 构建结果列表
        List<UserInteraction> results = buildUserInteractions(
                sorted, idMapping, interactionCounts, limit);

        log.info("Extracted {} interactions from {} read history items for user {}",
                results.size(), sorted.size(), document.getId());

        return results;
    }

    /**
     * 构建用户交互列表
     *
     * @param refs 已排序的文章引用列表
     * @param idMapping UUID到数据库ID的映射（必需）
     * @param interactionCounts 互动次数映射
     * @param limit 返回数量限制
     * @return 用户交互列表
     */
    private List<UserInteraction> buildUserInteractions(
            List<UserBehaviorDocument.ArticleRef> refs,
            Map<UUID, Long> idMapping,
            Map<String, Long> interactionCounts,
            int limit) {

        List<UserInteraction> results = new ArrayList<>();
        int index = 0;

        for (UserBehaviorDocument.ArticleRef ref : refs) {
            String articleId = ref.getArticleId();

            // 转换为 UUID
            UUID uuid = dataAccessor.safeUuid(articleId);
            if (uuid == null) {
                continue;
            }

            // 【关键】通过 idMapping 获取数据库主键
            Long dbArticleId = idMapping.get(uuid);
            if (dbArticleId == null) {
                // UUID 在数据库中不存在（可能已删除）
                continue;
            }

            // 计算时效性权重：使用指数衰减
            // 第0条记录权重最高 (exp(0) = 1.0)
            // 第limit条记录权重约为 exp(-1) ≈ 0.368
            double recencyWeight = Math.exp(-index / (recencyDecayFactor * limit));

            // 获取互动次数并计算互动权重
            long interactionCount = interactionCounts.getOrDefault(articleId, 0L);
            double interactionWeight = calculateInteractionWeight(interactionCount);

            // 综合权重：时效性 * (1 + 互动权重)
            // 示例：recency=0.9, interaction=0.2 → weight=0.9*(1+0.2)=1.08
            double weight = recencyWeight * (1.0 + interactionWeight);

            // 估算互动总时长（秒）
            double duration = interactionCount * estimatedInteractionDuration;

            // 时间戳（已在 filterAndSortRefs 中保证非空）
            Instant timestamp = ref.getTimestamp();

            // 创建 UserInteraction（需要 Long articleId）
            results.add(new UserInteraction(dbArticleId, duration, weight, timestamp));
            index++;

            if (results.size() >= limit) {
                break;
            }
        }

        return results;
    }

    /**
     * 计算互动权重
     *
     * 支持两种策略：
     * 1. 对数衰减：适合互动次数差异大的场景，防止极端值主导
     *    - 1次互动 → weight ≈ 0.069 (log(2) * 0.1)
     *    - 10次互动 → weight ≈ 0.240 (log(11) * 0.1)
     *    - 100次互动 → weight ≈ 0.461 (log(101) * 0.1)
     *
     * 2. 线性权重：适合互动次数分布均匀的场景
     *    - 1次互动 → weight = 0.1
     *    - 10次互动 → weight = 1.0
     *    - 100次互动 → weight = 10.0
     *
     * @param interactionCount 互动次数
     * @return 互动权重
     */
    private double calculateInteractionWeight(long interactionCount) {
        if (interactionCount <= 0) {
            return 0.0;
        }

        if (useLogarithmicInteraction) {
            // 对数衰减：log(1 + count) * base
            return Math.log1p(interactionCount) * interactionWeightBase;
        } else {
            // 线性权重：count * base
            return interactionCount * interactionWeightBase;
        }
    }
}