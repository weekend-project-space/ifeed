package org.bitmagic.ifeed.application.recommendation.recall.spi;

import java.util.List;
import java.util.Map;

/**
 * 向量近似最近邻索引接口，面向Embedding召回场景。
 */
public interface AnnIndex {

    List<ScoredId> query(float[] vector, int k, Map<String, Object> filters);
}
