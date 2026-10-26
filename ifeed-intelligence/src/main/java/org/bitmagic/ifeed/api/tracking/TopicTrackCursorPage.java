package org.bitmagic.ifeed.api.tracking;

import java.util.List;
import java.util.UUID;

public record TopicTrackCursorPage<T>(List<T> content, UUID nextCursor, boolean hasNext) {}
