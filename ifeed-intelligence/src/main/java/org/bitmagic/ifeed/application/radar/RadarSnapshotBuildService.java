package org.bitmagic.ifeed.application.radar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.bitmagic.ifeed.config.properties.RadarProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.radar.RadarSnapshot;
import org.bitmagic.ifeed.domain.model.radar.RadarTopic;
import org.bitmagic.ifeed.domain.model.radar.RadarTopicArticle;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.service.radar.RadarTopicNamingService;
import org.bitmagic.ifeed.infrastructure.ai.NSFWService;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RadarSnapshotBuildService {

    private final RadarProperties properties;
    private final ArticleRepository articleRepository;
    private final ArticleEmbeddingRepository articleEmbeddingRepository;
    private final RadarSnapshotSaveService saveService;
    private final RadarTopicNamingService topicNamingService;
    private final NSFWService nsfwService;

    public void build() {
        log.info("Starting radar snapshot build");
        Instant now = Instant.now();

        // 1) 拉取候选文章（embedding 已生成）
        int windowHours = properties.getWindowHours();
        Instant from = now.minus(Duration.ofHours(windowHours));
        List<Article> recent = loadCandidates(from, now);

        if (recent.isEmpty()) {
            log.info("Radar snapshot build skipped: no recent embedded articles");
            return;
        }

        Set<String> seenTitles = new HashSet<>();
        recent = recent.stream()
                .sorted(Comparator.comparing(Article::getPublishedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Article::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .filter(article -> StringUtils.isBlank(article.getTitle()) || seenTitles.add(article.getTitle()))
                .toList();

        // 2) 批量加载 embedding
        Map<Long, ArticleEmbeddingRecord> embeddingById = new HashMap<>();
        List<Long> articleIds = recent.stream().map(Article::getId).toList();
        for (int start = 0; start < articleIds.size(); start += 1000) {
            articleEmbeddingRepository.findAllByIds(articleIds.subList(start, Math.min(start + 1000, articleIds.size())))
                    .forEach(embedding -> embeddingById.put(embedding.id(), embedding));
        }

        Map<Long, Article> byId = recent.stream().collect(Collectors.toMap(Article::getId, a -> a));

        // 3) 构建近邻表（内存余弦相似度计算，替代 N 次向量检索）
        NeighborGraph neighborGraph = buildNeighborMap(embeddingById, byId);

        // 4) DBSCAN 聚类
        List<List<Long>> clusterList = runDbscan(neighborGraph);

        if (clusterList.isEmpty()) {
            log.info("Radar snapshot build produced no clusters meeting minClusterSize={}", properties.getMinClusterSize());
            return;
        }

        // 5) 组装并落库
        String snapshotId = "radar_%s_w%dh".formatted(
                now.toString().replace(":", "").replace(".", "").replace("-", ""),
                windowHours);

        RadarSnapshot snapshot = new RadarSnapshot();
        snapshot.setSnapshotId(snapshotId);
        snapshot.setGeneratedAt(now);
        snapshot.setWindowHours(windowHours);

        List<RadarTopic> topics = new ArrayList<>();
        List<RadarTopicArticle> articles = new ArrayList<>();
        assembleTopics(clusterList, byId, snapshotId, now, topics, articles);

        Duration ttl = properties.getSnapshotTtl().compareTo(properties.getFixedDelay()) > 0
                ? properties.getSnapshotTtl() : properties.getFixedDelay().multipliedBy(2);
        snapshot.setExpiresAt(Instant.now().plus(ttl));
        saveService.save(snapshot, topics, articles);

        log.info("Radar snapshot built: snapshotId={}, topics={}, articles={}, windowHours={}",
                snapshotId, topics.size(), articles.size(), windowHours);
    }

    private List<Article> loadCandidates(Instant from, Instant until) {
        List<Article> candidates = new ArrayList<>();
        long lastId = 0;
        while (true) {
            long afterId = lastId;
            List<Article> batch = articleRepository.findAll(
                    (root, query, builder) -> builder.and(
                            builder.greaterThan(root.get("id"), afterId),
                            builder.greaterThan(root.get("publishedAt"), from),
                            builder.lessThanOrEqualTo(root.get("publishedAt"), until),
                            builder.isTrue(root.get("embeddingGenerated"))),
                    PageRequest.of(0, 1000, Sort.by("id"))).getContent();
            candidates.addAll(batch);
            if (batch.size() < 1000) return candidates;
            lastId = batch.getLast().getId();
        }
    }

    /**
     * 用内存中已加载的 embedding 两两计算余弦相似度，构建邻居表。
     * 替代原来的 N 次向量数据库检索，消除所有网络往返开销。
     */
    private NeighborGraph buildNeighborMap(
            Map<Long, ArticleEmbeddingRecord> embeddingById,
            Map<Long, Article> byId) {

        List<Long> candidateIds = embeddingById.keySet().stream()
                .filter(byId::containsKey)
                .filter(id -> {
                    float[] vec = embeddingById.get(id).embedding();
                    return vec != null && vec.length > 0;
                })
                .sorted()
                .toList();

        double threshold = properties.getSimilarityThreshold();
        List<BitSet> neighbors = new ArrayList<>();
        for (int index = 0; index < candidateIds.size(); index++) {
            neighbors.add(new BitSet());
        }
        for (int left = 0; left < candidateIds.size(); left++) {
            float[] leftVector = embeddingById.get(candidateIds.get(left)).embedding();
            for (int right = left + 1; right < candidateIds.size(); right++) {
                double similarity = cosineSimilarity(leftVector, embeddingById.get(candidateIds.get(right)).embedding());
                if (similarity >= threshold) {
                    neighbors.get(left).set(right);
                    neighbors.get(right).set(left);
                }
            }
        }

        log.info("Neighbor map built: candidates={}, with-neighbors={}", candidateIds.size(),
                neighbors.stream().filter(adjacent -> !adjacent.isEmpty()).count());
        return new NeighborGraph(candidateIds, neighbors);
    }

    private record NeighborGraph(List<Long> articleIds, List<BitSet> neighbors) {}

    /**
     * DBSCAN 聚类。核心点扩展簇，边缘点加入不扩展，噪声点丢弃。
     */
    private List<List<Long>> runDbscan(NeighborGraph graph) {
        int minPts = properties.getMinClusterSize();

        BitSet corePoints = new BitSet();
        for (int index = 0; index < graph.articleIds().size(); index++) {
            if (graph.neighbors().get(index).cardinality() + 1 >= minPts) corePoints.set(index);
        }
        BitSet assigned = new BitSet();
        List<List<Long>> clusters = new ArrayList<>();

        for (int seed = corePoints.nextSetBit(0); seed >= 0; seed = corePoints.nextSetBit(seed + 1)) {
            if (assigned.get(seed)) continue;
            List<Long> cluster = new ArrayList<>();
            Deque<Integer> queue = new ArrayDeque<>();
            queue.push(seed);
            assigned.set(seed);
            cluster.add(graph.articleIds().get(seed));

            while (!queue.isEmpty()) {
                BitSet adjacent = graph.neighbors().get(queue.pop());
                for (int neighbor = adjacent.nextSetBit(0); neighbor >= 0; neighbor = adjacent.nextSetBit(neighbor + 1)) {
                    if (assigned.get(neighbor)) continue;
                    assigned.set(neighbor);
                    cluster.add(graph.articleIds().get(neighbor));
                    if (corePoints.get(neighbor)) {
                        queue.push(neighbor);
                    }
                }
            }
            clusters.add(cluster);
        }

        List<List<Long>> clusterList = clusters.stream()
                .filter(list -> list.size() >= minPts)
                .sorted((x, y) -> Integer.compare(y.size(), x.size()))
                .limit(properties.getMaxTopics())
                .toList();

        log.info("DBSCAN result: candidates={}, corePoints={}, clusters={}, noise={}",
                graph.articleIds().size(), corePoints.cardinality(), clusterList.size(), graph.articleIds().size() - assigned.cardinality());
        return clusterList;
    }

    /**
     * 根据聚类结果组装 RadarTopic 和 RadarTopicArticle。
     */
    private void assembleTopics(
            List<List<Long>> clusterList,
            Map<Long, Article> byId,
            String snapshotId,
            Instant now,
            List<RadarTopic> topics,
            List<RadarTopicArticle> articles) {

        for (List<Long> cluster : clusterList) {
            UUID topicId = UUID.randomUUID();

            List<String> titles = cluster.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .map(Article::getTitle)
                    .toList();
            Set<String> urls = cluster.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .map(Article::getLink)
                    .collect(Collectors.toSet());
            try {
                if (nsfwService.isNSFW(urls) || nsfwService.isNSFW(Strings.join(titles, ','))) {
                    log.info("Radar topic skipped: NSFW content, articles={}", cluster.size());
                    continue;
                }
            } catch (RuntimeException exception) {
                log.warn("Radar topic skipped: content check failed, articleIds={}", cluster, exception);
                continue;
            }
            var named = topicNamingService.nameTopic(titles);
            RadarTopic topic = new RadarTopic();
            topic.setTopicId(topicId);
            topic.setSnapshotId(snapshotId);
            topic.setTitle(named.title());
            topic.setDescription(named.description());
            topic.setCreatedAt(now);
            topic.setUpdatedAt(now);
            topic.setArticleCount(cluster.size());
            topic.setTopKeywords(CollectionUtils.isEmpty(named.keywords()) ? null : JSON.toJson(named.keywords()));
            topic.setScore((double) cluster.size());
            topics.add(topic);

            List<Long> ordered = cluster.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Article::getPublishedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(Article::getId)
                    .toList();

            int rank = 0;
            for (Long articleId : ordered) {
                rank++;
                RadarTopicArticle ta = new RadarTopicArticle();
                ta.setSnapshotId(snapshotId);
                ta.setTopicId(topicId);
                ta.setArticleId(articleId);
                ta.setRank(rank);
                ta.setScore(1.0);
                ta.setPublishedAt(byId.get(articleId).getPublishedAt());
                articles.add(ta);
            }
        }
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        int len = Math.min(a.length, b.length);
        for (int k = 0; k < len; k++) {
            dot += (double) a[k] * b[k];
            normA += (double) a[k] * a[k];
            normB += (double) b[k] * b[k];
        }
        return (normA == 0.0 || normB == 0.0) ? 0.0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
