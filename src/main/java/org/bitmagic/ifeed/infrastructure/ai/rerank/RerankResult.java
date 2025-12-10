package org.bitmagic.ifeed.infrastructure.ai.rerank;

import org.springframework.ai.model.ModelResult;
import org.springframework.ai.model.ResultMetadata;

/**
 * @author yangrd
 * @date 2025/11/11
 **/
public record RerankResult(
        String document,
        double relevanceScore,
        int index
) implements ModelResult<Double>, ResultMetadata {
    @Override
    public Double getOutput() {
        return relevanceScore;
    }

    @Override
    public ResultMetadata getMetadata() {
        return this;
    }
}