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
import org.bitmagic.ifeed.domain.entity.id.ArticleSimilarityId;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "article_similarity")
public class ArticleSimilarity {

    @EmbeddedId
    private ArticleSimilarityId id;

    @Column(name = "similarity", nullable = false)
    private double similarity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ArticleSimilarity(ArticleSimilarityId id, double similarity) {
        this.id = id;
        this.similarity = similarity;
    }

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}
