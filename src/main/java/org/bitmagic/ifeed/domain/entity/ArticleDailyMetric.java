package org.bitmagic.ifeed.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bitmagic.ifeed.domain.entity.id.ArticleDailyMetricId;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "article_daily_metrics")
public class ArticleDailyMetric {

    @EmbeddedId
    private ArticleDailyMetricId id;

    @Column(name = "collected_cnt", nullable = false)
    private long collectedCount;

    @Column(name = "read_cnt", nullable = false)
    private long readCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ArticleDailyMetric(ArticleDailyMetricId id) {
        this.id = id;
    }

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}
