package org.bitmagic.ifeed.domain.model.radar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "radar_topic")
public class RadarTopic {

    @Id
    @Column(name = "topic_id", nullable = false, updatable = false)
    private UUID topicId;

    @Column(name = "snapshot_id", nullable = false)
    private String snapshotId;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "article_count", nullable = false)
    private Integer articleCount;

    @Column(name = "top_keywords", columnDefinition = "text")
    private String topKeywords;

    @Column(name = "score", nullable = false)
    private Double score;
}

