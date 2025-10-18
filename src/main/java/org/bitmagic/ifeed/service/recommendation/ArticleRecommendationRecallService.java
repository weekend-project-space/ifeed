package org.bitmagic.ifeed.service.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.RecommendationProperties;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.repository.ArticleDailyMetricRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.ArticleSimilarityRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleRecommendationRecallService {

    private static final TypeReference<List<Double>> EMBEDDING_TYPE = new TypeReference<>() {
    };

    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleRepository articleRepository;
    private final ArticleDailyMetricRepository articleDailyMetricRepository;
    private final ArticleSimilarityRepository articleSimilarityRepository;
    private final RecommendationProperties recommendationProperties;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public RecallCandidates recall(UUID userId) {
        // Combine ItemCF, content similarity, and popularity to prepare recall pools for downstream ranking.
        if (userId == null) {
            return RecallCandidates.empty();
        }

        var document = userBehaviorRepository.findById(userId.toString()).orElse(null);
        if (document == null) {
            return RecallCandidates.empty();
        }

        var interactionContext = buildInteractionContext(document);
        log.debug("Building recall candidates for userId={} recentCount={} exclusionSize={}",
                userId, interactionContext.recentArticleIds().size(), interactionContext.exclusionSet().size());

        if (interactionContext.recentArticleIds().isEmpty()) {
            var popularity = recallByPopularity(interactionContext.exclusionSet());
            return new RecallCandidates(List.of(), List.of(), popularity);
        }

        var collaborative = recallByCollaborativeFiltering(interactionContext.recentArticleIds(), interactionContext.exclusionSet());
        var content = recallByContentSimilarity(interactionContext.recentArticleIds(), interactionContext.exclusionSet());
        var popularity = recallByPopularity(interactionContext.exclusionSet());

        return new RecallCandidates(collaborative, content, popularity);
    }

    @Transactional(readOnly = true)
    public RecallCandidates recallGlobal() {
        var popularity = recallByPopularity(Set.of());
        return new RecallCandidates(List.of(), List.of(), popularity);
    }

    private List<ArticleRecallCandidate> recallByCollaborativeFiltering(List<UUID> anchorArticleIds,
                                                                        Set<UUID> exclude) {
        // ItemCF: treat the daily collected counts as vectors and compute cosine similarity.
        if (CollectionUtils.isEmpty(anchorArticleIds)) {
            return List.of();
        }

        var precomputed = recallUsingSimilarityMatrix(anchorArticleIds, exclude);
        if (!precomputed.isEmpty()) {
            log.debug("Collaborative recall used precomputed matrix with {} anchors", anchorArticleIds.size());
            return precomputed;
        }

        var window = loadMetricWindow(Math.max(1, recommendationProperties.getCollaborativeMetricWindowDays()));
        if (window.seriesByArticle().isEmpty()) {
            return List.of();
        }

        var scores = new HashMap<UUID, Double>();
        anchorArticleIds.stream()
                .map(window.seriesByArticle()::get)
                .filter(Objects::nonNull)
                .filter(series -> series.collectedNorm() > 0)
                .forEach(anchorSeries -> accumulateCollaborativeScores(anchorSeries,
                        window.seriesByArticle(),
                        exclude,
                        scores));

        if (scores.isEmpty()) {
            return List.of();
        }

        var candidates = scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(recommendationProperties.getCollaborativeCandidateLimit())
                .map(entry -> new ArticleRecallCandidate(entry.getKey(), entry.getValue(), RecallSource.COLLABORATIVE))
                .toList();

        log.debug("Collaborative recall produced {} candidates", candidates.size());
        return candidates;
    }

    private void accumulateCollaborativeScores(MetricSeries anchorSeries,
                                               Map<UUID, MetricSeries> seriesByArticle,
                                               Set<UUID> exclude,
                                               Map<UUID, Double> scores) {
        var anchorId = anchorSeries.articleId();
        var anchorVector = anchorSeries.collected();
        var anchorNorm = anchorSeries.collectedNorm();

        seriesByArticle.forEach((articleId, candidateSeries) -> {
            if (articleId.equals(anchorId) || exclude.contains(articleId)) {
                return;
            }
            var candidateNorm = candidateSeries.collectedNorm();
            if (candidateNorm == 0) {
                return;
            }
            var similarity = cosineSimilarity(anchorVector, anchorNorm, candidateSeries.collected(), candidateNorm);
            if (similarity <= 0) {
                return;
            }
            scores.merge(articleId, similarity, Double::sum);
        });
    }

    private List<ArticleRecallCandidate> recallByContentSimilarity(List<UUID> anchorArticleIds,
                                                                   Set<UUID> exclude) {
        // Content-based: average embeddings from anchor articles and query pgvector via Spring AI.
        if (CollectionUtils.isEmpty(anchorArticleIds)) {
            return List.of();
        }

        var anchorArticles = articleRepository.findByIdIn(anchorArticleIds);
        if (CollectionUtils.isEmpty(anchorArticles)) {
            return List.of();
        }

        var userEmbedding = buildUserEmbedding(anchorArticles);
        if (userEmbedding.isEmpty()) {
            return List.of();
        }
//TODO
        var searchLimit = recommendationProperties.getContentCandidateLimit()
                + anchorArticleIds.size();
        var request = SearchRequest.builder()
                .topK(searchLimit)
//                .query(userEmbedding.get())
                .build();

        List<Document> documents;
        try {
            documents = vectorStore.similaritySearch(request);
        } catch (Exception ex) {
            log.warn("VectorStore similarity search failed", ex);
            return List.of();
        }

        if (CollectionUtils.isEmpty(documents)) {
            return List.of();
        }

        var candidates = new ArrayList<ArticleRecallCandidate>();
        for (Document document : documents) {
            var articleId = extractArticleId(document);
            if (articleId.isEmpty() || exclude.contains(articleId.get())) {
                continue;
            }
            var score = extractSimilarityScore(document);
            candidates.add(new ArticleRecallCandidate(articleId.get(), score, RecallSource.CONTENT));
            if (candidates.size() >= recommendationProperties.getContentCandidateLimit()) {
                break;
            }
        }
        log.debug("Content recall produced {} candidates", candidates.size());
        return candidates;
    }

    private List<ArticleRecallCandidate> recallUsingSimilarityMatrix(List<UUID> anchorArticleIds, Set<UUID> exclude) {
        var similarities = articleSimilarityRepository.findByIdArticleIdIn(anchorArticleIds);
        if (CollectionUtils.isEmpty(similarities)) {
            return List.of();
        }
        var scores = new HashMap<UUID, Double>();
        for (var similarity : similarities) {
            var similarId = similarity.getId().getSimilarArticleId();
            if (anchorArticleIds.contains(similarId) || exclude.contains(similarId)) {
                continue;
            }
            scores.merge(similarId, similarity.getSimilarity(), Double::sum);
        }
        if (scores.isEmpty()) {
            return List.of();
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(recommendationProperties.getCollaborativeCandidateLimit())
                .map(entry -> new ArticleRecallCandidate(entry.getKey(), entry.getValue(), RecallSource.COLLABORATIVE))
                .toList();
    }

    private List<ArticleRecallCandidate> recallByPopularity(Set<UUID> exclude) {
        // Popularity-based: leverage aggregated counts and publication recency for a warm-start pool.
        var window = loadMetricWindow(Math.max(1, recommendationProperties.getPopularityWindowDays()));
        if (window.seriesByArticle().isEmpty()) {
            return List.of();
        }

        var articleIds = window.seriesByArticle().keySet().stream()
                .filter(id -> !exclude.contains(id))
                .toList();

        if (articleIds.isEmpty()) {
            return List.of();
        }

        var articles = articleRepository.findByIdIn(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, article -> article));

        if (articles.isEmpty()) {
            return List.of();
        }

        var now = Instant.now();
        var halfLife = Math.max(0.1, recommendationProperties.getPopularityRecencyHalfLifeDays());

        var candidates = articleIds.stream()
                .map(articleId -> {
                    var article = articles.get(articleId);
                    if (article == null) {
                        return null;
                    }
                    var series = window.seriesByArticle().get(articleId);
                    if (series == null) {
                        return null;
                    }
                    var recency = computeRecencyFactor(article.getPublishedAt(), now, halfLife);
                    var score = recommendationProperties.getPopularityWeightCollected() * series.collectedSum()
                            + recommendationProperties.getPopularityWeightRead() * series.readSum()
                            + recommendationProperties.getPopularityWeightRecency() * recency;
                    return new ArticleRecallCandidate(articleId, score, RecallSource.POPULAR);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ArticleRecallCandidate::score).reversed())
                .limit(recommendationProperties.getPopularityCandidateLimit())
                .toList();
        log.debug("Popularity recall produced {} candidates", candidates.size());
        return candidates;
    }

    private Optional<List<Double>> buildUserEmbedding(List<Article> anchorArticles) {
        var embeddings = new ArrayList<List<Double>>();
        for (Article article : anchorArticles) {
            var embedding = parseEmbedding(article.getEmbedding());
            embedding.ifPresent(embeddings::add);
        }

        if (embeddings.isEmpty()) {
            return Optional.empty();
        }

        var dimension = embeddings.iterator().next().size();
        var accumulator = new double[dimension];

        int usedCount = 0;
        for (List<Double> vector : embeddings) {
            if (vector.size() != dimension) {
                continue;
            }
            for (int i = 0; i < dimension; i++) {
                accumulator[i] += vector.get(i);
            }
            usedCount++;
        }

        if (usedCount == 0) {
            return Optional.empty();
        }

        for (int i = 0; i < dimension; i++) {
            accumulator[i] = accumulator[i] / usedCount;
        }

        var averaged = new ArrayList<Double>(dimension);
        for (double value : accumulator) {
            averaged.add(value);
        }
        return Optional.of(averaged);
    }

    private Optional<List<Double>> parseEmbedding(String json) {
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        try {
            var values = objectMapper.readValue(json, EMBEDDING_TYPE);
            if (CollectionUtils.isEmpty(values)) {
                return Optional.empty();
            }
            return Optional.of(values);
        } catch (Exception ex) {
            log.debug("Failed to parse embedding JSON", ex);
            return Optional.empty();
        }
    }

    private Optional<UUID> extractArticleId(Document document) {
        if (document == null || document.getMetadata() == null) {
            return Optional.empty();
        }
        var metadata = document.getMetadata();
        var idValue = metadata.get("articleId");
        if (idValue instanceof String idString && StringUtils.hasText(idString)) {
            try {
                return Optional.of(UUID.fromString(idString));
            } catch (IllegalArgumentException ex) {
                log.debug("Invalid articleId '{}' in vector metadata", idString);
            }
        }
        return Optional.empty();
    }

    private double extractSimilarityScore(Document document) {
        var metadata = document.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return 0.0d;
        }
        var distance = metadata.get("distance");
        if (distance instanceof Number number) {
            var value = number.doubleValue();
            return value > 0 ? 1.0d / (1.0d + value) : 1.0d;
        }
        var similarity = metadata.get("similarity");
        if (similarity instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0d;
    }

    private MetricWindow loadMetricWindow(int windowDays) {
        var end = LocalDate.now(ZoneOffset.UTC);
        var start = end.minusDays(windowDays - 1L);
//        var dimension = (int) ChronoUnit.DAYS.between(start, end) + 1;
//        var metrics = articleDailyMetricRepository.findByIdMetricDateBetween(start, end);
//
//        if (CollectionUtils.isEmpty(metrics)) {
//            return MetricWindow.empty(start, end, dimension);
//        }
//
//        var indexByDate = new ConcurrentHashMap<LocalDate, Integer>(dimension);
//        var seriesByArticle = new HashMap<UUID, MetricSeries>();
//
//        for (ArticleDailyMetric metric : metrics) {
//            var articleId = metric.getId().getArticleId();
//            var metricDate = metric.getId().getMetricDate();
//            var index = indexByDate.computeIfAbsent(metricDate,
//                    date -> (int) ChronoUnit.DAYS.between(start, date));
//
//            if (index < 0 || index >= dimension) {
//                continue;
//            }
//
//            var series = seriesByArticle.computeIfAbsent(articleId,
//                    id -> new MetricSeries(id, dimension));
//            series.add(index, metric.getCollectedCount(), metric.getReadCount());
//        }
//
//        return new MetricWindow(start, end, dimension, seriesByArticle);
        return MetricWindow.empty(start, end, 0);
    }

    private InteractionContext buildInteractionContext(UserBehaviorDocument document) {
        // Merge read/collection history into a recency-sorted anchor list and an exclusion set.
        var entries = new ArrayList<InteractionEntry>();
        addEntries(entries, document.getCollections());
        addEntries(entries, document.getReadHistory());

        if (entries.isEmpty()) {
            return new InteractionContext(Set.of(), List.of());
        }

        entries.sort(Comparator.comparing(InteractionEntry::timestamp).reversed());

        var exclusion = entries.stream()
                .map(InteractionEntry::articleId)
                .collect(Collectors.toCollection(HashSet::new));

        var limit = Math.max(0, recommendationProperties.getRecentBehaviorLimit());
        if (limit == 0) {
            return new InteractionContext(exclusion, List.of());
        }

        var recent = new ArrayList<UUID>();
        var seen = new HashSet<UUID>();
        for (InteractionEntry entry : entries) {
            if (seen.add(entry.articleId())) {
                recent.add(entry.articleId());
            }
            if (recent.size() >= limit) {
                break;
            }
        }

        return new InteractionContext(exclusion, recent);
    }

    private void addEntries(List<InteractionEntry> entries,
                            List<UserBehaviorDocument.ArticleRef> refs) {
        if (CollectionUtils.isEmpty(refs)) {
            return;
        }
        refs.stream()
                .filter(ref -> ref != null && StringUtils.hasText(ref.getArticleId()) && ref.getTimestamp() != null)
                .forEach(ref -> {
                    try {
                        entries.add(new InteractionEntry(UUID.fromString(ref.getArticleId()), ref.getTimestamp()));
                    } catch (IllegalArgumentException ex) {
                        log.debug("Skip invalid articleId '{}' in user behavior", ref.getArticleId());
                    }
                });
    }

    private double cosineSimilarity(double[] vectorA,
                                    double normA,
                                    double[] vectorB,
                                    double normB) {
        // Guard against empty vectors to avoid NaN results.
        double dot = 0;
        for (int i = 0; i < vectorA.length && i < vectorB.length; i++) {
            dot += vectorA[i] * vectorB[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (normA * normB);
    }

    private double computeRecencyFactor(Instant publishedAt, Instant now, double halfLifeDays) {
        if (publishedAt == null) {
            return 0;
        }
        if (now.isBefore(publishedAt)) {
            return 1.0d;
        }
        var age = Duration.between(publishedAt, now);
        var ageDays = age.toHours() / 24.0d;
        if (ageDays <= 0) {
            return 1.0d;
        }
        return Math.pow(0.5d, ageDays / halfLifeDays);
    }

    private record InteractionEntry(UUID articleId, Instant timestamp) {
    }

    private record InteractionContext(Set<UUID> exclusionSet, List<UUID> recentArticleIds) {
    }

    private record MetricWindow(LocalDate start,
                                LocalDate end,
                                int dimension,
                                Map<UUID, MetricSeries> seriesByArticle) {
        static MetricWindow empty(LocalDate start, LocalDate end, int dimension) {
            return new MetricWindow(start, end, dimension, Map.of());
        }
    }

    private static final class MetricSeries {
        private final UUID articleId;
        private final double[] collected;
        private final double[] read;
        private double collectedSum;
        private double readSum;
        private Double collectedNorm;

        MetricSeries(UUID articleId, int dimension) {
            this.articleId = articleId;
            this.collected = new double[dimension];
            this.read = new double[dimension];
        }

        void add(int index, long collectedCount, long readCount) {
            this.collected[index] = collectedCount;
            this.read[index] = readCount;
            this.collectedSum += collectedCount;
            this.readSum += readCount;
            this.collectedNorm = null;
        }

        UUID articleId() {
            return articleId;
        }

        double[] collected() {
            return collected;
        }

        double collectedSum() {
            return collectedSum;
        }

        double readSum() {
            return readSum;
        }

        double[] read() {
            return read;
        }

        double collectedNorm() {
            if (collectedNorm == null) {
                double sumSquares = 0;
                for (double value : collected) {
                    sumSquares += value * value;
                }
                collectedNorm = Math.sqrt(sumSquares);
            }
            return collectedNorm;
        }
    }

    public record ArticleRecallCandidate(UUID articleId, double score, RecallSource source) {
    }

    public enum RecallSource {
        COLLABORATIVE,
        CONTENT,
        POPULAR
    }

    public record RecallCandidates(List<ArticleRecallCandidate> collaborative,
                                   List<ArticleRecallCandidate> content,
                                   List<ArticleRecallCandidate> popular) {

        public static RecallCandidates empty() {
            return new RecallCandidates(List.of(), List.of(), List.of());
        }
    }
}
