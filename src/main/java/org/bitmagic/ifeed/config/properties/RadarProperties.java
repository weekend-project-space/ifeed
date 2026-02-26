package org.bitmagic.ifeed.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "app.radar")
public class RadarProperties {

    /** How often to rebuild radar snapshot. */
    private Duration fixedDelay = Duration.ofHours(1);

    /** Initial delay after application start. */
    private Duration initialDelay = Duration.ofMinutes(1);

    /** Rolling window for topic generation. */
    private int windowHours = 24;

    /** Minimum similarity to connect two docs into the same topic cluster. */
    private double similarityThreshold = 0.82;

    /** Per-document nearest neighbors considered when building similarity graph. */
    private int neighbors = 10;

    /** Ignore clusters smaller than this size. */
    private int minClusterSize = 5;

    /** Cap number of topics stored per snapshot. */
    private int maxTopics = 12;

    /** Snapshot TTL. */
    private Duration snapshotTtl = Duration.ofHours(24);
}

