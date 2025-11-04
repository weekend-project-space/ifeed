package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 召回策略注册表，按策略标识管理实际的策略实现。
 */
public class StrategyRegistry {

    private final Map<StrategyId, RecallStrategy> strategies;

    public StrategyRegistry(List<RecallStrategy> strategies) {
        this.strategies = new EnumMap<>(StrategyId.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.id(), strategy));
    }

    public RecallStrategy get(StrategyId id) {
        RecallStrategy strategy = strategies.get(id);
        if (strategy == null) {
            throw new IllegalArgumentException("No recall strategy registered for id " + id);
        }
        return strategy;
    }

    public Collection<StrategyId> available() {
        return strategies.keySet();
    }
}
