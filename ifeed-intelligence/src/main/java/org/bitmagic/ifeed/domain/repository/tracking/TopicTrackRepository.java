package org.bitmagic.ifeed.domain.repository.tracking;

import org.bitmagic.ifeed.domain.model.tracking.TopicTrack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicTrackRepository extends JpaRepository<TopicTrack, Long> {
    long countByOwnerId(Integer ownerId);

    @Query("""
            select new org.bitmagic.ifeed.api.tracking.TopicTrackScanResponse(
                track.topicUid, track.configVersion, track.enabled,
                case when track.enabled = false then 'DISABLED'
                    when track.scanToken is not null and track.scanLeaseUntil <= current_timestamp then 'FAILED'
                    else track.scanStatus end,
                track.scanStartedAt, track.scanFinishedAt, track.scanError)
            from TopicTrack track where track.topicUid in :topicIds
            """)
    List<org.bitmagic.ifeed.api.tracking.TopicTrackScanResponse> scanStates(
            @org.springframework.data.repository.query.Param("topicIds") java.util.Collection<UUID> topicIds);
    Page<TopicTrack> findAllByOrderByUpdatedAtDesc(Pageable pageable);
    @Query("select track.id from TopicTrack track where track.enabled = true")
    List<Long> findEnabledTopicIds();
    Optional<TopicTrack> findByTopicUid(UUID topicUid);
    Optional<TopicTrack> findByNormalizedKeyword(String normalizedKeyword);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TopicTrack> findByTopicUidAndOwnerId(UUID topicUid, Integer ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select track from TopicTrack track where track.id = :id")
    Optional<TopicTrack> findForScan(@org.springframework.data.repository.query.Param("id") Long id);
}
