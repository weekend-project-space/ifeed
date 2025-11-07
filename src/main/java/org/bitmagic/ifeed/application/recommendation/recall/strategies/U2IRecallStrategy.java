package org.bitmagic.ifeed.application.recommendation.recall.strategies;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallStrategy;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.AnnIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.EmbeddingStore;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户到物品（U2I）召回：利用用户向量在ANN索引中直接检索高相似度物品。
 */
@Component
@ConditionalOnBean({EmbeddingStore.class, AnnIndex.class})
@RequiredArgsConstructor
public class U2IRecallStrategy implements RecallStrategy {

    private final EmbeddingStore embeddingStore;
    private final AnnIndex annIndex;

    @Override
    public StrategyId id() {
        return StrategyId.U2I;
    }

    @Cacheable(cacheNames = "U2I", key = "#p0.userId", unless = "#result == null")
    @Override
    public List<ItemCandidate> recall(UserContext context, int limit) {
        // 用户无向量时返回空集合，防止影响召回效率
        return embeddingStore.getUserVector(context.userId())
                .map(vector -> annIndex.query(vector, limit, context.attributes()))
                .map(results -> results.stream()
                        .map(this::toCandidate)
                        .toList())
                .orElse(List.of());
    }

    private ItemCandidate toCandidate(ScoredId scored) {
        return ItemCandidate.of(scored.id(), scored.score(), id());
    }
}
