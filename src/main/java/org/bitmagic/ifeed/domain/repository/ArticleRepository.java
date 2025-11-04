package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, Integer>, JpaSpecificationExecutor<Article> {

    boolean existsByFeedAndLink(Feed feed, String link);

    @Query(value = """
            select new org.bitmagic.ifeed.domain.record.ArticleSummaryView(
                a.uid,
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
            where (:feedUid is null or f.uid = :feedUid)
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
                    where (:feedUid is null or f.uid = :feedUid)
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
    Page<ArticleSummaryView> findArticleSummaries(@Param("feedUid") UUID feedUid,
                                                  @Param("tagPattern") String tagPattern,
                                                  @Param("category") String category,
                                                  @Param("ownerId") Integer ownerId,
                                                  Pageable pageable);

    @Query(value = """
            select new org.bitmagic.ifeed.domain.record.ArticleSummaryView(
                a.uid,
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
               or lower(a.author) like :term
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
                                                    @Param("ownerId") Integer ownerId,
                                                    Pageable pageable);

    List<Article> findByUidIn(Iterable<UUID> uids);

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
    List<Object[]> countCategoriesForOwnerWithin(@Param("ownerId") Integer ownerId,
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
    List<String> findTagJsonForOwnerWithin(@Param("ownerId") Integer ownerId,
                                           @Param("fromTs") Instant fromTs,
                                           @Param("toTs") Instant toTs);


    @Query(value = """
            select new org.bitmagic.ifeed.domain.record.ArticleSummaryView(
                a.uid,
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
            where a.id in (:ids)
            """)
    List<ArticleSummaryView> findArticleSummariesByIds(@Param("ids") Collection<Long> ids);

    @Query("select a.uid from Article a where a.id = :articleId")
    Optional<UUID> findUidByArticleId(@Param("articleId") Long articleId);

    @Query("select a.id from Article a where lower(coalesce(a.category, '')) = lower(:category) order by a.publishedAt desc")
    List<Long> findTopIdsByCategory(@Param("category") String category, Pageable pageable);

    @Query("select a.id from Article a where lower(coalesce(a.author, '')) = lower(:author) order by a.publishedAt desc")
    List<Long> findTopIdsByAuthor(@Param("author") String author, Pageable pageable);

    @Query("select a.id, a.publishedAt from Article a where a.id in (:ids)")
    List<Object[]> findPublishedAtByIdIn(@Param("ids") Collection<Long> ids);
}
