package org.bitmagic.ifeed.application.recommendation.recall.model;

import java.util.Map;
import java.util.Set;

/**
 * 召回融合配置（不可变）
 * <p>
 * 约定：
 * - 融合阶段负责「排序信号融合」
 * - 去重 / 多样化属于融合后的约束
 */
public record FusionConfig(

        /** 最终输出条数（融合 + 排序 + 多样化之后） */
        int topK,

        /** 是否按 itemId 去重 */
        boolean deduplicate,

        /** 各召回策略权重（RRF 使用，默认 1.0） */
        Map<StrategyId, Double> channelWeights,

        /** 是否启用通道交织（非 RRF 模式） */
        boolean interleaveChannels,

        /** 多样性配置（融合后处理） */
        DiversityConfig diversityConfig,

        /* ================= RRF 专用 ================= */

        /** RRF 平滑常数（典型值 60 ~ 200） */
        int rrfK,

        /** 每个策略最多参与融合的排名深度 */
        int maxRankPerChannel,

        /**
         * RRF 混合系数：
         * final = alpha * sumRrf + (1 - alpha) * maxRrf
         */
        double rrfAlpha
) {

    public FusionConfig {
        channelWeights = channelWeights == null ? Map.of() : Map.copyOf(channelWeights);
        diversityConfig = diversityConfig == null ? DiversityConfig.disabled() : diversityConfig;

        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be > 0");
        }
        if (rrfK <= 0) {
            throw new IllegalArgumentException("rrfK must be > 0");
        }
        if (maxRankPerChannel <= 0) {
            throw new IllegalArgumentException("maxRankPerChannel must be > 0");
        }
        if (rrfAlpha < 0.0 || rrfAlpha > 1.0) {
            throw new IllegalArgumentException("rrfAlpha must be in [0,1]");
        }

        channelWeights.forEach((id, w) -> {
            if (w == null || w.isNaN() || w < 0) {
                throw new IllegalArgumentException(
                        "Invalid channel weight: " + id + " -> " + w
                );
            }
        });
    }

    /* =============== 语义化 helper =============== */

    public double weightOf(StrategyId id) {
        return channelWeights.getOrDefault(id, 1.0d);
    }

    public boolean isDisabled(StrategyId id) {
        return weightOf(id) == 0.0d;
    }

    public boolean hasDiversity() {
        return diversityConfig.enabled();
    }


    /* =============== 推荐默认值 =============== */

    public static FusionConfig defaultRrf(int topK) {
        return new FusionConfig(
                topK,
                true,
                Map.of(),
                false,
                DiversityConfig.disabled(),
                60,   // rrfK
                50,    // maxRankPerChannel
                0.8    // rrfAlpha
        );
    }
}
