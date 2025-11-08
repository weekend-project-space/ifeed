package org.bitmagic.ifeed.application.recommendation.recall.strategies;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallStrategy;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemFreshnessProvider;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 最新物品召回
 */
@Component
@ConditionalOnBean({ItemFreshnessProvider.class})
@RequiredArgsConstructor
public class LatestRecallStrategy implements RecallStrategy {

    private final ItemFreshnessProvider itemFreshnessProvider;

    @Override
    public StrategyId id() {
        return StrategyId.LATEST;
    }

    @Override
    public List<ItemCandidate> recall(UserContext context, int limit) {
        // 用户无向量时返回空集合，防止影响召回效率
        return itemFreshnessProvider.latest(limit).stream().map(this::toCandidate).toList();
    }

    private ItemCandidate toCandidate(ScoredId scored) {
        return ItemCandidate.of(scored.id(), scored.score(), id());
    }
}
