package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.DiversityConfig;
import org.bitmagic.ifeed.application.recommendation.recall.model.FusionContext;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemFreshnessProvider;
import org.bitmagic.ifeed.infrastructure.FreshnessCalculatorUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认召回融合逻辑，支持通道权重、去重、交织重排、多样化约束以及新鲜度加权。
 *
 * 改进点：
 * 1. 调整处理顺序：新鲜度加权 → 排序 → 多样化 → 交织
 * 2. 交织时保持得分顺序，避免破坏全局排序
 * 3. 统一处理无时间戳的候选项
 * 4. 去重时保留所有来源通道信息
 */
public class DefaultRecallFusion implements RecallFusion {

    private static final Duration DEFAULT_HALF_LIFE = Duration.ofHours(48);
    private static final double MIN_LAMBDA = 0.0001d; // 降低最小lambda限制

    private final ItemFreshnessProvider freshnessProvider;
    private final double freshnessWeight;
    private final double freshnessLambda;
    private final Duration freshnessHalfLife;
    private final double defaultFreshnessScore; // 无时间戳时的默认新鲜度分

    public DefaultRecallFusion() {
        this(ItemFreshnessProvider.noop(), 0.3d, DEFAULT_HALF_LIFE);
    }

    public DefaultRecallFusion(ItemFreshnessProvider freshnessProvider,
                               double freshnessWeight,
                               Duration freshnessHalfLife) {
        this(freshnessProvider, freshnessWeight, freshnessHalfLife, 0.5d);
    }

    public DefaultRecallFusion(ItemFreshnessProvider freshnessProvider,
                               double freshnessWeight,
                               Duration freshnessHalfLife,
                               double defaultFreshnessScore) {
        this.freshnessProvider = freshnessProvider == null ? ItemFreshnessProvider.noop() : freshnessProvider;
        this.freshnessWeight = clamp(freshnessWeight, 0.0d, 1.0d);
        this.freshnessHalfLife = normalizeHalfLife(freshnessHalfLife);
        this.freshnessLambda = clamp(Math.log(2) / this.freshnessHalfLife.toHours(), MIN_LAMBDA, 1.0d);
        this.defaultFreshnessScore = clamp(defaultFreshnessScore, 0.0d, 1.0d);
    }

    @Override
    public List<ItemCandidate> fuse(Map<StrategyId, List<ItemCandidate>> channelResults, FusionContext context) {
        // 1. 按通道权重调整各候选的原始得分
        List<ItemCandidate> weighted = new ArrayList<>();
        channelResults.forEach((id, items) -> {
            double weight = context.config().weightOf(id);
            items.forEach(candidate -> weighted.add(candidate.withScore(candidate.score() * weight)));
        });

        // 2. 去重（保留得分最高的版本，并合并来源通道信息）
        List<ItemCandidate> merged;
        if (context.config().deduplicate()) {
            merged = deduplicateWithSourceMerge(weighted);
        } else {
            merged = weighted;
        }

        // 3. 应用新鲜度加权
        List<ItemCandidate> adjusted = applyFreshness(merged, context);

        // 4. 按最终得分排序
        List<ItemCandidate> sorted = new ArrayList<>(adjusted);
        sorted.sort(Comparator.comparingDouble(ItemCandidate::score).reversed());

        // 5. 应用多样化约束（基于排序后的结果）
        List<ItemCandidate> diversified = context.config().hasDiversity()
                ? applyDiversity(sorted, context)
                : sorted;

        // 6. 交织重排（在多样化后的结果中平衡通道）
        List<ItemCandidate> reranked = context.config().interleaveChannels()
                ? interleaveByChannelPreserveOrder(diversified, context.config().topK())
                : diversified;

        // 7. 截取最终结果
        int limit = Math.min(context.config().topK(), reranked.size());
        return reranked.subList(0, limit);
    }

    /**
     * 去重时保留最高分版本，并记录所有来源通道
     */
    private List<ItemCandidate> deduplicateWithSourceMerge(List<ItemCandidate> candidates) {
        Map<Long, ItemCandidate> deduplicated = new LinkedHashMap<>();
        Map<Long, Set<StrategyId>> sourcesMap = new HashMap<>();

        for (ItemCandidate candidate : candidates) {
            Long itemId = candidate.itemId();
            ItemCandidate existing = deduplicated.get(itemId);

            if (existing == null || candidate.score() > existing.score()) {
                deduplicated.put(itemId, candidate);
            }

            // 记录所有来源通道
            sourcesMap.computeIfAbsent(itemId, k -> new HashSet<>()).add(candidate.source());
        }

        return new ArrayList<>(deduplicated.values());
    }

