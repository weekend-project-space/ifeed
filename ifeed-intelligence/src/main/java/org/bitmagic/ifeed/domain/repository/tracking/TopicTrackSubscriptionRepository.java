package org.bitmagic.ifeed.domain.repository.tracking;

import org.bitmagic.ifeed.domain.model.tracking.TopicTrackSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TopicTrackSubscriptionRepository extends JpaRepository<TopicTrackSubscription, Long> {
    long countByUserId(Integer userId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            insert into topic_track_subscriptions(topic_track_id, user_id, created_at)
            values (:topicId, :userId, current_timestamp)
            on conflict (topic_track_id, user_id) do nothing
            """, nativeQuery = true)
    int subscribe(@Param("topicId") Long topicId, @Param("userId") Integer userId);
    Optional<TopicTrackSubscription> findByTopicTrack_IdAndUserId(Long topicTrackId, Integer userId);
    boolean existsByTopicTrack_IdAndUserId(Long topicTrackId, Integer userId);
    void deleteByTopicTrack_IdAndUserId(Long topicTrackId, Integer userId);

    @Query("select subscription.topicTrack.id from TopicTrackSubscription subscription where subscription.userId = :userId and subscription.topicTrack.id in :topicIds")
    java.util.List<Long> findSubscribedTopicIds(@Param("userId") Integer userId, @Param("topicIds") java.util.Collection<Long> topicIds);

    @Query("select subscription.topicTrack from TopicTrackSubscription subscription where subscription.userId = :userId")
    Page<org.bitmagic.ifeed.domain.model.tracking.TopicTrack> findTopicsByUserId(@Param("userId") Integer userId, Pageable pageable);
}
