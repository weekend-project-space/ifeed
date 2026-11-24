package org.bitmagic.ifeed.domain.repository.radar;

import org.bitmagic.ifeed.domain.model.radar.RadarTopicArticle;
import org.bitmagic.ifeed.domain.model.radar.RadarTopicArticleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RadarTopicArticleRepository extends JpaRepository<RadarTopicArticle, RadarTopicArticleId>, JpaSpecificationExecutor<RadarTopicArticle> {
}
