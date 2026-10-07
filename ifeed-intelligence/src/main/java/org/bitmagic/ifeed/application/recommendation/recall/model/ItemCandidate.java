package org.bitmagic.ifeed.application.recommendation.recall.model;

import java.util.Map;
import java.util.Objects;

/**
 * 召回阶段返回的候选物品，包含打分、来源策略以及附加属性。
 */
public record ItemCandidate(long itemId,
                            double score,
                            StrategyId source,
                            String reason,
                            Map<String, Object> attributes) {

    public ItemCandidate {
        Objects.requireNonNull(source, "source");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static ItemCandidate of(long itemId, double score, StrategyId source) {
        return new ItemCandidate(itemId, score, source, null, Map.of());
    }

    public ItemCandidate withScore(double newScore) {
        return new ItemCandidate(itemId, newScore, source, reason, attributes);
    }


//    public ItemCandidate mix(ItemCandidate mix) {
//        return new ItemCandidate(itemId, score + mix.score, StrategyId.MIX, mix.reason, attributes);
//    }
}
