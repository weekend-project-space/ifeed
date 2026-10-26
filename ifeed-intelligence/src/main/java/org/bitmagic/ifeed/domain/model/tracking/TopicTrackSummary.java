package org.bitmagic.ifeed.domain.model.tracking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "topic_track_summaries", indexes = {
        @Index(name = "idx_topic_track_summaries_created", columnList = "created_at")
})
public class TopicTrackSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_uid", nullable = false, unique = true, updatable = false)
    private UUID summaryUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_track_id", nullable = false)
    private TopicTrack topicTrack;

    @Column(name = "config_version", nullable = false)
    private int configVersion;

    @Column(name = "topic_name", nullable = false, length = 120)
    private String topicName;

    @Column(nullable = false, length = 240)
    private String keyword;

    @Column(name = "summary", nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "titles_only", nullable = false)
    private boolean titlesOnly;

    @Column(name = "article_count", nullable = false)
    private int articleCount;

    @Column(name = "article_previews", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String articlePreviews = "[]";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void ensureUid() {
        if (summaryUid == null) summaryUid = UUID.randomUUID();
    }
}
