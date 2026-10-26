package org.bitmagic.ifeed.application.tracking;

import org.bitmagic.ifeed.config.properties.TopicTrackingProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class TopicTrackEmbeddingService {


    private final EmbeddingModel embeddingModel;
    private final TopicTrackingProperties properties;

    public TopicTrackEmbeddingService(@Qualifier("embeddingModelMini") EmbeddingModel embeddingModel, TopicTrackingProperties properties) {
        this.embeddingModel = embeddingModel;
        this.properties = properties;
    }

    public List<ArticleEmbeddingRecord> embed(List<Article> articles) {
        List<ArticleEmbeddingRecord> embeddings = new ArrayList<>();
        int dimensions = 0;
        int batchSize = properties.getEmbeddingBatchSize();
        for (int start = 0; start < articles.size(); start += batchSize) {
            List<Article> batch = articles.subList(start, Math.min(start + batchSize, articles.size()));
            List<float[]> vectors = embeddingModel.embed(batch.stream().map(this::text).toList());
            if (vectors == null || vectors.size() != batch.size()) {
                throw new IllegalStateException("Topic embedding response count does not match articles");
            }
            for (int index = 0; index < batch.size(); index++) {
                float[] vector = vectors.get(index);
                if (vector == null || vector.length == 0) throw new IllegalStateException("Topic embedding is empty");
                if (dimensions == 0) dimensions = vector.length;
                if (vector.length != dimensions) throw new IllegalStateException("Topic embedding dimensions do not match");
                double norm = 0;
                for (float value : vector) {
                    if (!Float.isFinite(value)) throw new IllegalStateException("Topic embedding contains non-finite values");
                    norm += (double) value * value;
                }
                if (norm == 0) throw new IllegalStateException("Topic embedding is a zero vector");
                embeddings.add(new ArticleEmbeddingRecord(batch.get(index).getId(), vector));
            }
        }
        return embeddings;
    }

    private String text(Article article) {
        String title = StringUtils.hasText(article.getTitle()) ? article.getTitle().trim() : "";
        String content = StringUtils.hasText(article.getSummary()) ? article.getSummary()
                : StringUtils.hasText(article.getDescription()) ? article.getDescription() : article.getContent();
        if (!StringUtils.hasText(content)) content = "";
        String text = (title + "\n" + content.trim()).trim();
        if (!StringUtils.hasText(text)) throw new IllegalStateException("Article has no text for topic embedding: " + article.getId());
        return text.substring(0, Math.min(text.length(), properties.getEmbeddingTextMaxLength()));
    }
}
