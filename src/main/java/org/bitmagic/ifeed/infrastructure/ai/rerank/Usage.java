package org.bitmagic.ifeed.infrastructure.ai.rerank;

/**
 * @author yangrd
 * @date 2025/11/11
 **/
public record Usage(
        int promptTokens,
        int totalTokens
) {
}