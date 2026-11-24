package org.bitmagic.ifeed.infrastructure.text.search.tracking;

import java.util.List;

public interface TopicArticleSearchStore {

    List<TopicArticleCandidate> search(TopicArticleSearchRequest request);
}
