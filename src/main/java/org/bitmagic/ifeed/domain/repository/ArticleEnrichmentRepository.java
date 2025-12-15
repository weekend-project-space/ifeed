package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author yangrd
 * @date 2025/12/15
 **/
public interface ArticleEnrichmentRepository extends JpaRepository<ArticleEnrichment, Long> {
}
