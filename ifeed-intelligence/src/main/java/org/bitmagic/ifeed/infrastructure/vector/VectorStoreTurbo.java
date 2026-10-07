package org.bitmagic.ifeed.infrastructure.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
public interface VectorStoreTurbo extends VectorStore {

    @Nullable
    List<Document> similaritySearch(SearchRequestTurbo request);
}
