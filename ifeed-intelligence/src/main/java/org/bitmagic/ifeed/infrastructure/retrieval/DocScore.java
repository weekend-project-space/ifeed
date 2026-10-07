package org.bitmagic.ifeed.infrastructure.retrieval;

import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.springframework.ai.document.Document;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
public record DocScore(Long docId, double score, Instant pubDate, String source, Object meta) {

    // 按权重缩放分数
    public DocScore scale(double weight) {
        return new DocScore(this.docId, this.score * weight, this.pubDate, this.source, this.meta);
    }

    public DocScore from(double score) {
        return new DocScore(this.docId, score, this.pubDate, this.source, this.meta);
    }

    // 合并两个 DocScore（同一篇文章多通道命中）
    public DocScore combine(DocScore other) {
        if (!Objects.equals(docId, other.docId)) {
            throw new RuntimeException();
        }
        double mergedScore = Math.max(this.score, other.score);
        Instant newerDate = this.pubDate.isAfter(other.pubDate) ? this.pubDate : other.pubDate;
        return new DocScore(this.docId, mergedScore, newerDate, this.source, this.meta != null ? this.meta : other.meta);
    }

    public String text() {
        if (meta instanceof Document) {
            return ((Document) meta).getText();
        } else if (meta instanceof String) {
            return (String) meta;
        } else if (meta instanceof Map<?, ?>) {
            return JSON.toJson(meta);
        } else {
            return meta.toString();
        }
    }
}