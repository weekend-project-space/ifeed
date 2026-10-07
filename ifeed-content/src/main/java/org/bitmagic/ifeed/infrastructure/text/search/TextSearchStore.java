package org.bitmagic.ifeed.infrastructure.text.search;

/**
 * @author yangrd
 * @date 2025/11/24
 **/

import java.util.List;
import java.util.Optional;

// ============ 核心接口 ============

/**
 * 文本搜索存储的核心接口
 */
public interface TextSearchStore {
    void add(List<Document> documents);

    Optional<Boolean> delete(List<Long> idList);

    List<Document> similaritySearch(SearchRequest request);

    List<ScoredDocument> similaritySearchWithScore(SearchRequest request);
}


