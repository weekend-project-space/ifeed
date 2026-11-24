package org.bitmagic.ifeed.application.recommendation.recall.strategies;

import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * 最新物品召回
 */
@Component
@ConditionalOnBean({ItemProvider.class})
public class LatestRecallStrategy extends AbstractRecallStrategy {

    @Override
    public StrategyId id() {
        return StrategyId.LATEST;
    }
}
