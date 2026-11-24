package org.bitmagic.ifeed.domain.model.tracking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "topic_tracks", indexes = {
        @Index(name = "idx_topic_tracks_owner_updated", columnList = "owner_id, updated_at")
})
public class TopicTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_uid", nullable = false, unique = true, updatable = false)
    private UUID topicUid;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Integer ownerId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "keyword", nullable = false, length = 240)
    private String keyword;

    @Column(name = "normalized_keyword", nullable = false, unique = true, length = 240)
    private String normalizedKeyword;

    @Column(name = "include_keywords", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String includeKeywords = "[]";

    @Column(name = "blocked_keywords", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String blockedKeywords = "[]";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "topic_track_sources", joinColumns = @JoinColumn(name = "topic_track_id"))
    @Column(name = "feed_id", nullable = false)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private Set<Integer> sourceIds = new HashSet<>();

    @Column(name = "categories", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String categories = "[]";

    @Column(name = "relevance_prompt", nullable = false, columnDefinition = "text")
    private String relevancePrompt = "";

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "config_version", nullable = false)
    private int configVersion = 1;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "scan_status", nullable = false, length = 24)
    private String scanStatus = "IDLE";

    @Column(name = "scan_started_at")
    private Instant scanStartedAt;
    @Column(name = "scan_finished_at")
    private Instant scanFinishedAt;
    @Column(name = "scan_error", length = 240)
    private String scanError;
    @Column(name = "scan_token")
    private UUID scanToken;
    @Column(name = "scan_lease_until")
    private Instant scanLeaseUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void ensureUid() {
        if (topicUid == null) topicUid = UUID.randomUUID();
        normalizedKeyword = TopicKeyword.normalize(keyword);
    }
}
