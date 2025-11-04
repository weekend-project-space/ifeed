package org.bitmagic.ifeed.application.recommendation.recall.strategies;

import org.bitmagic.ifeed.application.recommendation.recall.core.RecallStrategy;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.CoOccurIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 物品到物品（I2I）召回：根据用户最近互动的种子物品，通过共现索引扩展相似物品。
 */
@Component
@ConditionalOnBean({SequenceStore.class, CoOccurIndex.class})
public class I2IRecallStrategy implements RecallStrategy {

    private final SequenceStore sequenceStore;
    private final CoOccurIndex coOccurIndex;
    private final int seedLimit;
    private final int perSeedLimit;

    public I2IRecallStrategy(SequenceStore sequenceStore,
                             CoOccurIndex coOccurIndex,
                             @Value("${recall.i2i.seed-limit:3}") int seedLimit,
                             @Value("${recall.i2i.per-seed-limit:20}") int perSeedLimit) {
        this.sequenceStore = sequenceStore;
        this.coOccurIndex = coOccurIndex;
        this.seedLimit = seedLimit;
        this.perSeedLimit = perSeedLimit;
    }

    @Override
    public StrategyId id() {
        return StrategyId.I2I;
    }

    @Override
    public List<ItemCandidate> recall(UserContext context, int limit) {
        Integer userId = context.userId();
        List<SequenceStore.UserInteraction> interactions = context.interactions();
        if (interactions.isEmpty() && sequenceStore != null) {
            interactions = sequenceStore.recentInteractions(userId, seedLimit);
        }

        if (interactions.isEmpty()) {
            return List.of();
        }

        Set<Long> history = new HashSet<>(context.recentItemIds());
        Map<Long, Double> scores = new HashMap<>();
        List<SequenceStore.UserInteraction> seeds = interactions.stream()
                .sorted(Comparator.comparing(SequenceStore.UserInteraction::timestamp,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(seedLimit)
                .toList();

        for (int index = 0; index < seeds.size(); index++) {
            SequenceStore.UserInteraction seed = seeds.get(index);
            double baseWeight = seed.weight() > 0 ? seed.weight() : 1.0;
            double seedWeight = baseWeight * Math.exp(-(double) index / Math.max(1, seedLimit));
            // 按种子权重累加候选得分，过滤已点击历史
            for (ScoredId neighbor : coOccurIndex.topRelated(seed.itemId(), perSeedLimit)) {
                long candidateId = neighbor.id();
                if (history.contains(candidateId)) {
                    continue;
                }
                scores.merge(candidateId, neighbor.score() * seedWeight, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> ItemCandidate.of(entry.getKey(), entry.getValue(), id()))
                .toList();
    }

}
