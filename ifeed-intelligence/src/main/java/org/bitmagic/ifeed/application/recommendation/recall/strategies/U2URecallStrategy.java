package org.bitmagic.ifeed.application.recommendation.recall.strategies;

import org.bitmagic.ifeed.application.recommendation.recall.core.RecallStrategy;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserNeighborFinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户到用户（U2U）召回：先找到相似用户，再聚合其高频物品。
 */
//@Component
@ConditionalOnBean(UserNeighborFinder.class)
public class U2URecallStrategy implements RecallStrategy {

    private final UserNeighborFinder neighborFinder;
    private final int neighborLimit;

    public U2URecallStrategy(UserNeighborFinder neighborFinder,
                             @Value("${recall.u2u.neighbor-limit:50}") int neighborLimit) {
        this.neighborFinder = neighborFinder;
        this.neighborLimit = neighborLimit;
    }

    @Override
    public StrategyId id() {
        return StrategyId.U2U;
    }

    @Override
    public List<ItemCandidate> recall(UserContext context, int limit) {
        List<UserNeighborFinder.UserNeighbor> neighbors = neighborFinder.topNeighbors(context.userId(), neighborLimit);
        if (neighbors.isEmpty()) {
            return List.of();
        }
        Set<Long> history = Set.copyOf(context.recentItemIds());
        Map<Long, Double> scoreBoard = new HashMap<>();

        for (UserNeighborFinder.UserNeighbor neighbor : neighbors) {
            double similarity = neighbor.similarity();
            List<ScoredId> items = neighbor.topItems();
            if (items == null || items.isEmpty()) {
                continue;
            }
            // 邻居物品的贡献按相似度衰减，避免热门用户放大
            for (ScoredId scoredId : items) {
                long candidateId = scoredId.id();
                if (history.contains(candidateId)) {
                    continue;
                }
                scoreBoard.merge(candidateId, scoredId.score() * similarity, Double::sum);
            }
        }

        return scoreBoard.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> ItemCandidate.of(entry.getKey(), entry.getValue(), id()))
                .toList();
    }

}
