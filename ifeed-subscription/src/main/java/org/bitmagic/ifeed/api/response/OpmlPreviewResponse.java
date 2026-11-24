package org.bitmagic.ifeed.api.response;

import java.util.List;

public record OpmlPreviewResponse(
        List<OpmlPreviewFeedResponse> feeds,
        List<String> warnings,
        int remainingQuota
) {
}
