package org.bitmagic.ifeed.service.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationMaintenanceScheduler {

    private final ArticleSimilarityRebuildService articleSimilarityRebuildService;
    private final ArticleRecommendationService articleRecommendationService;

    @Scheduled(cron = "0 30 2 * * *", zone = "UTC")
    public void rebuildSimilarityMatrix() {
        try {
            articleSimilarityRebuildService.rebuildDailyMatrix();
        } catch (Exception ex) {
            log.warn("Failed to rebuild article similarity matrix", ex);
        }
    }

    @Scheduled(cron = "0 */30 * * * *", zone = "UTC")
    public void prewarmGlobalRecommendations() {
        try {
            articleRecommendationService.prewarmGlobalCache();
        } catch (Exception ex) {
            log.debug("Global recommendation prewarm failed", ex);
        }
    }
}
