package org.bitmagic.ifeed.application.recommendation.recall.model;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 召回引擎返回值，包含融合后的候选集合以及各通道的原始结果。
 */
public record RecallResponse(List<ItemCandidate> items,
                             Map<StrategyId, List<ItemCandidate>> channelResults,
                             UserContext userContext,
                             Duration latency,
                             Map<String, Object> debugInfo) {

    public RecallResponse {
        items = List.copyOf(items);
        channelResults = Map.copyOf(channelResults);
        latency = latency == null ? Duration.ZERO : latency;
        debugInfo = debugInfo == null ? Map.of() : Map.copyOf(debugInfo);
    }

    public static RecallResponse empty() {
        return new RecallResponse(List.of(), Map.of(), null, Duration.ZERO, Map.of());
    }

    public RecallResponse withDebug(Map<String, Object> extra) {
        if (extra == null || extra.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new java.util.HashMap<>(debugInfo);
        merged.putAll(extra);
        return new RecallResponse(items, channelResults, userContext, latency, Map.copyOf(merged));
    }
}
