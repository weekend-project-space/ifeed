package org.bitmagic.ifeed.application.retrieval;

import java.time.Instant;
import java.util.Objects;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
public record DocScore(Long docId, double score, Instant pubDate, Object meta) {

    // 按权重缩放分数
    public DocScore scale(double weight) {
        return new DocScore(this.docId, this.score * weight, this.pubDate, this.meta);
    }

    public DocScore from(double score) {
        return new DocScore(this.docId, score, this.pubDate, this.meta);
    }

    // 合并两个 DocScore（同一篇文章多通道命中）
    public DocScore combine(DocScore other) {
        if (!Objects.equals(docId, other.docId)) {
            throw new RuntimeException();
        }
        double mergedScore = this.score + other.score;
        Instant newerDate = this.pubDate.isAfter(other.pubDate) ? this.pubDate : other.pubDate;
        return new DocScore(this.docId, mergedScore, newerDate, this.meta != null ? this.meta : other.meta);
    }
}