package org.bitmagic.ifeed.application.recommendation.recall.model;

import java.util.Map;

/**
 * 召回融合配置，控制去重、通道权重、交织排序以及多样化策略等参数。
 */
public record FusionConfig(int topK,
                           boolean deduplicate,
                           Map<StrategyId, Double> channelWeights,
                           boolean interleaveChannels,
                           DiversityConfig diversityConfig) {

    public FusionConfig {
        channelWeights = channelWeights == null ? Map.of() : Map.copyOf(channelWeights);
        diversityConfig = diversityConfig == null ? DiversityConfig.disabled() : diversityConfig;
    }

    public double weightOf(StrategyId id) {
        return channelWeights.getOrDefault(id, 1.0d);
    }

    public boolean hasDiversity() {
        return diversityConfig.enabled();
    }
}
