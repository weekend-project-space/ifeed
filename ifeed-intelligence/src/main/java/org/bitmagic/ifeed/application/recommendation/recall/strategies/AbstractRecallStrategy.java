package org.bitmagic.ifeed.application.recommendation.recall.strategies;

import org.bitmagic.ifeed.application.recommendation.recall.core.RecallStrategy;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemProvider;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/10
 **/
public abstract class AbstractRecallStrategy implements RecallStrategy {

    @Autowired
    private  ItemProvider itemProvider;

    @Override
    public List<ItemCandidate> recall(UserContext context, int limit) {
        // 用户无向量时返回空集合，防止影响召回效率
        return itemProvider.ls(context, ItemProvider.ScoredLsType.valueOf(id().name()), limit).stream().map(this::toCandidate).toList();
    }

    private ItemCandidate toCandidate(ScoredId scored) {
        return ItemCandidate.of(scored.id(), scored.score(), id());
    }
}
