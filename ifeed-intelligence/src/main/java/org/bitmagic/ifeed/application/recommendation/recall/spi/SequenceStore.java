package org.bitmagic.ifeed.application.recommendation.recall.spi;

import java.time.Instant;
import java.util.List;

/**
 * 用户交互序列存储接口，用于获取近期浏览或点击。
 */
public interface SequenceStore {

    List<UserInteraction> recentInteractions(Integer userId, int limit);

    record UserInteraction(long itemId,
                           String itemTitle,
                           double durationSeconds,
                           double weight,
                           Instant timestamp) {
    }
}
