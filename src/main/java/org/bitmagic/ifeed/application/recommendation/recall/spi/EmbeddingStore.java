package org.bitmagic.ifeed.application.recommendation.recall.spi;

import java.util.Optional;

/**
 * 用户与物品向量存取接口，可封装数据库或在线服务。
 */
public interface EmbeddingStore {

    Optional<float[]> getUserVector(Integer userId);

    Optional<float[]> getItemVector(Long itemId);
}
