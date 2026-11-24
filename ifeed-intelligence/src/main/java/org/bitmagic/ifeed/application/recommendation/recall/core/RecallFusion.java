package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.FusionContext;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;

import java.util.List;
import java.util.Map;

/**
 * 负责融合多个召回通道的结果，输出最终候选集合。
 */
public interface RecallFusion {

    List<ItemCandidate> fuse(Map<StrategyId, List<ItemCandidate>> channelResults, FusionContext context);
}
