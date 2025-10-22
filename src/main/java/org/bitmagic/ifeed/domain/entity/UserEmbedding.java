package org.bitmagic.ifeed.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.bitmagic.ifeed.domain.converter.FloatArrayConverter;

import java.time.Instant;
import java.util.UUID;

/**
 * @author yangrd
 * @date 2025/10/21
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_embeddings")
public class UserEmbedding {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Convert(converter = FloatArrayConverter.class)
    @Column(name = "embedding", columnDefinition = "vector(1024)", nullable = false)
    private float[] embedding;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touchTimestamp() {
        this.updatedAt = Instant.now();
    }

}
