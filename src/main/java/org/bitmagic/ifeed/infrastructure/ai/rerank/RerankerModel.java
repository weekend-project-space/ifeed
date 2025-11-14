package org.bitmagic.ifeed.infrastructure.ai.rerank;

// RerankerModel.java

import org.springframework.ai.model.Model;

import java.util.List;

/**
 * Reranker 模型接口，参考 {@link org.springframework.ai.embedding.EmbeddingModel}
 */
public interface RerankerModel extends Model<RerankRequest, RerankResponse> {

    /**
     * 执行 rerank
     */
    RerankResponse call(RerankRequest request);

    /**
     * 便捷方法：返回 topN 文档
     */
    default List<String> rerankDocuments(String query, List<String> documents, RerankOptions options) {
        RerankRequest request = RerankRequest.create(query, documents)
                .withOptions(options);
        return call(request).results().stream()
                .map(RerankResult::document)
                .toList();
    }

    /**
     * 便捷方法：返回 topN 文档
     */
    default List<RerankResult> rerankDocumentsResult(String query, List<String> documents, RerankOptions options) {
        RerankRequest request = RerankRequest.create(query, documents)
                .withOptions(options);
        return call(request).results();
    }
}