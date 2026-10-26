package org.bitmagic.ifeed.domain.repository.tracking;

import org.bitmagic.ifeed.domain.model.tracking.TopicTrackSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicTrackSummaryRepository extends JpaRepository<TopicTrackSummary, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            insert into topic_track_summary_articles(summary_id, article_id)
            select :summaryId, article.id from articles article where article.id in (:articleIds)
            on conflict do nothing
            """, nativeQuery = true)
    int linkArticles(@Param("summaryId") Long summaryId, @Param("articleIds") java.util.Collection<Long> articleIds);
    @Query("""
            select summary.id as id, summary.summaryUid as summaryUid, summary.topicTrack.topicUid as topicUid,
                   summary.topicName as topicName, summary.keyword as keyword, summary.summary as summary,
                   summary.articleCount as articleCount, summary.createdAt as createdAt, summary.titlesOnly as titlesOnly
            from TopicTrackSummary summary
            where (:topicUid is null or summary.topicTrack.topicUid = :topicUid)
            and summary.configVersion = summary.topicTrack.configVersion
            and (:userId is null or exists (select subscription.id from TopicTrackSubscription subscription
                where subscription.topicTrack = summary.topicTrack and subscription.userId = :userId))
            and (summary.createdAt < coalesce(:before, summary.createdAt)
                or summary.createdAt = coalesce(:before, summary.createdAt)
                   and summary.id < coalesce(:beforeId, summary.id + 1))
            order by summary.createdAt desc, summary.id desc
            """)
    java.util.List<SummaryRow> findSummaries(@Param("topicUid") java.util.UUID topicUid,
                                           @Param("userId") Integer userId,
                                           @Param("before") java.time.Instant before,
                                           @Param("beforeId") Long beforeId, Pageable pageable);

    @Query("""
            select summary.id as id, summary.summaryUid as summaryUid, summary.topicTrack.topicUid as topicUid,
                   summary.topicName as topicName, summary.keyword as keyword, summary.summary as summary,
                   summary.articleCount as articleCount, summary.createdAt as createdAt, summary.titlesOnly as titlesOnly
            from TopicTrackSummary summary
            where summary.summaryUid = :summaryUid and summary.configVersion = summary.topicTrack.configVersion
            """)
    java.util.Optional<SummaryRow> findActiveSummary(@Param("summaryUid") java.util.UUID summaryUid);

    @Query(value = """
            select article.id as id, article.uid as "articleId", article.title as title, article.link as url,
                   coalesce(feed.title, '') as "feedName", feed.icon as "feedIcon", article.pub_date as "publishedAt"
            from topic_track_summary_articles association
            join articles article on article.id = association.article_id
            left join feeds feed on feed.id = article.feed_id
            where association.summary_id = :summaryId and article.id > :afterId
            order by article.id asc
            """, nativeQuery = true)
    java.util.List<ArticleRow> findArticles(@Param("summaryId") Long summaryId, @Param("afterId") Long afterId,
                                            Pageable pageable);

    @Query(value = """
            select article.id from topic_track_summary_articles association
            join articles article on article.id = association.article_id
            where association.summary_id = :summaryId and article.uid = :articleUid
            """, nativeQuery = true)
    java.util.Optional<Long> findArticleCursor(@Param("summaryId") Long summaryId,
                                             @Param("articleUid") java.util.UUID articleUid);

    interface SummaryRow {
        Long getId();
        java.util.UUID getSummaryUid();
        java.util.UUID getTopicUid();
        String getTopicName();
        String getKeyword();
        String getSummary();
        int getArticleCount();
        java.time.Instant getCreatedAt();
        boolean getTitlesOnly();
    }

    interface ArticleRow {
        Long getId();
        java.util.UUID getArticleId();
        String getTitle();
        String getUrl();
        String getFeedName();
        String getFeedIcon();
        java.time.Instant getPublishedAt();
    }
}
