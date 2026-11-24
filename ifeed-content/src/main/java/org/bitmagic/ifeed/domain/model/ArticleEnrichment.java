package org.bitmagic.ifeed.domain.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * @author yangrd
 * @date 2025/12/15
 * 增强信息
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "article_enrichments")
public class ArticleEnrichment {

    @Id
    @Column(name = "article_id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", length = 1)
    private Rating rating;

    @Column(name = "ai_summary", columnDefinition = "text")
    private String aiSummary;

    @Column(name = "mind_map", columnDefinition = "text")
    private String mindMap;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * 文章评级枚举
     */
    public enum Rating {
        A, // 优秀
        B, // 良好
        C, // 一般
        D  // 较差
    }
}