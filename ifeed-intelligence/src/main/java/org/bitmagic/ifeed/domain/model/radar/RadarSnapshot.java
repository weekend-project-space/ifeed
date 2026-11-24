package org.bitmagic.ifeed.domain.model.radar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "radar_snapshot")
public class RadarSnapshot {

    @Id
    @Column(name = "snapshot_id", nullable = false, updatable = false)
    private String snapshotId;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "window_hours", nullable = false)
    private Integer windowHours;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}

