package org.bitmagic.ifeed.infrastructure.text.search;

/**
 * 带分数的文档
 */
public record ScoredDocument(
        Document document,
        double score
) {}
