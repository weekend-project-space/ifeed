package org.bitmagic.ifeed.application.recommendation.recall.model;

/**
 * 召回融合阶段所需的上下文，包含原始请求与融合配置。
 */
public record FusionContext(RecallRequest request, FusionConfig config) {
}
