package org.bitmagic.ifeed.infrastructure.vector;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.bitmagic.ifeed.infrastructure.retrieval.DocScore;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalPipeline;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author yangrd
 * @date 2025/11/19
 **/
@RequiredArgsConstructor
public class Bm25VectorStore implements VectorStore {
    @Delegate
    final VectorStore vectorStore;

    final RetrievalPipeline retrievalPipeline;

    final ChatClient chatClient;

    @Nullable
    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        String topQuery = request.getQuery();//chatClient.prompt("'%s\n提取出用户关键查询词 ，简扼要，不要啰嗦，只给出关键词即可".formatted(request.getQuery())).call().content();
        List<DocScore> docScores = retrievalPipeline.execute(RetrievalContext.builder().query(topQuery).includeGlobal(true).topK(request.getTopK()).build());
        return docScores.stream().map(d -> {
            if (d.meta() instanceof Document) {
                return (Document) d.meta();
            } else if (d.meta() instanceof Map<?, ?>) {
                return new Document(JSON.toJson(d.meta()), (Map<String, Object>) d.meta());
            } else {
                return (Document) d.meta();
            }
        }).toList();
    }
}
