package org.bitmagic.ifeed.application.recommendation.recall.strategies;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallStrategy;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.CoOccurIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemProvider;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/10
 **/
@Component
@ConditionalOnBean({ItemProvider.class, CoOccurIndex.class})
@RequiredArgsConstructor
public class RandomI2IStrategy implements RecallStrategy {

    private final ItemProvider itemProvider;

    private final CoOccurIndex coOccurIndex;

    @Override
    public StrategyId id() {
        return StrategyId.RANDOM_I2I;
    }

    @Override
    public List<ItemCandidate> recall(UserContext context, int limit) {
        List<ScoredId> ls = itemProvider.ls(context, ItemProvider.ScoredLsType.RANDOM, limit);
        int i = RandomUtils.secure().randomInt() % ls.size();
        return coOccurIndex.topRelated(ls.get(i).id(), limit).stream().map(this::toCandidate).toList();
    }

    private ItemCandidate toCandidate(ScoredId scored) {
        return ItemCandidate.of(scored.id(), scored.score(), id());
    }
}
