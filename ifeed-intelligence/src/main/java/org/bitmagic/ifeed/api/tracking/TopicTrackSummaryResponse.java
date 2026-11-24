package org.bitmagic.ifeed.api.tracking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopicTrackSummaryResponse(
        UUID summaryId,
        UUID topicId,
        String topicName,
        String keyword,
        String summary,
        int articleCount,
        List<ArticlePreview> articles,
        Instant createdAt,
        boolean titlesOnly
) {
    public record ArticlePreview(UUID articleId, String title, String url, String feedName,
                                 String feedIcon, Instant publishedAt) {
    }
}
