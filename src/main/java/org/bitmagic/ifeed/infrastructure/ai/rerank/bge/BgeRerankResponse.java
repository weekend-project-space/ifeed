package org.bitmagic.ifeed.infrastructure.ai.rerank.bge;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/11
 **/
public record BgeRerankResponse(
        String model,
        String object,
        BgeUsage usage,
        List<BgeResult> results
) {}

record BgeUsage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("total_tokens") int totalTokens
) {}

record BgeResult(
        int index,
        @JsonProperty("relevance_score") double relevanceScore
) {}