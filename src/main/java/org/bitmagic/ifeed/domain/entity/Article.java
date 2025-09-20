package org.bitmagic.ifeed.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "articles")
public class Article {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Lob
    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "link", nullable = false)
    private String link;

    @Column(name = "author", length = 255)
    private String author;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "pub_date", nullable = false)
    private Instant publishedAt;

    @Lob
    @Column(name = "enclosure")
    private String enclosure;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Lob
    @Column(name = "summary")
    private String summary;

    @Column(name = "category", length = 50)
    private String category;

    @Lob
    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    @Lob
    @Column(name = "embedding")
    private String embedding;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
