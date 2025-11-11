package org.bitmagic.ifeed.application.recommendation.recall.spi;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据源接口，用于提供候选物品的发布时间等新鲜度信息。
 */
public interface ItemFreshnessProvider {

    Map<Long, Instant> publishedAt(Collection<Long> itemIds);

    static ItemFreshnessProvider noop() {
        return (ids) -> Map.of();
    }
}
