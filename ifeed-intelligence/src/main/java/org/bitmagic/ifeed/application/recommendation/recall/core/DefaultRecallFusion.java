package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.*;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemFreshnessProvider;
import org.bitmagic.ifeed.infrastructure.FreshnessCalculatorUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认召回融合逻辑
 *
 * 核心策略：使用 RRF（Reciprocal Rank Fusion）融合多通道结果
 * - 不依赖各通道分数可比性，只看排名位置
 * - 多通道命中的 item 获得加成
 * - 支持通道权重、新鲜度加权、多样化约束
 */
public class DefaultRecallFusion implements RecallFusion {

    private static final Duration DEFAULT_HALF_LIFE = Duration.ofHours(48);
    private static final double MIN_LAMBDA = 0.0001d;
    private final ItemFreshnessProvider freshnessProvider;
    private final double freshnessWeight;
    private final double freshnessLambda;
    private final Duration freshnessHalfLife;
    private final double defaultFreshnessScore;

    public DefaultRecallFusion() {
        this(ItemFreshnessProvider.noop(), 0.3d, DEFAULT_HALF_LIFE);
    }

    public DefaultRecallFusion(ItemFreshnessProvider freshnessProvider,
                               double freshnessWeight,
                               Duration freshnessHalfLife) {
        this(freshnessProvider, freshnessWeight, freshnessHalfLife, 0.5d );
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
        if (channelResults == null || channelResults.isEmpty()) {
            return List.of();
        }

        // 1. RRF 融合
        List<ItemCandidate> fused = fuseByRrf(channelResults, context.config());

        if (fused.isEmpty()) {
            return List.of();
        }

        // 2. 应用新鲜度加权
        List<ItemCandidate> adjusted = applyFreshness(fused, context);

        // 3. 按最终得分排序
        List<ItemCandidate> sorted = new ArrayList<>(adjusted);
        sorted.sort(Comparator.comparingDouble(ItemCandidate::score).reversed());

        // 4. 应用多样化约束
        List<ItemCandidate> diversified = context.config().hasDiversity()
                ? applyDiversity(sorted, context)
                : sorted;

        // 5. 截取最终结果
        int limit = Math.min(context.config().topK(), diversified.size());
        return diversified.subList(0, limit);
    }

    /**
     * RRF（Reciprocal Rank Fusion）融合
     */
//    private List<ItemCandidate> reciprocalRankFusion(
//            Map<StrategyId, List<ItemCandidate>> channelResults,
//            FusionContext context) {
//
//        Map<Long, Double> rrfScores = new HashMap<>();
//        Map<Long, ItemCandidate> candidateMap = new HashMap<>();
//        Map<Long, Set<StrategyId>> sourcesMap = new HashMap<>();
//        Map<Long, List<String>> reasonsMap = new HashMap<>();
//
//        for (Map.Entry<StrategyId, List<ItemCandidate>> entry : channelResults.entrySet()) {
//            StrategyId channel = entry.getKey();
//            List<ItemCandidate> items = entry.getValue();
//
//            if (items == null || items.isEmpty()) {
//                continue;
//            }
//
//            double channelWeight = context.config().weightOf(channel);
//
//            for (int rank = 0; rank < items.size(); rank++) {
//                ItemCandidate item = items.get(rank);
//                long itemId = item.itemId();
//
//                // RRF 公式
//                double positionScore = channelWeight / (rrfK + rank + 1);
//                rrfScores.merge(itemId, positionScore, Double::sum);
//
//                // 保留最高分的候选项版本
//                ItemCandidate existing = candidateMap.get(itemId);
//                if (existing == null || item.score() > existing.score()) {
//                    candidateMap.put(itemId, item);
//                }
//
//                // 记录来源通道
//                sourcesMap.computeIfAbsent(itemId, id -> new HashSet<>()).add(channel);
//
//                // 收集 reason
//                if (item.reason() != null && !item.reason().isBlank()) {
//                    reasonsMap.computeIfAbsent(itemId, id -> new ArrayList<>()).add(item.reason());
//                }
//            }
//        }
//
//        if (rrfScores.isEmpty()) {
//            return List.of();
//        }
//
//        // 按 RRF 分数排序
//        List<Map.Entry<Long, Double>> sorted = new ArrayList<>(rrfScores.entrySet());
//        sorted.sort(Map.Entry.<Long, Double>comparingByValue().reversed());
//
//        // 构建结果
//        List<ItemCandidate> result = new ArrayList<>();
//        for (Map.Entry<Long, Double> entry : sorted) {
//            long itemId = entry.getKey();
//            double rrfScore = entry.getValue();
//            ItemCandidate original = candidateMap.get(itemId);
//            Set<StrategyId> sources = sourcesMap.get(itemId);
//            List<String> reasons = reasonsMap.get(itemId);
//
//            // 构建新的 attributes
//            Map<String, Object> attrs = new HashMap<>(original.attributes());
//            if (sources != null && !sources.isEmpty()) {
//                attrs.put("_sources", sources.stream().map(Enum::name).toList());
//                attrs.put("_sourceCount", sources.size());
//            }
//            attrs.put("_originalScore", original.score());
//
//            // 合并 reason
//            String mergedReason = mergeReasons(reasons, sources);
//
//            result.add(new ItemCandidate(
//                    itemId,
//                    rrfScore,
//                    sources != null && sources.size() > 1 ? StrategyId.MIX : original.source(),
//                    mergedReason,
//                    attrs
//            ));
//        }
//
//        return result;
//    }


