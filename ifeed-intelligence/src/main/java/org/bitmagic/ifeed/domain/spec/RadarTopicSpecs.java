package org.bitmagic.ifeed.domain.spec;

import org.bitmagic.ifeed.domain.model.radar.RadarTopic;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author yangrd
 * @date 2025/11/8
 **/
public interface RadarTopicSpecs {

    static Specification<RadarTopic> snapshotId(String snapshotId) {
        return Spec.<RadarTopic>on().eq("snapshotId", snapshotId).build();
    }
}
