package org.bitmagic.ifeed.service.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.RecommendationProperties;
import org.bitmagic.ifeed.domain.entity.ArticleSimilarity;
import org.bitmagic.ifeed.domain.entity.id.ArticleSimilarityId;
import org.bitmagic.ifeed.domain.repository.ArticleDailyMetricRepository;
import org.bitmagic.ifeed.domain.repository.ArticleSimilarityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSimilarityRebuildService {

    private final ArticleDailyMetricRepository articleDailyMetricRepository;
    private final ArticleSimilarityRepository articleSimilarityRepository;
    private final RecommendationProperties recommendationProperties;

    @Transactional
    public void rebuildDailyMatrix() {
        var windowDays = Math.max(1, recommendationProperties.getCollaborativeMetricWindowDays());
        var end = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        var start = end.minusDays(windowDays - 1L);
        rebuildMatrixBetween(start, end);
    }

    @Transactional
    public void rebuildMatrixBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Invalid similarity rebuild window");
        }
        log.info("Rebuilding article similarity matrix for window {} - {}", start, end);
        var metrics = articleDailyMetricRepository.findByIdMetricDateBetween(start, end);
        if (CollectionUtils.isEmpty(metrics)) {
            articleSimilarityRepository.deleteAllInBatch();
            log.info("No metrics available for similarity rebuild; cleared matrix");
            return;
        }

        var dimension = (int) Duration.between(start, end).toDays() + 1;
        var indexByDate = new HashMap<LocalDate, Integer>(dimension);
        var vectors = new HashMap<UUID, MetricVector>();

        metrics.forEach(metric -> {
            var metricDate = metric.getId().getMetricDate();
            var index = indexByDate.computeIfAbsent(metricDate,
                    date -> (int) Duration.between(start, date).toDays());
            if (index < 0 || index >= dimension) {
                return;
            }
            var vector = vectors.computeIfAbsent(metric.getId().getArticleId(),
                    id -> new MetricVector(dimension));
            vector.add(index, metric.getCollectedCount());
        });

        if (vectors.isEmpty()) {
            articleSimilarityRepository.deleteAllInBatch();
            log.info("No vectors built from metrics; cleared matrix");
            return;
        }

        var candidates = new ArrayList<ArticleSimilarity>();
        int topK = Math.max(1, recommendationProperties.getSimilarityTopK());
        var articleIds = new ArrayList<>(vectors.keySet());

        for (UUID anchorId : articleIds) {
            var anchorVector = vectors.get(anchorId);
            var anchorNorm = anchorVector.norm();
            if (anchorNorm == 0) {
                continue;
            }
            var queue = new PriorityQueue<SimilarityCandidate>(Comparator.comparingDouble(SimilarityCandidate::similarity));
            for (UUID otherId : articleIds) {
                if (anchorId.equals(otherId)) {
                    continue;
                }
                var otherVector = vectors.get(otherId);
                var similarity = cosineSimilarity(anchorVector.values(), anchorNorm, otherVector.values(), otherVector.norm());
                if (similarity <= 0) {
                    continue;
                }
                queue.offer(new SimilarityCandidate(otherId, similarity));
                if (queue.size() > topK) {
                    queue.poll();
                }
            }
            if (queue.isEmpty()) {
                continue;
            }
            var sorted = new ArrayList<SimilarityCandidate>(queue);
            sorted.sort(Comparator.comparingDouble(SimilarityCandidate::similarity).reversed());
            sorted.forEach(candidate -> candidates.add(
                    new ArticleSimilarity(new ArticleSimilarityId(anchorId, candidate.articleId()), candidate.similarity())));
        }

        articleSimilarityRepository.deleteAllInBatch();
        if (!candidates.isEmpty()) {
            articleSimilarityRepository.saveAll(candidates);
        }
        log.info("Rebuilt article similarity matrix with {} records", candidates.size());
    }

    private double cosineSimilarity(double[] left,
                                    double normLeft,
                                    double[] right,
                                    double normRight) {
        double dot = 0;
        for (int i = 0; i < left.length && i < right.length; i++) {
            dot += left[i] * right[i];
        }
        if (normLeft == 0 || normRight == 0) {
            return 0;
        }
        return dot / (normLeft * normRight);
    }

    private static final class MetricVector {
        private final double[] values;
        private Double norm;

        private MetricVector(int dimension) {
            this.values = new double[dimension];
        }

        void add(int index, long collected) {
            values[index] = collected;
            norm = null;
        }

        double[] values() {
            return values;
        }

        double norm() {
            if (norm == null) {
                double sum = 0;
                for (double value : values) {
                    sum += value * value;
                }
                norm = Math.sqrt(sum);
            }
            return norm;
        }
    }

    private record SimilarityCandidate(UUID articleId, double similarity) {
    }
}
