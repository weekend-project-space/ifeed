package org.bitmagic.ifeed.domain.repository.tracking;

import org.bitmagic.ifeed.domain.model.tracking.TopicTrackMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TopicTrackMatchRepository extends JpaRepository<TopicTrackMatch, Long> {
    List<TopicTrackMatch> findAllByTopicTrack_IdAndConfigVersionAndArticleIdIn(Long topicTrackId, int configVersion,
                                                                                 Collection<Long> articleIds);
}
