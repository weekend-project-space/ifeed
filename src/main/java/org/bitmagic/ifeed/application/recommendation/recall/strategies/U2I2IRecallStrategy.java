package org.bitmagic.ifeed.application.recommendation.recall.strategies;

import org.bitmagic.ifeed.application.recommendation.recall.core.RecallStrategy;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.AnnIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.CoOccurIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.EmbeddingStore;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户到物品再到物品（U2I2I）召回：先找用户相关的种子物品，再扩展相似/互补物品。
 */
@Component
@ConditionalOnBean({EmbeddingStore.class, AnnIndex.class, CoOccurIndex.class})
public class U2I2IRecallStrategy implements RecallStrategy {

    private final EmbeddingStore embeddingStore;
    private final AnnIndex annIndex;
    private final CoOccurIndex coOccurIndex;
    private final int seedLimit;
    private final int perSeedLimit;

    public U2I2IRecallStrategy(EmbeddingStore embeddingStore,
                               AnnIndex annIndex,
                               CoOccurIndex coOccurIndex,
                               @Value("${recall.u2i2i.seed-limit:3}") int seedLimit,
                               @Value("${recall.u2i2i.per-seed-limit:10}") int perSeedLimit) {
        this.embeddingStore = embeddingStore;
        this.annIndex = annIndex;
        this.coOccurIndex = coOccurIndex;
        this.seedLimit = seedLimit;
        this.perSeedLimit = perSeedLimit;
    }

    @Override
    public StrategyId id() {
        return StrategyId.U2I2I;
    }

    @Cacheable(cacheNames = "U2I2I", key = "#p0.userId", unless = "#result == null")
    @Override
    public List<ItemCandidate> recall(UserContext context, int limit) {
        return embeddingStore.getUserVector(context.userId())
                .map(vector -> expandWithSeeds(vector, context, limit))
                .orElse(List.of());
    }

    private List<ItemCandidate> expandWithSeeds(float[] vector, UserContext context, int limit) {
        List<ScoredId> seeds = annIndex.query(vector, seedLimit, context.attributes());
        if (seeds.isEmpty()) {
            return List.of();
        }

        Set<Long> history = Set.copyOf(context.recentItemIds());
        Map<Long, Double> merged = new HashMap<>();
        for (ScoredId seed : seeds) {
            double seedWeight = seed.score();
            // 避免推荐已看内容，种子得分作为相似扩展的权重
            for (ScoredId neighbor : coOccurIndex.topRelated(seed.id(), perSeedLimit)) {
                long candidateId = neighbor.id();
                if (history.contains(candidateId) || neighbor.id() == seed.id()) {
                    continue;
                }
                merged.merge(candidateId, neighbor.score() * seedWeight, Double::sum);
            }
        }

        return merged.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> ItemCandidate.of(entry.getKey(), entry.getValue(), id()))
                .toList();
    }

}
