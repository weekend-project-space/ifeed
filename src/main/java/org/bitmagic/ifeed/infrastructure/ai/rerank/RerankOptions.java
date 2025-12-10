package org.bitmagic.ifeed.infrastructure.ai.rerank;

import org.springframework.ai.model.ModelOptions;

/**
 * @author yangrd
 * @date 2025/11/11
 **/
public record RerankOptions(
        String model,
        Integer topN,
        Double minimumRelevanceScore
) implements ModelOptions {
    // 静态工厂 + 链式构建
    public static RerankOptions create() {
        return new RerankOptions("bge-reranker-v2-m3", null, null);
    }

    public RerankOptions withModel(String model) {
        return new RerankOptions(model,  topN, minimumRelevanceScore);
    }

    public RerankOptions withTopN(Integer topN) {
        return new RerankOptions(model, topN, minimumRelevanceScore);
    }

    public RerankOptions withMinScore(Double score) {
        return new RerankOptions(model,  topN, score);
    }
}