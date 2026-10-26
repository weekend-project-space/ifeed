package org.bitmagic.ifeed.api.tracking;

import java.time.Instant;
import java.util.UUID;

public record TopicTrackScanResponse(UUID topicId, int configVersion, boolean enabled, String scanStatus,
                                     Instant scanStartedAt, Instant scanFinishedAt, String scanError) {}
