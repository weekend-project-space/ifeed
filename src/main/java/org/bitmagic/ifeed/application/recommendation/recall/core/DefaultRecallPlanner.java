package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.DiversityConfig;
import org.bitmagic.ifeed.application.recommendation.recall.model.FusionConfig;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallPlan;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallRequest;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;

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

        // 平均分配每个策略的召回配额，并对剩余的名额做一次补偿
        int perStrategy = Math.max(1, request.topK() / availableStrategies.size());
        Map<StrategyId, Integer> quotas = new EnumMap<>(StrategyId.class);
        availableStrategies.forEach(id -> quotas.put(id, perStrategy));

        int remaining = Math.max(0, request.topK() - perStrategy * availableStrategies.size());
        var iterator = availableStrategies.iterator();
        while (remaining > 0 && iterator.hasNext()) {
            StrategyId next = iterator.next();
            quotas.computeIfPresent(next, (k, v) -> v + 1);
            remaining--;
        }

        Map<StrategyId, Double> weights = new EnumMap<>(StrategyId.class);
        availableStrategies.forEach(id -> weights.put(id, 1.0d));

        // 从请求的过滤器中读取交织和多样化配置
        boolean interleave = parseBoolean(request.filters().getOrDefault("interleaveChannels", Boolean.TRUE));
        DiversityConfig diversity = extractDiversityConfig(request);

        FusionConfig config = new FusionConfig(request.topK(), true, weights, interleave, diversity);
        return new RecallPlan(quotas, config);
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
