package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * 默认召回计划器，将topK在可用策略之间平均分配，并支持请求级的多样化开关。
 */
public class DefaultRecallPlanner implements RecallPlanner {

    @Override
    public RecallPlan plan(RecallRequest request, Collection<StrategyId> availableStrategies) {
        if (availableStrategies.isEmpty()) {
            return new RecallPlan(Map.of(), new FusionConfig(request.topK(), true, Map.of(), false, DiversityConfig.disabled()));
        }

        Map<StrategyId, Integer> quotas = getStrategyQuotas(availableStrategies, request.topK());

        Map<StrategyId, Double> weights = getStrategyWeight(availableStrategies);

        // 从请求的过滤器中读取交织和多样化配置
        boolean interleave = parseBoolean(request.filters().getOrDefault("interleaveChannels", Boolean.TRUE));
        DiversityConfig diversity = extractDiversityConfig(request);

        FusionConfig config = new FusionConfig(request.topK(), true, weights, interleave, diversity);
        return new RecallPlan(quotas, config);
    }

    private static @NotNull Map<StrategyId, Double> getStrategyWeight(Collection<StrategyId> availableStrategies) {
        Map<StrategyId, Double> weights = new EnumMap<>(StrategyId.class);
        availableStrategies.forEach(id -> {
            double weight = id.equals(StrategyId.U2A2I) ? 1 : (id.name().contains("U2") ? 0.7 : (id.equals(StrategyId.I2I) ? 0.5 : (id.equals(StrategyId.RANDOM_I2I) ? 0.3 : 0.1)));
            weights.put(id, weight);
        });
        return weights;
    }

    private static @NotNull Map<StrategyId, Integer> getStrategyQuotas(Collection<StrategyId> availableStrategies, int topK) {
        // 平均分配每个策略的召回配额，并对剩余的名额做一次补偿
        int recallTotal = topK * 2;
        int perStrategy = Math.max(1, recallTotal / availableStrategies.size());
        Map<StrategyId, Integer> quotas = new EnumMap<>(StrategyId.class);
        availableStrategies.forEach(id -> quotas.put(id, perStrategy));
        return quotas;
    }

    private DiversityConfig extractDiversityConfig(RecallRequest request) {
        Object keyObject = request.filters().get("diversityKey");
        if (keyObject == null) {
            return DiversityConfig.disabled();
        }
        // 将外部传入的属性、阈值等信息转换为多样化配置
        String attributeKey = String.valueOf(keyObject);
        Object limitObject = request.filters().getOrDefault("diversityLimit", 0);
        int limit = parseInt(limitObject);
        boolean fill = parseBoolean(request.filters().getOrDefault("diversityFillOverflow", Boolean.FALSE));
        return new DiversityConfig(attributeKey, limit, fill);
    }

    private boolean parseBoolean(Object flag) {
        if (flag instanceof Boolean bool) {
            return bool;
        }
        if (flag instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return flag != null;
    }

    private int parseInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
