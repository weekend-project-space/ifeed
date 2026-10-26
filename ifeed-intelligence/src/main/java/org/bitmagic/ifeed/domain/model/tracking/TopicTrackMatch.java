package org.bitmagic.ifeed.domain.model.tracking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "topic_track_matches", uniqueConstraints = @UniqueConstraint(
        name = "uq_topic_track_matches_track_version_article", columnNames = {"topic_track_id", "config_version", "article_id"}))
public class TopicTrackMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_track_id", nullable = false)
    private TopicTrack topicTrack;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "config_version", nullable = false)
    private int configVersion;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt;
}
