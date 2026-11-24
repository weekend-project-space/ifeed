package org.bitmagic.ifeed.domain.model.tracking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "topic_track_subscriptions", uniqueConstraints = @UniqueConstraint(
        name = "uq_topic_track_subscriptions_topic_user", columnNames = {"topic_track_id", "user_id"}), indexes = {
        @Index(name = "idx_topic_track_subscriptions_user", columnList = "user_id, created_at")
})
public class TopicTrackSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_track_id", nullable = false)
    private TopicTrack topicTrack;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
