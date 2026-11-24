package org.bitmagic.ifeed.infrastructure.ai.rerank;

import org.springframework.ai.model.ModelResponse;
import org.springframework.ai.model.ResponseMetadata;

import java.util.List;

// RerankResponse.java
public record RerankResponse(
        List<RerankResult> results,
        String model,
        Usage  usage
) implements ModelResponse<RerankResult> {
    public RerankResponse {
        results = List.copyOf(results);
    }

    @Override
    public RerankResult getResult() {
        return null;
    }

    @Override
    public List<RerankResult> getResults() {
        return this.results;
    }

    @Override
    public ResponseMetadata getMetadata() {
        return null;
    }
}