package org.bitmagic.ifeed.domain.spec;

import org.bitmagic.ifeed.domain.model.radar.RadarTopicArticle;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * @author yangrd
 * @date 2025/11/8
 **/
public interface RadarTopicArticleSpecs {

    static Specification<RadarTopicArticle> bySnapshotAndTopic(String snapshotId, UUID topicId) {
        return Spec.<RadarTopicArticle>on()
                .eq("snapshotId", snapshotId)
                .eq("topicId", topicId)
                .build();
    }
}
