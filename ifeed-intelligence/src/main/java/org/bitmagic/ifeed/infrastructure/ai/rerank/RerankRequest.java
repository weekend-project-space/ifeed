package org.bitmagic.ifeed.infrastructure.ai.rerank;

import org.springframework.ai.model.ModelOptions;
import org.springframework.ai.model.ModelRequest;

import java.util.List;

// RerankRequest.java
public record RerankRequest(
        String query,
        List<String> documents,
        RerankOptions options
) implements ModelRequest<String> {
    // 静态工厂方法（推荐）
    public static RerankRequest create(String query, List<String> documents) {
        return new RerankRequest(query, List.copyOf(documents), RerankOptions.create());
    }

    public RerankRequest withOptions(RerankOptions options) {
        return new RerankRequest(query, documents, options);
    }

    // 防御性拷贝（防止外部修改）
    public RerankRequest {
        documents = List.copyOf(documents);
        if (options == null) options = RerankOptions.create();
    }

    @Override
    public String getInstructions() {
        return query;
    }

    @Override
    public ModelOptions getOptions() {
        return options;
    }
}