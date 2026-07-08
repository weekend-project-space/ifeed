package org.bitmagic.ifeed.application.radar;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.model.radar.RadarSnapshot;
import org.bitmagic.ifeed.domain.model.radar.RadarTopic;
import org.bitmagic.ifeed.domain.model.radar.RadarTopicArticle;
import org.bitmagic.ifeed.domain.repository.radar.RadarAdminRepository;
import org.bitmagic.ifeed.domain.repository.radar.RadarSnapshotRepository;
import org.bitmagic.ifeed.domain.repository.radar.RadarTopicArticleRepository;
import org.bitmagic.ifeed.domain.repository.radar.RadarTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles the transactional persistence of a fully-built radar snapshot.
 *
 * <p>Keeping the write transaction separate from the scheduler's external IO
 * (vector search, AI naming) prevents holding a database connection while
 * waiting on network calls and ensures that an IO failure cannot roll back
 * an otherwise valid snapshot.
 */
@Service
@RequiredArgsConstructor
public class RadarSnapshotSaveService {

    private final RadarAdminRepository radarAdminRepository;
    private final RadarSnapshotRepository snapshotRepository;
    private final RadarTopicRepository topicRepository;
    private final RadarTopicArticleRepository topicArticleRepository;

    @Transactional
    public void save(RadarSnapshot snapshot, List<RadarTopic> topics, List<RadarTopicArticle> articles) {
        radarAdminRepository.deleteExpiredSnapshots();
        snapshotRepository.save(snapshot);
        topicRepository.saveAll(topics);
        topicArticleRepository.saveAll(articles);
    }
}
