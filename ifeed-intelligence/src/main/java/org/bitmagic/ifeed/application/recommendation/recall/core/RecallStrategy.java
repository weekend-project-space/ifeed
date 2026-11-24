package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;

import java.util.List;

/**
 * 召回策略统一接口，每个策略根据用户上下文返回一组候选物品。
 */
public interface RecallStrategy {

    StrategyId id();

    List<ItemCandidate> recall(UserContext context, int limit);
}
