package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author yangrd
 * @date 2025/12/15
 **/
public interface ArticleEnrichmentRepository extends JpaRepository<ArticleEnrichment, Long>, JpaSpecificationExecutor<ArticleEnrichment> {
}
