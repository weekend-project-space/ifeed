package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.entity.Feed;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
              and (:category is null or lower(coalesce(a.category, '')) = :category)
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
                      and (:category is null or lower(coalesce(a.category, '')) = :category)
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
                                                  @Param("category") String category,
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
               or lower(a.summary) like :term
               or lower(a.category) like :term
               or lower(a.tags) like :term)
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
                       or lower(a.summary) like :term
                       or lower(a.category) like :term
                       or lower(a.tags) like :term)
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

    /**
     * Count categories for articles visible to the owner (their active subscriptions), within time window.
     */
    @Query("""
            select a.category as category, count(a) as cnt
            from Article a
            where a.publishedAt between :fromTs and :toTs
              and (:ownerId is null or exists (
                    select 1 from UserSubscription us
                    where us.feed = a.feed and us.user.id = :ownerId and us.active = true
              ))
              and (coalesce(a.category, '') <> '')
            group by a.category
            order by cnt desc
            """)
    List<Object[]> countCategoriesForOwnerWithin(@Param("ownerId") UUID ownerId,
                                                 @Param("fromTs") Instant fromTs,
                                                 @Param("toTs") Instant toTs);

    /**
     * Fetch raw tag JSON strings for later in-memory aggregation, for articles visible to owner within window.
     */
    @Query("""
            select a.tags
            from Article a
            where a.publishedAt between :fromTs and :toTs
              and (:ownerId is null or exists (
                    select 1 from UserSubscription us
                    where us.feed = a.feed and us.user.id = :ownerId and us.active = true
              ))
              and (coalesce(a.tags, '') <> '')
            """)
    List<String> findTagJsonForOwnerWithin(@Param("ownerId") UUID ownerId,
                                           @Param("fromTs") Instant fromTs,
                                           @Param("toTs") Instant toTs);
}
