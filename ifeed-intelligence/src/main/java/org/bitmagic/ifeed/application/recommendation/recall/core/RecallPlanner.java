package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.RecallPlan;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallRequest;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;

import java.util.Collection;

/**
 * 根据请求与可用策略确定各通道的召回配额及融合配置。
 */
public interface RecallPlanner {

    RecallPlan plan(RecallRequest request, Collection<StrategyId> availableStrategies);
}
