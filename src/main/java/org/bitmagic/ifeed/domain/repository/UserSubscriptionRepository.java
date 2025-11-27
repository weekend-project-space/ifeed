package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.model.UserSubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UserSubscriptionId>, JpaSpecificationExecutor<UserSubscription> {

    List<UserSubscription> findAllByUser(User user);

    List<UserSubscription> findAllByUserId(Integer userId);

    List<UserSubscription> findAllByUserIdAndActiveTrue(Integer userId);

    long countByUserAndActiveTrue(User user);

    Optional<UserSubscription> findByUserAndFeed(User user, Feed feed);

    long countByFeedAndActiveTrue(Feed feed);

    boolean existsByUser_IdAndFeedAndActiveTrue(Integer userId, Feed feed);

    @Query("select us.feed.id from UserSubscription us where us.user.id = :userId and us.active = true")
    List<Integer> findActiveFeedIdsByUserId(@Param("userId") Integer userId);

    @Query("""
        SELECT s.feed.id, COUNT(s.id)
        FROM UserSubscription s
        WHERE s.feed.id IN :feedIds
          AND s.active = true
        GROUP BY s.feed.id
        """)
    List<Object[]> countActiveSubscribersByFeedIds(@Param("feedIds") List<Integer> feedIds);
}
