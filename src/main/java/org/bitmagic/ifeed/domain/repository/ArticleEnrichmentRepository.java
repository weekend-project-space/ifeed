package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author yangrd
 * @date 2025/12/15
 **/
public interface ArticleEnrichmentRepository extends JpaRepository<ArticleEnrichment, Long>, JpaSpecificationExecutor<ArticleEnrichment> {

    @Query(value = """
            select new org.bitmagic.ifeed.domain.model.ArticleEnrichment(
                e.id,
                e.rating,
                null ,
                null ,
                e.createdAt,
                e.updatedAt)
            from Article a
            right join ArticleEnrichment  e on e.id = a.id
            where 
                e.rating != 'D'
                and (:ownerId is null or exists (
                    select 1
                    from UserSubscription us
                    where us.sourceType = 'FEED'
                      and us.sourceId = a.feed.id
                      and us.user.id = :ownerId
                      and us.active = true
              ))
            """, countQuery = """
            select count(a)
            from Article a
            right join ArticleEnrichment  e on e.id = a.id
            where  
                 e.rating != 'D'
                and (:ownerId is null or exists (
                    select 1
                    from UserSubscription us
                    where us.sourceType = 'FEED'
                      and us.sourceId = a.feed.id
                      and us.user.id = :ownerId
                      and us.active = true
              ))
            """)
    Page<ArticleEnrichment> findByUser(
            @Param("ownerId") Integer ownerId,
            Pageable pageable);
}
