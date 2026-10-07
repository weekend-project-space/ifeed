package org.bitmagic.ifeed.infrastructure.recall;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 召回向量检索相关参数与辅助构造工具。
 */
@Component
@ConfigurationProperties(prefix = "recall.vector")
public class RecallVectorProperties {

    /**
     * 相似度阈值，越大意味着召回更严格。
     */
    private double similarityThreshold = 0.2d;

    public double similarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * 根据召回上下文中的过滤参数构造向量检索的过滤表达式。
     */
    public Optional<Filter.Expression> buildFilter(Map<String, Object> filters) {
        // TODO: 根据业务字段构造向量检索过滤条件
        return Optional.empty();
    }
}
