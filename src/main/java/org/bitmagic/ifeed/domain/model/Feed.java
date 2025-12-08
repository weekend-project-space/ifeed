package org.bitmagic.ifeed.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bitmagic.ifeed.domain.model.value.FeedFetchStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "feeds")
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "uid", nullable = false, updatable = false, unique = true)
    private UUID uid;

    @Column(name = "url", nullable = false, unique = true, length = 255)
    private String url;

    @Column(name = "site_url", nullable = false, length = 255)
    private String siteUrl;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "last_fetched")
    private Instant lastFetched;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_fetch_status", length = 32)
    @Builder.Default
    private FeedFetchStatus lastFetchStatus = FeedFetchStatus.PENDING;

    @Column(name = "fetch_error_at")
    private Instant fetchErrorAt;

    @Column(name = "fetch_error", length = 2048)
    private String fetchError;

    @Column(name = "failure_count")
    @Builder.Default
    private Integer failureCount = 0;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "featured")
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "update_frequency", length = 20)
    private String updateFrequency;

    @PrePersist
    void onCreate() {
        if (uid == null) {
            uid = UUID.randomUUID();
        }
        if (lastFetchStatus == null) {
            lastFetchStatus = FeedFetchStatus.PENDING;
        }
        if (failureCount == null) {
            failureCount = 0;
        }
        if (category == null || category.isBlank()) {
            category = "tech";
        }
        if (featured == null) {
            featured = false;
        }
        if (updateFrequency == null || updateFrequency.isBlank()) {
            updateFrequency = "UNKNOWN";
        }
    }
}
