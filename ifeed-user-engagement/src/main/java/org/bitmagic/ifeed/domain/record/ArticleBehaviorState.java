package org.bitmagic.ifeed.domain.record;

import java.util.UUID;

public record ArticleBehaviorState(boolean collected, boolean liked, UUID folderId) {
    public static final ArticleBehaviorState EMPTY = new ArticleBehaviorState(false, false, null);
}
