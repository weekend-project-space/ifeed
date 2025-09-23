package org.bitmagic.ifeed.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "link", nullable = false, columnDefinition = "text")
    private String link;

    @Column(name = "author", length = 255)
    private String author;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "pub_date", nullable = false)
    private Instant publishedAt;

    @Column(name = "enclosure", columnDefinition = "text")
    private String enclosure;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "tags", columnDefinition = "text")
    private String tags;

    @Column(name = "embedding", columnDefinition = "text")
    private String embedding;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
