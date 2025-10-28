package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.entity.Feed;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.domain.entity.UserSubscription;
import org.bitmagic.ifeed.domain.entity.UserSubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UserSubscriptionId> {

    List<UserSubscription> findAllByUser(User user);

    List<UserSubscription> findAllByUserId(UUID userId);

    List<UserSubscription> findAllByUserAndActiveTrue(User user);

    long countByUserAndActiveTrue(User user);

    Optional<UserSubscription> findByUserAndFeed(User user, Feed feed);

    long countByFeedAndActiveTrue(Feed feed);

    boolean existsByUser_IdAndFeedAndActiveTrue(UUID userId, Feed feed);

    @Query("select us.feed.id from UserSubscription us where us.user.id = :userId and us.active = true")
    List<UUID> findActiveFeedIdsByUserId(@Param("userId") UUID userId);
}
