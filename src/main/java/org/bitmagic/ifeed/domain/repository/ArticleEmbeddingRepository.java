package org.bitmagic.ifeed.domain.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArticleEmbeddingRepository {


    private final VectorStore vectorStore;

    private final JdbcTemplate jdbcTemplate;


    public void upsert(
            Integer feedId,
            String feedTitle,
            Long articleId,
            String title,
            String category,
            String tags,
            String summary,
            String content,
            String link,
            Instant publishedAt) {
        var textBody = StringUtils.hasText(summary) ? summary : content;
        textBody = "#标题:%s\n作者:%s\n 时间:%s\n分类:%s\n标签:%s\n大纲:%s".formatted(title, feedTitle, publishedAt.toString(), category, tags, summary);
        if (!StringUtils.hasText(textBody)) {
            log.debug("Skip embedding persistence for article {} because there is no textual content", articleId);
            return;
        }

        var document = Document.builder()
                .id(articleId.toString())
                .text(textBody)
                .metadata(buildMetadata(feedId, feedTitle, articleId, title, link, summary, publishedAt))
                .build();

        vectorStore.add(List.of(document));
    }

    public Optional<ArticleEmbeddingRecord> findById(Long articleId) {
        if (articleId == null) {
            return Optional.empty();
        }
        var sql = """
                SELECT id, embedding FROM article_embeddings  WHERE id = ?
                """;
        List<ArticleEmbeddingRecord> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            String value = rs.getString("embedding");
            float[] vector = parseVector(value);
            return new ArticleEmbeddingRecord(rs.getLong("id"), vector);
        }, articleId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    public List<ArticleEmbeddingRecord> findAllByIds(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyList();
        }
        var params = articleIds.stream().distinct().toList();
        var placeholders = params.stream().map(id -> "?").toList();
        var sql = """
                SELECT id, embedding
                FROM article_embeddings
                WHERE id IN (%s)
                """.formatted(String.join(",", placeholders));
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String value = rs.getString("embedding");
            float[] vector = parseVector(value);
            return new ArticleEmbeddingRecord(rs.getLong("id"), vector);
        }, articleIds.toArray(new Long[]{}));
    }


    private Map<String, Object> buildMetadata(Integer feedId,
                                              String feedTitle,
                                              Long articleId,
                                              String title,
                                              String link,
                                              String summary,
                                              Instant publishedAt) {
        var metadata = new HashMap<String, Object>();
        metadata.put("articleId", articleId);
        metadata.put("feedId", feedId != null ? feedId : null);
        if (StringUtils.hasText(feedTitle)) {
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
            metadata.put("publishedAt", publishedAt.getEpochSecond());
        }
        metadata.values().removeIf(value -> value == null || (value instanceof String str && !StringUtils.hasText(str)));
        return metadata;
    }

    private float[] parseVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()) {
            return new float[1024]; // 默认1024维，填充0
        }
        // 去掉大括号，分割逗号
        String cleaned = vectorStr.replace("[", "").replace("]", "");
        String[] values = cleaned.split(",");
        float[] embedding = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            embedding[i] = Float.parseFloat(values[i].trim());
        }
        return embedding;
    }


}
