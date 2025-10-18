package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.entity.ArticleSimilarity;
import org.bitmagic.ifeed.domain.entity.id.ArticleSimilarityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ArticleSimilarityRepository extends JpaRepository<ArticleSimilarity, ArticleSimilarityId> {

    List<ArticleSimilarity> findByIdArticleIdIn(Collection<UUID> articleIds);

    List<ArticleSimilarity> findByIdArticleId(UUID articleId);
}
