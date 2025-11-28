package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.MixFeed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MixFeedRepository extends JpaRepository<MixFeed, Integer> {

    Optional<MixFeed> findByUid(UUID uid);

    @Query("SELECT m FROM MixFeed m WHERE m.uid = :uid AND m.user.id = :userId")
    Optional<MixFeed> findByUidAndUserId(@Param("uid") UUID uid, @Param("userId") Integer userId);

    List<MixFeed> findByUserId(Integer userId);

    Page<MixFeed> findByIsPublicTrue(Pageable pageable);

    @Query("""
            SELECT m FROM MixFeed m
            WHERE m.isPublic = true
              AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<MixFeed> searchByQuery(@Param("query") String query, Pageable pageable);

    @Modifying
    @Query("UPDATE MixFeed m SET m.subscriberCount = m.subscriberCount + 1 WHERE m.id = :id")
    void incrementSubscriberCount(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE MixFeed m SET m.subscriberCount = m.subscriberCount - 1 WHERE m.id = :id AND m.subscriberCount > 0")
    void decrementSubscriberCount(@Param("id") Integer id);
}
