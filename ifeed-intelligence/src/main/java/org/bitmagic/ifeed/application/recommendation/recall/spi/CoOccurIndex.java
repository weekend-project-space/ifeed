package org.bitmagic.ifeed.application.recommendation.recall.spi;

import java.util.List;

/**
 * 物品与物品的共现关系索引（共购、共看等）。
 */
public interface CoOccurIndex {

    List<ScoredId> topRelated(Long itemId, int k);
}
