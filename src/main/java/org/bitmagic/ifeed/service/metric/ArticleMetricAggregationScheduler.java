package org.bitmagic.ifeed.service.metric;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleMetricAggregationScheduler {

    private final ArticleMetricAggregationService articleMetricAggregationService;

    @Scheduled(cron = "0 5 1 * * *", zone = "UTC")
    public void aggregatePreviousDayMetrics() {
        var targetDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        try {
            articleMetricAggregationService.aggregateDailyMetrics(targetDate);
        } catch (Exception ex) {
            log.warn("Failed to aggregate article metrics for {}", targetDate, ex);
        }
    }

    @Scheduled(cron = "0 10 * * * *", zone = "UTC")
    public void aggregateCurrentDayHourly() {
        var today = LocalDate.now(ZoneOffset.UTC);
        try {
            articleMetricAggregationService.aggregateDailyMetrics(today);
        } catch (Exception ex) {
            log.warn("Failed to aggregate hourly article metrics for {}", today, ex);
        }
    }
}
