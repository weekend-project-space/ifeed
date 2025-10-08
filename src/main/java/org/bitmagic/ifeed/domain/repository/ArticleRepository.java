package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.entity.Feed;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    List<Article> findByFeed(Feed feed);

    boolean existsByLink(String link);

    @Query(value = """
            select new org.bitmagic.ifeed.domain.projection.ArticleSummaryView(
                a.id,
                a.title,
                a.link,
                a.summary,
                f.title,
                a.publishedAt,
                a.tags,
                a.thumbnail,
                a.enclosure)
            from Article a
            left join a.feed f
            where (:feedId is null or f.id = :feedId)
              and (:tagPattern is null or lower(coalesce(a.tags, '')) like :tagPattern)
              and (:ownerId is null or exists (
                    select 1
                    from UserSubscription us
                    where us.feed = f
                      and us.user.id = :ownerId
                      and us.active = true
              ))
            """,
            countQuery = """
                    select count(a)
                    from Article a
                    left join a.feed f
                    where (:feedId is null or f.id = :feedId)
                      and (:tagPattern is null or lower(coalesce(a.tags, '')) like :tagPattern)
                      and (:ownerId is null or exists (
                            select 1
                            from UserSubscription us
                            where us.feed = f
                              and us.user.id = :ownerId
                              and us.active = true
                      ))
                    """)
    Page<ArticleSummaryView> findArticleSummaries(@Param("feedId") UUID feedId,
                                                  @Param("tagPattern") String tagPattern,
                                                  @Param("ownerId") UUID ownerId,
                                                  Pageable pageable);

    @Query(value = """
            select new org.bitmagic.ifeed.domain.projection.ArticleSummaryView(
                a.id,
                a.title,
                a.link,
                a.summary,
                f.title,
                a.publishedAt,
                a.tags,
                a.thumbnail,
                a.enclosure)
            from Article a
            left join a.feed f
            where (lower(a.title) like :term
               or lower(a.summary) like :term)
              and (:ownerId is null or exists (
                    select 1
                    from UserSubscription us
                    where us.feed = f
                      and us.user.id = :ownerId
                      and us.active = true
              ))
            """,
            countQuery = """
                    select count(a)
                    from Article a
                    where (lower(a.title) like :term
                       or lower(a.summary) like :term)
                      and (:ownerId is null or exists (
                            select 1
                            from UserSubscription us
                            where us.feed = a.feed
                              and us.user.id = :ownerId
                              and us.active = true
                      ))
                    """)
    Page<ArticleSummaryView> searchArticleSummaries(@Param("term") String term,
                                                    @Param("ownerId") UUID ownerId,
                                                    Pageable pageable);

    List<Article> findByIdIn(Iterable<UUID> ids);

    long countByFeed(Feed feed);

    Optional<Article> findTopByFeedOrderByPublishedAtDesc(Feed feed);
}
