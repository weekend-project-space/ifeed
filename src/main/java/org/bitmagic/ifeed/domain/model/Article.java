package org.bitmagic.ifeed.domain.model;

import jakarta.persistence.*;
import lombok.*;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "uid", nullable = false, updatable = false, unique = true)
    private UUID uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "link", nullable = false, columnDefinition = "text")
    private String link;

    @Column(name = "author")
    private String author;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "pub_date", nullable = false)
    private Instant publishedAt;

    @Column(name = "enclosure", columnDefinition = "text")
    private String enclosure;

    @Column(name = "enclosure_type", length = 16)
    private String enclosureType;

    @Column(name = "thumbnail", columnDefinition = "text")
    private String thumbnail;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "tags", columnDefinition = "text")
    private String tags;

    private Boolean embeddingGenerated;

    private Boolean aiGenerated;

    @PrePersist
    void onCreate() {
        if (uid == null) {
            uid = UUID.randomUUID();
            embeddingGenerated = false;
            aiGenerated = false;
        }
    }
}
