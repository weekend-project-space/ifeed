package org.bitmagic.ifeed.infrastructure.text.search.pg;

import org.bitmagic.ifeed.infrastructure.text.search.Document;

/**
 * 带高亮的搜索结果
 */
public record HighlightedResult(Document document, double score, String headline) {}
