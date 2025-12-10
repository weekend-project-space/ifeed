package org.bitmagic.ifeed.application.recommendation.recall.spi;

import java.util.List;

/**
 * 用户相似度查询接口，返回相似用户及其高频物品。
 */
public interface UserNeighborFinder {

    List<UserNeighbor> topNeighbors(Integer userId, int k);

    record UserNeighbor(Integer userId, double similarity, List<ScoredId> topItems) {
    }
}
