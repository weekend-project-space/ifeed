package org.bitmagic.ifeed.application.recommendation.recall.spi;

import java.util.List;

/**
 * 属性到物品的倒排索引，支持按偏好批量获取候选。
 */
public interface InvertedIndex {

    List<ScoredId> query(List<UserPreferenceService.AttributePreference> attributes, int k);
}
