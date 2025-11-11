package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.DiversityConfig;
import org.bitmagic.ifeed.application.recommendation.recall.model.FusionContext;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemFreshnessProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认召回融合逻辑，支持通道权重、去重、交织重排、多样化约束以及新鲜度加权。
 */
public class DefaultRecallFusion implements RecallFusion {

    private static final Duration DEFAULT_HALF_LIFE = Duration.ofHours(48);

    private final ItemFreshnessProvider freshnessProvider;
    private final double freshnessWeight;
    private final double freshnessLambda;
    private final Duration freshnessHalfLife;

    public DefaultRecallFusion() {
        this(ItemFreshnessProvider.noop(), 0.0d, 0.5d, DEFAULT_HALF_LIFE);
    }

    public DefaultRecallFusion(ItemFreshnessProvider freshnessProvider,
                               double freshnessWeight,
                               double freshnessLambda,
                               Duration freshnessHalfLife) {
        this.freshnessProvider = freshnessProvider == null ? ItemFreshnessProvider.noop() : freshnessProvider;
        this.freshnessWeight = clamp(freshnessWeight, 0.0d, 1.0d);
        this.freshnessLambda = clamp(freshnessLambda, 0.01d, 1.0d);
        this.freshnessHalfLife = normalizeHalfLife(freshnessHalfLife);
    }

    @Override
    public List<ItemCandidate> fuse(Map<StrategyId, List<ItemCandidate>> channelResults, FusionContext context) {
        // 1. 先按通道权重调整各候选的原始得分
        List<ItemCandidate> weighted = new ArrayList<>();
        channelResults.forEach((id, items) -> {
            double weight = context.config().weightOf(id);
            for (ItemCandidate candidate : items) {
                weighted.add(candidate.withScore(candidate.score() * weight));
            }
        });

        List<ItemCandidate> merged;
        if (context.config().deduplicate()) {
            // 2. 需要去重时保留得分最高的版本
            Map<Long, ItemCandidate> deduplicated = new LinkedHashMap<>();
            for (ItemCandidate candidate : weighted) {
                deduplicated.merge(candidate.itemId(), candidate,
                        (left, right) -> left.score() >= right.score() ? left : right);
//                deduplicated.merge(candidate.itemId(), candidate,
//                        ItemCandidate::mix);
            }
            merged = new ArrayList<>(deduplicated.values());
        } else {
            merged = weighted;
        }

        // 3. 融合新鲜度后排序，便于后续重排和多样化处理
        List<ItemCandidate> adjusted = applyFreshness(merged, context);
        List<ItemCandidate> sorted = new ArrayList<>(adjusted);
        sorted.sort(Comparator.comparingDouble(ItemCandidate::score).reversed());

        List<ItemCandidate> reranked = context.config().interleaveChannels()
                ? interleaveByChannel(sorted, context.config().topK())
                : sorted;

        List<ItemCandidate> diversified = context.config().hasDiversity()
                ? applyDiversity(reranked, context)
                : reranked;

        int limit = Math.min(context.config().topK(), diversified.size());
        return diversified.subList(0, limit);
    }

    /**
     * 按策略通道交替取数，避免单一路径连续输出过多候选。
     */
    private List<ItemCandidate> interleaveByChannel(List<ItemCandidate> candidates, int limit) {
        Map<StrategyId, List<ItemCandidate>> buckets = new EnumMap<>(StrategyId.class);
        for (ItemCandidate candidate : candidates) {
            buckets.computeIfAbsent(candidate.source(), key -> new ArrayList<>()).add(candidate);
        }
        buckets.values().forEach(list -> list.sort(Comparator.comparingDouble(ItemCandidate::score).reversed()));

        List<ItemCandidate> reordered = new ArrayList<>();
        boolean added = true;
        while (added) {
            added = false;
            for (Map.Entry<StrategyId, List<ItemCandidate>> entry : buckets.entrySet()) {
                List<ItemCandidate> bucket = entry.getValue();
                if (bucket.isEmpty()) {
                    continue;
                }
                reordered.add(bucket.remove(0));
                added = true;
                if (reordered.size() >= limit) {
                    return reordered;
                }
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
            if (publishedAt == null) {
                adjusted.add(candidate);
                continue;
            }
            double freshnessScore = computeFreshnessScore(publishedAt, referenceTime);
            if (freshnessScore <= 0.0d) {
                adjusted.add(candidate);
                continue;
            }
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
        if (publishedAt == null || referenceTime == null) {
            return 0.0d;
        }
        if (!publishedAt.isBefore(referenceTime)) {
            return 1.0d;
        }

        double ageHours = (double) Duration.between(publishedAt, referenceTime).toSeconds() / 3600.0d;
        if (ageHours <= 0.0d) {
            return 1.0d;
        }

        double halfLifeHours = Math.max(1.0d, (double) freshnessHalfLife.toSeconds() / 3600.0d);
        double exponent = ageHours / halfLifeHours;
        double score = Math.pow(freshnessLambda, exponent);
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(1.0d, score));
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
