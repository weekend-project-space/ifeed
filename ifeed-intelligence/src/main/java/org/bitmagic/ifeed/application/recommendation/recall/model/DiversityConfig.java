package org.bitmagic.ifeed.application.recommendation.recall.model;

/**
 * 融合阶段的多样化配置，用于限制同一属性下的结果数量。
 */
public record DiversityConfig(String attributeKey,
                              int maxPerAttribute,
                              boolean fillOverflow) {

    public DiversityConfig {
        if (attributeKey != null && attributeKey.isBlank()) {
            attributeKey = null;
        }
        maxPerAttribute = Math.max(0, maxPerAttribute);
    }

    public static DiversityConfig disabled() {
        return new DiversityConfig(null, 0, false);
    }

    public boolean enabled() {
        return attributeKey != null && maxPerAttribute > 0;
    }
}
