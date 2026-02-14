package org.bitmagic.ifeed.application.radar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.RadarProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.radar.RadarSnapshot;
import org.bitmagic.ifeed.domain.model.radar.RadarTopic;
import org.bitmagic.ifeed.domain.model.radar.RadarTopicArticle;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.radar.RadarAdminRepository;
import org.bitmagic.ifeed.domain.repository.radar.RadarSnapshotRepository;
import org.bitmagic.ifeed.domain.repository.radar.RadarTopicArticleRepository;
import org.bitmagic.ifeed.domain.repository.radar.RadarTopicRepository;
import org.bitmagic.ifeed.domain.service.radar.RadarTopicNamingService;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.bitmagic.ifeed.infrastructure.vector.SearchRequestTurbo;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Periodically builds a radar snapshot by clustering recent article embeddings.
 *
 * <p>整体思路：
 * <ol>
 *   <li>选取时间窗口内的候选文章（并且 embedding 已生成）</li>
 *   <li>对每篇文章在向量库里检索近邻（topK + 相似度阈值）</li>
 *   <li>把“近邻关系”视为无向图的边，形成相似度图</li>
 *   <li>用并查集（Union-Find）求连通分量（每个连通分量 = 一个话题 topic）</li>
 *   <li>把 snapshot + topics + topic-article 映射落库，保证分页一致性</li>
 * </ol>
 *
 * <p>MVP 取舍/注意点：
 * <ul>
 *   <li>候选集目前最多取 10000 篇文章。</li>
 *   <li>聚类质量高度依赖 (neighbors, similarityThreshold, minClusterSize) 参数。</li>
 *   <li>当前实现是“每篇文章检索一次近邻”，规模上来会比较耗时。</li>
 *   <li>话题命名依赖 AI 服务，需考虑失败/超时/降级策略。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RadarSnapshotBuildScheduler {

    private final RadarProperties properties;
    private final VectorStoreTurbo vectorStore;
    private final ArticleRepository articleRepository;
    private final ArticleEmbeddingRepository articleEmbeddingRepository;
    private final RadarSnapshotRepository snapshotRepository;
    private final RadarTopicRepository topicRepository;
    private final RadarTopicArticleRepository topicArticleRepository;
    private final RadarAdminRepository radarAdminRepository;
    private final RadarTopicNamingService topicNamingService;

    @Scheduled(
            initialDelayString = "${app.radar.initial-delay:PT1M}",
            fixedDelayString = "${app.radar.fixed-delay:PT1H}"
    )
    @Transactional
    public void build() {
        // 定时任务入口：周期性构建一份新的 radar snapshot。
        log.info("Starting scheduled radar snapshot ");
        Instant now = Instant.now();

        // 清理过期 snapshot/topic，避免表无限增长。
        radarAdminRepository.deleteExpiredSnapshots();

        // 1) 拉取候选文章。
        // 只选择 embedding 已生成的文章（embeddingGenerated = true），因为后续要做向量近邻检索。
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

        // 2) 批量加载候选文章的 embedding。
        // 这些向量将作为向量库近邻检索的输入。
        Map<Long, ArticleEmbeddingRecord> embeddingById = articleEmbeddingRepository.findAllByIds(recent.stream()
                        .map(Article::getId)
                        .filter(Objects::nonNull)
                        .toList())
                .stream()
                .collect(Collectors.toMap(ArticleEmbeddingRecord::id, r -> r));

        // 3) 准备 snapshotId（真正落库在聚类结果可用后再执行）。
        // snapshotId 用来保证分页稳定性：digest/topicDetail 分页都绑定该 snapshotId，避免翻页过程中结果变化。
        String snapshotId = "radar_%s_w%dh".formatted(now.toString().replace(":", "").replace(".", ""), windowHours);

        // 4) 构建相似度图，并用“连通分量”做聚类。
        // 图结构用并查集隐式表示：
        // - 每篇文章是一个节点
        // - 如果 A 的近邻里包含 B，就把 A 与 B union 到同一集合
        Map<Long, Article> byId = recent.stream().collect(Collectors.toMap(Article::getId, a -> a));
        List<Long> ids = new ArrayList<>(byId.keySet());
        UnionFind uf = new UnionFind(ids);

        // 向量检索过滤条件：限制近邻检索范围在同一时间窗口内。
        // 这样能减少跨窗口连接，避免出现“巨型簇”。
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op timeFilter = b.and(
                b.gte("publishedAt", from.getEpochSecond()),
                b.lte("publishedAt", now.getEpochSecond())
        );

        // 对每篇候选文章做近邻检索，并把近邻关系 union 进并查集。
        for (Long id : ids) {
            Article a = byId.get(id);
            if (a == null) continue;
            ArticleEmbeddingRecord emb = embeddingById.get(id);
            if (emb == null || emb.embedding() == null || emb.embedding().length == 0) {
                continue;
            }

            // 在向量库里检索近邻（ANN）。
            SearchRequestTurbo req = SearchRequestTurbo.builder()
                    .embedding(emb.embedding())
                    .topK(properties.getNeighbors())
                    .similarityThreshold(properties.getSimilarityThreshold())
                    .filterExpression(timeFilter.build())
                    .build();

            List<Document> nn = vectorStore.similaritySearch(req);
            if (nn == null || nn.isEmpty()) {
                continue;
            }
            for (Document d : nn) {
                try {
                    long otherId = Long.parseLong(d.getId());
                    if (otherId != id && byId.containsKey(otherId)) {
                        uf.union(id, otherId);
                    }
                } catch (RuntimeException ignored) {
                }
            }
        }

        // 5) 从并查集中提取聚类结果（连通分量），并做过滤/截断。
        // - minClusterSize：过滤噪声小簇
        // - maxTopics：限制每个 snapshot 存储的 topic 数量
        Map<Long, List<Long>> clusters = uf.groups();
        List<List<Long>> clusterList = clusters.values().stream()
                .filter(list -> list.size() >= properties.getMinClusterSize())
                .sorted((a, b2) -> Integer.compare(b2.size(), a.size()))
                .limit(properties.getMaxTopics())
                .toList();

        if (clusterList.isEmpty()) {
            log.info("Radar snapshot build produced no clusters meeting minClusterSize={}", properties.getMinClusterSize());
            return;
        }

        // 6) 创建 snapshot 记录（只有在生成有效聚类时才落库）。
        RadarSnapshot snapshot = new RadarSnapshot();
        snapshot.setSnapshotId(snapshotId);
        snapshot.setGeneratedAt(now);
        snapshot.setWindowHours(windowHours);
        snapshot.setExpiresAt(now.plus(properties.getSnapshotTtl()));
        snapshotRepository.save(snapshot);

        // 7) 聚类落库：topic + topic-article 映射。
        // 每个 cluster 对应一个 RadarTopic；topic 内文章按发布时间倒序排序并写入 rank。
        for (List<Long> cluster : clusterList) {
            UUID topicId = UUID.randomUUID();

            // 话题命名交给 AI：把 cluster 内文章标题作为提示词。
            // 这里为了简单直接传全量标题；规模更大时建议采样/去重，否则 token 成本和失败率会上升。
            List<String> titles = cluster.stream().map(byId::get).filter(Objects::nonNull).map(Article::getTitle).toList();
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
            topicRepository.save(topic);

            // 对 topic 内文章按“新→旧”排序，生成 rank。
            // rank 用于稳定分页；score 目前是占位值（1.0）。
            List<Long> ordered = cluster.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Article::getPublishedAt).reversed())
                    .map(Article::getId)
                    .toList();

            int rank = 0;
            for (Long articleId : ordered) {
                rank++;
                Article a = byId.get(articleId);
                if (a == null) continue;
                RadarTopicArticle ta = new RadarTopicArticle();
                ta.setSnapshotId(snapshotId);
                ta.setTopicId(topicId);
                ta.setArticleId(articleId);
                ta.setRank(rank);
                ta.setScore(1.0);
                ta.setPublishedAt(a.getPublishedAt());
                topicArticleRepository.save(ta);
            }
        }

        log.info("Radar snapshot built: snapshotId={}, topics={}, windowHours={}", snapshotId, clusterList.size(), windowHours);
    }


    private static final class UnionFind {
        private final Map<Long, Long> parent = new HashMap<>();
        private final Map<Long, Integer> rank = new HashMap<>();

        UnionFind(List<Long> nodes) {
            for (Long n : nodes) {
                parent.put(n, n);
                rank.put(n, 0);
            }
        }

        long find(long x) {
            long p = parent.get(x);
            if (p != x) {
                parent.put(x, find(p));
            }
            return parent.get(x);
        }

        void union(long a, long b) {
            long ra = find(a);
            long rb = find(b);
            if (ra == rb) return;
            int rka = rank.get(ra);
            int rkb = rank.get(rb);
            if (rka < rkb) {
                parent.put(ra, rb);
            } else if (rka > rkb) {
                parent.put(rb, ra);
            } else {
                parent.put(rb, ra);
                rank.put(ra, rka + 1);
            }
        }

        Map<Long, List<Long>> groups() {
            Map<Long, List<Long>> grouped = new HashMap<>();
            for (Long n : parent.keySet()) {
                long root = find(n);
                grouped.computeIfAbsent(root, k -> new ArrayList<>()).add(n);
            }
            return grouped;
        }
    }
}
