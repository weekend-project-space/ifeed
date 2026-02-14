package org.bitmagic.ifeed.domain.repository.radar;

import org.bitmagic.ifeed.domain.model.radar.RadarSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RadarSnapshotRepository extends JpaRepository<RadarSnapshot, String>, JpaSpecificationExecutor<RadarSnapshot> {
}