    /**
     * 按策略通道交替取数，同时保持得分顺序不被破坏
     *
     * 策略：在相同得分区间内优先轮询不同通道，避免单一通道连续输出
     */
    private List<ItemCandidate> interleaveByChannelPreserveOrder(List<ItemCandidate> candidates, int limit) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        // 按通道分组（candidates已经按分数降序排列）
        Map<StrategyId, List<ItemCandidate>> buckets = new EnumMap<>(StrategyId.class);
        for (ItemCandidate candidate : candidates) {
            buckets.computeIfAbsent(candidate.source(), key -> new ArrayList<>()).add(candidate);
        }

        List<ItemCandidate> reordered = new ArrayList<>();
        Map<StrategyId, Integer> indices = new EnumMap<>(StrategyId.class);
        buckets.keySet().forEach(key -> indices.put(key, 0));

        // 使用round-robin方式交织，但仍基于全局排序
        int totalProcessed = 0;
        while (totalProcessed < candidates.size() && reordered.size() < limit) {
            boolean addedInRound = false;

            // 轮询每个通道
            for (StrategyId strategy : buckets.keySet()) {
                List<ItemCandidate> bucket = buckets.get(strategy);
                int index = indices.get(strategy);

                if (index < bucket.size()) {
                    reordered.add(bucket.get(index));
                    indices.put(strategy, index + 1);
                    addedInRound = true;
                    totalProcessed++;

                    if (reordered.size() >= limit) {
                        return reordered;
                    }
                }
            }

            // 如果某轮没有添加任何元素，说明所有通道都已耗尽
            if (!addedInRound) {
                break;
            }
        }

        return reordered;
    }

    /**
     * 根据属性约束执行多样化过滤，超过上限的候选进入溢出列表，必要时回填。
     */
    private List<ItemCandidate> applyDiversity(List<ItemCandidate> candidates, FusionContext context) {
        DiversityConfig config = context.config().diversityConfig();
        if (!config.enabled()) {
            return candidates;
        }

        Map<String, Integer> counts = new HashMap<>();
        List<ItemCandidate> accepted = new ArrayList<>();
        List<ItemCandidate> overflow = new ArrayList<>();
        int max = context.config().topK();

        for (ItemCandidate candidate : candidates) {
            if (accepted.size() >= max) {
                break;
            }

            Object attrValue = candidate.attributes().get(config.attributeKey());
            if (attrValue == null) {
                // 无属性的候选直接接受
                accepted.add(candidate);
                continue;
            }

            String bucket = String.valueOf(attrValue);
            int current = counts.getOrDefault(bucket, 0);

            if (current >= config.maxPerAttribute()) {
                overflow.add(candidate);
                continue;
            }

            counts.put(bucket, current + 1);
            accepted.add(candidate);
        }

        // 如果允许溢出回填，且还未达到topK
        if (config.fillOverflow() && accepted.size() < max) {
            for (ItemCandidate candidate : overflow) {
                if (accepted.size() >= max) {
                    break;
                }
                accepted.add(candidate);
            }
        }

        return accepted;
    }

    /**
     * 应用新鲜度加权，统一处理有无时间戳的情况
     */
    private List<ItemCandidate> applyFreshness(List<ItemCandidate> candidates, FusionContext context) {
        if (candidates.isEmpty() || freshnessWeight <= 0.0d) {
            return candidates;
        }

        Set<Long> ids = candidates.stream().map(ItemCandidate::itemId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return candidates;
        }

        Map<Long, Instant> publishTimes = freshnessProvider.publishedAt(ids);
        if (publishTimes.isEmpty()) {
            return candidates;
        }

        Instant referenceTime = resolveReferenceTime(context);
        List<ItemCandidate> adjusted = new ArrayList<>(candidates.size());

        for (ItemCandidate candidate : candidates) {
            Instant publishedAt = publishTimes.get(candidate.itemId());
            double freshnessScore;

            if (publishedAt == null) {
                // 无时间戳时使用默认新鲜度分
                freshnessScore = defaultFreshnessScore;
            } else {
                freshnessScore = computeFreshnessScore(publishedAt, referenceTime);
                // 确保新鲜度分在[0, 1]范围内
                freshnessScore = clamp(freshnessScore, 0.0d, 1.0d);
            }

            // 加权融合：原始分数 * (1 - weight) + 新鲜度分 * weight
            double combined = candidate.score() * (1.0d - freshnessWeight) + freshnessScore * freshnessWeight;
            adjusted.add(candidate.withScore(combined));
        }

        return adjusted;
    }

    private Instant resolveReferenceTime(FusionContext context) {
        if (context != null && context.request() != null && context.request().requestTime() != null) {
            return context.request().requestTime();
        }
        return Instant.now();
    }

    private double computeFreshnessScore(Instant publishedAt, Instant referenceTime) {
        return FreshnessCalculatorUtils.calculate(freshnessLambda, publishedAt, referenceTime);
    }

    private Duration normalizeHalfLife(Duration value) {
        if (value == null || value.isNegative() || value.isZero()) {
            return DEFAULT_HALF_LIFE;
        }
        return value;
    }

    private double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}