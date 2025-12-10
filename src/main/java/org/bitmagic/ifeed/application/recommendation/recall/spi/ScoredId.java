package org.bitmagic.ifeed.application.recommendation.recall.spi;

import java.util.Map;
import java.util.Objects;

/**
 * 召回底层返回的通用ID及评分结构，可附带元数据。
 * 排序项
 */
public record ScoredId(long id, double score, Map<String, Object> metadata) {

    public ScoredId {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static ScoredId of(long id, double score) {
        return new ScoredId(id, score, Map.of());
    }
}
