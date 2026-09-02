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
import org.bitmagic.ifeed.infrastructure.ai.rerank.RerankerModel;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.springframework.data.domain.PageRequest;
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
        List<Article> recent = articleRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.greaterThan(root.get("publishedAt"), from),
                        cb.isTrue(root.get("embeddingGenerated"))
                ),
                PageRequest.of(0, 10000)
        ).getContent();

        if (recent.isEmpty()) {
            log.info("Radar snapshot build skipped: no recent embedded articles");
            return;
        }

        // 2) 批量加载 embedding
        Map<Long, ArticleEmbeddingRecord> embeddingById = articleEmbeddingRepository.findAllByIds(
                        recent.stream().map(Article::getId).filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(ArticleEmbeddingRecord::id, r -> r));

        Map<Long, Article> byId = recent.stream().collect(Collectors.toMap(Article::getId, a -> a));

        // 3) 构建近邻表（内存余弦相似度计算，替代 N 次向量检索）
        Map<Long, Set<Long>> neighborMap = buildNeighborMap(embeddingById, byId);

        // 4) DBSCAN 聚类
        List<List<Long>> clusterList = runDbscan(neighborMap, byId);

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
        snapshot.setExpiresAt(now.plus(properties.getSnapshotTtl()));

        List<RadarTopic> topics = new ArrayList<>();
        List<RadarTopicArticle> articles = new ArrayList<>();
        assembleTopics(clusterList, byId, snapshotId, now, topics, articles);

        saveService.save(snapshot, topics, articles);

        log.info("Radar snapshot built: snapshotId={}, topics={}, articles={}, windowHours={}",
                snapshotId, topics.size(), articles.size(), windowHours);
    }

    /**
     * 用内存中已加载的 embedding 两两计算余弦相似度，构建邻居表。
     * 替代原来的 N 次向量数据库检索，消除所有网络往返开销。
     */
    private Map<Long, Set<Long>> buildNeighborMap(
            Map<Long, ArticleEmbeddingRecord> embeddingById,
            Map<Long, Article> byId) {

        List<Long> candidateIds = embeddingById.keySet().stream()
                .filter(byId::containsKey)
                .filter(id -> {
                    float[] vec = embeddingById.get(id).embedding();
                    return vec != null && vec.length > 0;
                })
                .toList();

        double threshold = properties.getSimilarityThreshold();
        int topK = properties.getNeighbors();

        Map<Long, Set<Long>> neighborMap = new HashMap<>();

        for (int i = 0; i < candidateIds.size(); i++) {
            Long idA = candidateIds.get(i);
            float[] vecA = embeddingById.get(idA).embedding();

            List<Map.Entry<Long, Double>> hits = new ArrayList<>();
            for (int j = 0; j < candidateIds.size(); j++) {
                if (i == j) continue;
                Long idB = candidateIds.get(j);
                double sim = cosineSimilarity(vecA, embeddingById.get(idB).embedding());
                if (sim >= threshold) {
                    hits.add(Map.entry(idB, sim));
                }
            }

            if (hits.isEmpty()) continue;

            Set<Long> neighbors = hits.stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(topK)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            neighborMap.put(idA, neighbors);
        }

        log.info("Neighbor map built: candidates={}, with-neighbors={}", candidateIds.size(), neighborMap.size());
        return neighborMap;
    }

    /**
     * DBSCAN 聚类。核心点扩展簇，边缘点加入不扩展，噪声点丢弃。
     */
    private List<List<Long>> runDbscan(Map<Long, Set<Long>> neighborMap, Map<Long, Article> byId) {
        int minPts = properties.getMinClusterSize();

        Set<Long> corePoints = neighborMap.entrySet().stream()
                .filter(e -> e.getValue().size() >= minPts)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<Long, Integer> clusterLabel = new HashMap<>();
        int nextClusterId = 0;

        for (Long seed : corePoints) {
            if (clusterLabel.containsKey(seed)) continue;
            int cid = ++nextClusterId;
            Deque<Long> queue = new ArrayDeque<>();
            queue.push(seed);
            clusterLabel.put(seed, cid);

            while (!queue.isEmpty()) {
                Long cur = queue.pop();
                for (Long nbr : neighborMap.getOrDefault(cur, Set.of())) {
                    if (clusterLabel.containsKey(nbr)) continue;
                    clusterLabel.put(nbr, cid);
                    if (corePoints.contains(nbr)) {
                        queue.push(nbr);
                    }
                }
            }
        }

        Map<Integer, List<Long>> grouped = new HashMap<>();
        clusterLabel.forEach((aid, cid) ->
                grouped.computeIfAbsent(cid, k -> new ArrayList<>()).add(aid));

        List<List<Long>> clusterList = grouped.values().stream()
                .filter(list -> list.size() >= minPts)
                .sorted((x, y) -> Integer.compare(y.size(), x.size()))
                .limit(properties.getMaxTopics())
                .toList();

        log.info("DBSCAN result: candidates={}, corePoints={}, clusters={}, noise={}",
                byId.size(), corePoints.size(), clusterList.size(), byId.size() - clusterLabel.size());
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
            var named = topicNamingService.nameTopic(titles);

            if (nsfwService.isNSFW(urls) || nsfwService.isNSFW(Strings.join(titles, ','))) {
                continue;
            }
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
