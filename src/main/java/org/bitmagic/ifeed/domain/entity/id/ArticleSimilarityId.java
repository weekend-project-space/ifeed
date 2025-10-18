package org.bitmagic.ifeed.domain.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ArticleSimilarityId implements Serializable {

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "similar_article_id", nullable = false)
    private UUID similarArticleId;
}