    /**
     * RRF（Reciprocal Rank Fusion）融合（增强版）
     */
    private List<ItemCandidate> fuseByRrf(
            Map<StrategyId, List<ItemCandidate>> channelResults,
            FusionConfig config) {

        Map<Long, Double> sumRrf = new HashMap<>();
        Map<Long, Double> maxRrf = new HashMap<>();
        Map<Long, ItemCandidate> bestCandidate = new HashMap<>();
        Map<Long, Set<StrategyId>> sources = new HashMap<>();
        Map<Long, List<String>> reasons = new HashMap<>();

        int rrfK = config.rrfK();
        int maxRank = config.maxRankPerChannel();
        double alpha = config.rrfAlpha();

        for (var entry : channelResults.entrySet()) {
            StrategyId strategy = entry.getKey();
            List<ItemCandidate> items = entry.getValue();

            if (items == null || items.isEmpty() || config.isDisabled(strategy)) {
                continue;
            }

            double weight = config.weightOf(strategy);
            int limit = Math.min(items.size(), maxRank);

            for (int rank = 0; rank < limit; rank++) {
                ItemCandidate item = items.get(rank);
                long itemId = item.itemId();

                double rrfScore = weight / (rrfK + rank + 1);

                sumRrf.merge(itemId, rrfScore, Double::sum);
                maxRrf.merge(itemId, rrfScore, Math::max);

                // 选一个“质量最好”的 candidate 版本
                ItemCandidate existing = bestCandidate.get(itemId);
                if (existing == null || item.score() > existing.score()) {
                    bestCandidate.put(itemId, item);
                }

                sources.computeIfAbsent(itemId, k -> new HashSet<>()).add(strategy);

                if (item.reason() != null && !item.reason().isBlank()) {
                    reasons.computeIfAbsent(itemId, k -> new ArrayList<>()).add(item.reason());
                }
            }
        }

        if (sumRrf.isEmpty()) {
            return List.of();
        }

        List<ItemCandidate> result = new ArrayList<>(sumRrf.size());

        for (var entry : sumRrf.entrySet()) {
            long itemId = entry.getKey();

            double sum = entry.getValue();
            double max = maxRrf.getOrDefault(itemId, 0.0);

            Set<StrategyId> src = sources.getOrDefault(itemId, Set.of());
            int sourceCount = src.size();

            double finalScore = alpha * sum + (1 - alpha) * max;

            // 多策略命中轻量奖励（不破坏排序稳定性）
            if (sourceCount > 1) {
                finalScore *= (1 + Math.log(sourceCount));
            }

            ItemCandidate base = bestCandidate.get(itemId);

            Map<String, Object> attrs = new HashMap<>(base.attributes());
            attrs.put("_sumRrf", sum);
            attrs.put("_maxRrf", max);
            attrs.put("_sourceCount", sourceCount);
            attrs.put("_sources", src.stream().map(Enum::name).toList());
            attrs.put("_originalScore", base.score());

            result.add(new ItemCandidate(
                    itemId,
                    finalScore,
                    sourceCount > 1 ? StrategyId.MIX : base.source(),
                    mergeReasons(reasons.get(itemId), src),
                    attrs
            ));
        }

        result.sort(Comparator.comparingDouble(ItemCandidate::score).reversed());

        return result.size() > config.topK()
                ? result.subList(0, config.topK())
                : result;
    }


    /**
     * 合并多来源的 reason
     */
    private String mergeReasons(List<String> reasons, Set<StrategyId> sources) {
        if (reasons == null || reasons.isEmpty()) {
            if (sources != null && sources.size() > 1) {
                return "多通道召回: " + sources.stream().map(Enum::name).collect(Collectors.joining(", "));
            }
            return null;
        }

        if (reasons.size() == 1) {
            return reasons.getFirst();
        }

        // 去重并合并
        return reasons.stream()
                .distinct()
                .collect(Collectors.joining("; "));
    }

    /**
     * 应用新鲜度加权
     */
    private List<ItemCandidate> applyFreshness(List<ItemCandidate> candidates, FusionContext context) {
        if (candidates.isEmpty() || freshnessWeight <= 0.0d) {
            return candidates;
        }

        Set<Long> ids = candidates.stream()
                .map(ItemCandidate::itemId)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return candidates;
        }

        Map<Long, Instant> publishTimes = freshnessProvider.publishedAt(ids);
        Instant referenceTime = resolveReferenceTime(context);

        // 归一化 RRF 分数
        double maxRrf = candidates.stream().mapToDouble(ItemCandidate::score).max().orElse(1.0d);
        double minRrf = candidates.stream().mapToDouble(ItemCandidate::score).min().orElse(0.0d);
        double rrfRange = maxRrf - minRrf;

        List<ItemCandidate> adjusted = new ArrayList<>(candidates.size());

        for (ItemCandidate candidate : candidates) {
            double normalizedRrf = (rrfRange > 1e-9)
                    ? (candidate.score() - minRrf) / rrfRange
                    : 1.0d;

            Instant publishedAt = publishTimes.get(candidate.itemId());
            double freshnessScore = (publishedAt == null)
                    ? defaultFreshnessScore
                    : clamp(computeFreshnessScore(publishedAt, referenceTime), 0.0d, 1.0d);

            double combined = normalizedRrf * (1.0d - freshnessWeight) + freshnessScore * freshnessWeight;
            adjusted.add(candidate.withScore(combined));
        }

        return adjusted;
    }

    /**
     * 多样化过滤
     */
    private List<ItemCandidate> applyDiversity(List<ItemCandidate> candidates, FusionContext context) {
        DiversityConfig config = context.config().diversityConfig();
        if (config == null || !config.enabled()) {
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
        return Math.max(min, Math.min(max, value));
    }
}