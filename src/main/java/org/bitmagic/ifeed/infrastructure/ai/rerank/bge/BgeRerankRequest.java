package org.bitmagic.ifeed.infrastructure.ai.rerank.bge;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/11
 **/
public record BgeRerankRequest(
        String query,
        List<String> documents,
        String model,
        @JsonProperty("encoding_format") String encodingFormat
) {
    public BgeRerankRequest {
        documents = List.copyOf(documents);
        if (model == null || model.isBlank()) {
            model = "bge-reranker-v2-m3-Q4_K_M.gguf";
        }
        if (encodingFormat == null) {
            encodingFormat = "float";
        }
    }

    public static BgeRerankRequest of(String query, List<String> documents) {
        return new BgeRerankRequest(query, documents, null, null);
    }
}