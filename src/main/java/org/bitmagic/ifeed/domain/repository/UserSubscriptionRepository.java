package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository
    extends JpaRepository<UserSubscription, Long>, JpaSpecificationExecutor<UserSubscription> {

  List<UserSubscription> findAllByUser(User user);

  List<UserSubscription> findAllByUserId(Integer userId);

  List<UserSubscription> findAllByUserIdAndActiveTrue(Integer userId);

  long countByUserAndActiveTrue(User user);

  Optional<UserSubscription> findByUserIdAndSourceTypeAndSourceId(Integer userId, SourceType sourceType,
      Integer sourceId);

  long countBySourceTypeAndSourceIdAndActiveTrue(SourceType sourceType, Integer sourceId);

  boolean existsByUserIdAndSourceTypeAndSourceIdAndActiveTrue(Integer userId, SourceType sourceType, Integer sourceId);

  @Query("select us.sourceId from UserSubscription us where us.user.id = :userId and us.sourceType = 'FEED' and us.active = true")
  List<Integer> findActiveFeedIdsByUserId(@Param("userId") Integer userId);

  @Query("""
      SELECT s.sourceId, COUNT(s.id)
      FROM UserSubscription s
      WHERE s.sourceType = 'FEED'
        AND s.sourceId IN :feedIds
        AND s.active = true
      GROUP BY s.sourceId
      """)
  List<Object[]> countActiveSubscribersByFeedIds(@Param("feedIds") List<Integer> feedIds);

  // Discovery feature methods
  List<UserSubscription> findByUserIdAndSourceTypeAndActiveTrue(Integer userId, SourceType sourceType);

  @Query("""
      SELECT s.sourceId, COUNT(s.id)
      FROM UserSubscription s
      WHERE s.sourceType = :sourceType
        AND s.sourceId IN :sourceIds
        AND s.active = true
      GROUP BY s.sourceId
      """)
  List<Object[]> countBySourceTypeAndSourceIdIn(@Param("sourceType") SourceType sourceType,
      @Param("sourceIds") List<Integer> sourceIds);
}
