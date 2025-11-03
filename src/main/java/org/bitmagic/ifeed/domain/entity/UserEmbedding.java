package org.bitmagic.ifeed.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private Integer userId;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1024)
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
