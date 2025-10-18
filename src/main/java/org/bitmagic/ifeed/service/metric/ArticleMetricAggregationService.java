package org.bitmagic.ifeed.service.metric;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.entity.ArticleDailyMetric;
import org.bitmagic.ifeed.domain.entity.id.ArticleDailyMetricId;
import org.bitmagic.ifeed.domain.repository.ArticleDailyMetricRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleMetricAggregationService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleDailyMetricRepository articleDailyMetricRepository;

    @Transactional
    public void aggregateDailyMetrics(LocalDate metricDate) {
        if (metricDate == null) {
            throw new IllegalArgumentException("metricDate must not be null");
        }

        var startInclusive = metricDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        var endExclusive = metricDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        var collectedCounts = new HashMap<UUID, LongAdder>();
        var readCounts = new HashMap<UUID, LongAdder>();

        var behaviors = userBehaviorRepository.findAll();
        if (CollectionUtils.isEmpty(behaviors)) {
            log.info("No user behavior documents found for metricDate {}", metricDate);
            return;
        }

        behaviors.forEach(document -> aggregateDocument(document, startInclusive, endExclusive, collectedCounts, readCounts));

        if (collectedCounts.isEmpty() && readCounts.isEmpty()) {
            log.info("No user behaviors matched metricDate {}", metricDate);
            return;
        }

        var existingMetrics = articleDailyMetricRepository.findByIdMetricDate(metricDate);
        var metricByArticleId = new HashMap<UUID, ArticleDailyMetric>(existingMetrics.size());
        existingMetrics.forEach(metric -> {
            metric.setCollectedCount(0);
            metric.setReadCount(0);
            metricByArticleId.put(metric.getId().getArticleId(), metric);
        });

        collectedCounts.forEach((articleId, counter) -> upsertMetric(metricDate, articleId, counter.longValue(), metricByArticleId, false));
        readCounts.forEach((articleId, counter) -> upsertMetric(metricDate, articleId, counter.longValue(), metricByArticleId, true));

        articleDailyMetricRepository.saveAll(metricByArticleId.values());
        log.info("Aggregated metrics for {} articles on {}", metricByArticleId.size(), metricDate);
    }

    private void aggregateDocument(UserBehaviorDocument document,
                                   Instant startInclusive,
                                   Instant endExclusive,
                                   Map<UUID, LongAdder> collectedCounts,
                                   Map<UUID, LongAdder> readCounts) {
        aggregateEntries(document.getCollections(), startInclusive, endExclusive, collectedCounts);
        aggregateEntries(document.getReadHistory(), startInclusive, endExclusive, readCounts);
    }

    private void aggregateEntries(List<UserBehaviorDocument.ArticleRef> entries,
                                  Instant startInclusive,
                                  Instant endExclusive,
                                  Map<UUID, LongAdder> counters) {
        if (CollectionUtils.isEmpty(entries)) {
            return;
        }

        entries.stream()
                .filter(entry -> entry != null && entry.getTimestamp() != null && entry.getArticleId() != null)
                .filter(entry -> !entry.getTimestamp().isBefore(startInclusive) && entry.getTimestamp().isBefore(endExclusive))
                .forEach(entry -> {
                    try {
                        var articleId = UUID.fromString(entry.getArticleId());
                        counters.computeIfAbsent(articleId, ignored -> new LongAdder()).increment();
                    } catch (IllegalArgumentException ex) {
                        log.debug("Skip aggregation because of invalid articleId '{}'", entry.getArticleId());
                    }
                });
    }

    private void upsertMetric(LocalDate metricDate,
                              UUID articleId,
                              long delta,
                              Map<UUID, ArticleDailyMetric> metricByArticleId,
                              boolean isRead) {
        if (delta <= 0) {
            return;
        }
        var metric = metricByArticleId.computeIfAbsent(articleId, id ->
                new ArticleDailyMetric(new ArticleDailyMetricId(id, metricDate))
        );
        if (metric.getId() == null) {
            metric.setId(new ArticleDailyMetricId(articleId, metricDate));
        }
        if (isRead) {
            metric.setReadCount(metric.getReadCount() + delta);
        } else {
            metric.setCollectedCount(metric.getCollectedCount() + delta);
        }
    }
}
