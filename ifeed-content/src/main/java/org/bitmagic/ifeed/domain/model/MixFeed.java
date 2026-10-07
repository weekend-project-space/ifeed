package org.bitmagic.ifeed.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.bitmagic.ifeed.domain.model.value.MixFeedFilterConfig;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mix_feeds")
public class MixFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "uid", nullable = false, updatable = false, unique = true)
    private UUID uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 创建者

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "icon", length = 255)
    private String icon;

    @Column(name = "subscriber_count", nullable = false)
    @Builder.Default
    private Integer subscriberCount = 0;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "filter_config", nullable = false, columnDefinition = "text")
    private String filterConfig; // JSON 格式

    @Column(name = "last_fetched")
    private Instant lastFetched;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (uid == null) {
            uid = UUID.randomUUID();
        }
        if (isPublic == null) {
            isPublic = false;
        }
        if (subscriberCount == null) {
            subscriberCount = 0;
        }
    }

    public MixFeedFilterConfig config() {
        return Objects.isNull(this.filterConfig) ? null : JSON.fromJson(this.filterConfig, MixFeedFilterConfig.class);
    }
}
