package org.bitmagic.ifeed.domain.spec;

import org.bitmagic.ifeed.domain.model.radar.RadarSnapshot;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * @author yangrd
 * @date 2025/11/8
 **/
public interface RadarSnapshotSpecs {

    static Specification<RadarSnapshot> active(Instant now, Integer windowHours) {
        return Spec.<RadarSnapshot>on()
                .gt("expiresAt", now)
                .when(windowHours != null, b -> b.eq("windowHours", windowHours))
                .build();
    }
}
