package org.bitmagic.ifeed.domain.model.radar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "radar_topic_article")
@IdClass(RadarTopicArticleId.class)
public class RadarTopicArticle {

    @Id
    @Column(name = "snapshot_id", nullable = false)
    private String snapshotId;

    @Id
    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Id
    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "published_at")
    private Instant publishedAt;
}

