package org.bitmagic.ifeed.api.response;

import java.util.List;

public record OpmlPreviewFeedResponse(
        String feedUrl,
        String title,
        String siteUrl,
        String avatar,
        boolean alreadySubscribed,
        List<String> errors
) {
}
