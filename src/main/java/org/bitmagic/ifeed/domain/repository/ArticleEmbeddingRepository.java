package org.bitmagic.ifeed.domain.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArticleEmbeddingRepository {


    private final VectorStore vectorStore;

    @Async
    public void upsert(UUID articleId,
                       UUID feedId,
                       String feedTitle,
                       String title,
                       String summary,
                       String content,
                       String link,
                       Instant publishedAt) {
        var textBody = StringUtils.hasText(summary) ? summary : content;
        textBody = "作者:[%s]\n时间:[%s]\n标题:[%s]\n大纲:[%s]".formatted(feedTitle, title, publishedAt.toString(), summary);
        if (!StringUtils.hasText(textBody)) {
            log.debug("Skip embedding persistence for article {} because there is no textual content", articleId);
            return;
        }

        var document = Document.builder()
                .id(articleId.toString())
                .text(textBody)
                .metadata(buildMetadata(articleId, feedId, feedTitle, title, link, summary, publishedAt))
                .build();


        vectorStore.delete(List.of(articleId.toString()));
        vectorStore.add(List.of(document));
    }

    private Map<String, Object> buildMetadata(UUID articleId,
                                              UUID feedId,
                                              String feedTitle,
                                              String title,
                                              String link,
                                              String summary,
                                              Instant publishedAt) {
        var metadata = new HashMap<String, Object>();
        metadata.put("articleId", articleId.toString());
        metadata.put("feedId", feedId != null ? feedId.toString() : null);
        f(StringUtils.hasText(feedTitle)) {
            metadata.put("feedTitle", feedTitle);
        }
        if (StringUtils.hasText(title)) {
            metadata.put("title", title);
        }
        if (StringUtils.hasText(link)) {
            metadata.put("link", link);
        }
        if (StringUtils.hasText(summary)) {
            metadata.put("summary", summary);
        }
        if (publishedAt != null) {
            metadata.put("publishedAt", publishedAt.toString());
        }
        metadata.values().removeIf(value -> value == null || (value instanceof String str && !StringUtils.hasText(str)));
        return metadata;
    }

}
