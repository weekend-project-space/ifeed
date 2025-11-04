package org.bitmagic.ifeed.application.recommendation.recall.model;

import java.util.Map;

/**
 * 各召回通道的配额计划以及对应的融合配置。
 */
public record RecallPlan(Map<StrategyId, Integer> quotas,
                         FusionConfig fusionConfig) {

    public RecallPlan {
        quotas = Map.copyOf(quotas);
    }

    public int quota(StrategyId id) {
        return quotas.getOrDefault(id, 0);
    }
}
