package org.bitmagic.ifeed.application.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.bitmagic.ifeed.config.properties.RecommendationProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.UserVectorStore;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserVectorRepository;
import org.bitmagic.ifeed.infrastructure.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户画像向量构建服务
 * 基于用户近期交互序列和时间衰减权重，生成用户的向量表示
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEmbeddingService {

    private static final int EMBEDDING_DIMENSION = 1024;
    private static final int MAX_PROFILE_ITEMS = 20;
    private static final String PROFILE_SEPARATOR = " | ";

    private final SequenceStore sequenceStore;
    private final ArticleEmbeddingRepository articleEmbeddingRepository;
    private final ArticleRepository articleRepository;
    private final UserVectorRepository userVectorRepository;
    private final RecommendationProperties recommendationProperties;
    private final AiProviderProperties aiProviderProperties;

    /**
     * 重建用户向量
     *
     * @param userId 用户ID
     * @return 新构建的用户向量，如果无法构建则返回 empty
     */
    @Transactional
    public Optional<UserVectorStore> rebuildUserEmbedding(Integer userId) {
        // 1. 前置检查
        if (!aiProviderProperties.isEnabled()) {
            log.debug("AI provider is disabled, skip user embedding rebuild");
            return Optional.empty();
        }

        if (userId == null) {
            log.warn("Cannot rebuild user embedding: userId is null");
            return Optional.empty();
        }

        try {
            // 2. 检查是否需要重建
            Optional<UserVectorStore> existingOpt = userVectorRepository.findById(userId);

            // 3. 获取用户交互数据
            int behaviorLimit = Math.max(1, recommendationProperties.getRecentBehaviorLimit());
            List<SequenceStore.UserInteraction> interactions =
                    sequenceStore.recentInteractions(userId, behaviorLimit);

            if (interactions.isEmpty()) {
                log.debug("No interactions found for user {}, removing existing embedding", userId);
                existingOpt.ifPresent(e -> userVectorRepository.delete(e));
                return Optional.empty();
            }

            // 4. 判断是否需要重建（基于最新交互时间）
            Instant latestInteractionTime = interactions.stream()
                    .map(SequenceStore.UserInteraction::timestamp)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(Instant.EPOCH);

            if (existingOpt.isPresent()) {
                Instant existingUpdatedAt = existingOpt.get().getUpdatedAt();
                if (existingUpdatedAt != null && !existingUpdatedAt.isBefore(latestInteractionTime)) {
                    log.debug("User {} embedding is up-to-date, skip rebuild", userId);
                    return existingOpt;
                }
            }

            // 5. 收集文章ID并查询嵌入向量
            Set<Long> articleIds = interactions.stream()
                    .map(SequenceStore.UserInteraction::itemId)
                    .collect(Collectors.toSet());

            Map<Long, ArticleEmbeddingRecord> embeddingById =
                    articleEmbeddingRepository.findAllByIds(articleIds).stream()
                            .collect(Collectors.toMap(ArticleEmbeddingRecord::id, Function.identity()));

            if (embeddingById.isEmpty()) {
                log.info("No article embeddings found for user {}, removing existing embedding", userId);
                existingOpt.ifPresent(e -> userVectorRepository.delete(e));
                return Optional.empty();
            }

            // 6. 查询文章详情（用于生成 profile text）
            Map<Long, Article> articleById = queryArticles(articleIds);

            // 7. 聚合向量
            Instant now = Instant.now();
            VectorAggregationResult aggregationResult =
                    aggregateVectors(interactions, embeddingById, articleById, now);

            if (aggregationResult == null || aggregationResult.totalWeight == 0.0) {
                log.debug("User {} interactions produced no usable weight, removing existing embedding", userId);
                existingOpt.ifPresent(e -> userVectorRepository.delete(e));
                return Optional.empty();
            }

            // 8. 归一化向量
            float[] normalizedVector = normalizeVector(aggregationResult.aggregatedVector);
            if (normalizedVector == null) {
                log.warn("Failed to normalize vector for user {}", userId);
                existingOpt.ifPresent(e -> userVectorRepository.delete(e));
                return Optional.empty();
            }

            // 9. 构建 profile 文本
            String profileText = buildProfileText(aggregationResult.profileItems);
            if (!StringUtils.hasText(profileText)) {
                profileText = StringUtils.hasText(recommendationProperties.getDefaultProfile())
                        ? recommendationProperties.getDefaultProfile()
                        : "";
            }

            // 10. 保存或更新
            UserVectorStore embedding = existingOpt.orElseGet(() -> UserVectorStore.builder()
                    .userId(userId)
                    .build());

            embedding.setEmbedding(normalizedVector);
            embedding.setContent(profileText);
            embedding.setUpdatedAt(now);

            UserVectorStore saved = userVectorRepository.save(embedding);
            log.info("Successfully rebuilt user embedding for user {} with {} interactions",
                    userId, interactions.size());

            return Optional.of(saved);

        } catch (Exception e) {
            log.error("Failed to rebuild user embedding for user {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * 查询文章详情
     * 处理 Long 到 Integer 的安全转换
     */
    private Map<Long, Article> queryArticles(Set<Long> articleIds) {
        List<Integer> articleIdInts = articleIds.stream()
                .filter(id -> id <= Integer.MAX_VALUE && id >= Integer.MIN_VALUE)
                .map(Long::intValue)
                .toList();

        if (articleIdInts.size() < articleIds.size()) {
            log.warn("Some article IDs are out of Integer range: {} total, {} valid",
                    articleIds.size(), articleIdInts.size());
        }

        return articleRepository.findAllById(articleIdInts).stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));
    }

    /**
     * 聚合向量结果
     */
    private static class VectorAggregationResult {
        float[] aggregatedVector;
        double totalWeight;
        List<ProfileItem> profileItems;

        VectorAggregationResult(float[] vector, double weight, List<ProfileItem> items) {
            this.aggregatedVector = vector;
            this.totalWeight = weight;
            this.profileItems = items;
        }
    }

    /**
     * 聚合用户交互的向量
     */
    private VectorAggregationResult aggregateVectors(
            List<SequenceStore.UserInteraction> interactions,
            Map<Long, ArticleEmbeddingRecord> embeddingById,
            Map<Long, Article> articleById,
            Instant now) {

        float[] aggregated = new float[EMBEDDING_DIMENSION];
        double totalWeight = 0.0;
        List<ProfileItem> profileItems = new ArrayList<>();

        for (SequenceStore.UserInteraction interaction : interactions) {
            ArticleEmbeddingRecord record = embeddingById.get(interaction.itemId());
            if (record == null || record.embedding() == null || record.embedding().length == 0) {
                continue;
            }

            // 确保向量维度正确
            float[] source = ensureDimension(record.embedding());
            if (source == null) {
                log.debug("Invalid embedding dimension for article {}", interaction.itemId());
                continue;
            }

            // 计算最终权重：基础权重 * 时间衰减
            double baseWeight = interaction.weight() > 0
                    ? interaction.weight()
                    : recommendationProperties.getReadWeight();

            double decay = calculateRecencyDecay(interaction.timestamp(), now);
            double finalWeight = baseWeight * decay;

            if (finalWeight <= 0.0) {
                continue;
            }

            // 累加向量
            for (int i = 0; i < aggregated.length; i++) {
                aggregated[i] += source[i] * finalWeight;
            }
            totalWeight += finalWeight;

            // 收集 profile 信息
            Article article = articleById.get(interaction.itemId());
            if (article != null) {
                profileItems.add(new ProfileItem(article, interaction.timestamp(), finalWeight));
            }
        }

        if (totalWeight == 0.0) {
            return null;
        }

        return new VectorAggregationResult(aggregated, totalWeight, profileItems);
    }

    /**
     * 确保向量维度正确
     *
     * @param source 原始向量
     * @return 正确维度的向量，如果无效则返回 null
     */
    private float[] ensureDimension(float[] source) {
        if (source == null || source.length == 0) {
            return null;
        }

        if (source.length == EMBEDDING_DIMENSION) {
            return source.clone();
        }

        // 维度不匹配，截断或填充
        if (source.length != EMBEDDING_DIMENSION) {
            log.debug("Embedding dimension mismatch: expected {}, got {}",
                    EMBEDDING_DIMENSION, source.length);
        }

        float[] target = new float[EMBEDDING_DIMENSION];
        System.arraycopy(source, 0, target, 0, Math.min(source.length, EMBEDDING_DIMENSION));
        return target;
    }

    /**
     * L2 归一化向量
     *
     * @param vector 输入向量
     * @return 归一化后的向量，如果输入无效则返回 null
     */
    private float[] normalizeVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }

        // 计算 L2 范数
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        // 零向量无法归一化
        if (norm < 1e-10) {
            log.warn("Cannot normalize zero vector");
            return null;
        }

        // 归一化
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }

        return normalized;
    }

    /**
     * 计算时间衰减系数
     * 使用半衰期公式：weight = 0.5 ^ (elapsed / halfLife)
     *
     * @param timestamp 事件时间
     * @param now       当前时间
     * @return 衰减系数 [0, 1]
     */
    private double calculateRecencyDecay(Instant timestamp, Instant now) {
        if (timestamp == null) {
            return 1.0;
        }

        Duration halfLife = recommendationProperties.getDecayHalfLife();
        if (halfLife == null || halfLife.isZero() || halfLife.isNegative()) {
            return 1.0;
        }

        double elapsedSeconds = Duration.between(timestamp, now).getSeconds();
        if (elapsedSeconds <= 0) {
            return 1.0;
        }

        double halfLifeSeconds = Math.max(1.0, halfLife.getSeconds());
        return Math.pow(0.5, elapsedSeconds / halfLifeSeconds);
    }

    /**
     * 构建用户兴趣 profile 文本
     *
     * @param items profile 条目列表
     * @return profile 文本
     */
    private String buildProfileText(List<ProfileItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        // 预估容量：每个条目约100字符
        StringBuilder builder = new StringBuilder(items.size() * 100);
        builder.append("用户近期浏览兴趣：");

        items.stream()
                .sorted(Comparator
                        .comparingDouble(ProfileItem::weight).reversed()
                        .thenComparing(ProfileItem::timestamp,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_PROFILE_ITEMS)
                .forEach(item -> {
                    Article article = item.article();
                    builder.append(article.getTitle());

                    if (StringUtils.hasText(article.getCategory())) {
                        builder.append(" ").append(article.getCategory());
                    }

                    if (StringUtils.hasText(article.getTags())) {
                        builder.append(" ").append(article.getTags());
                    }

                    // 人性化时间显示
                    if (item.timestamp() != null) {
                        builder.append(" ").append(DateUtils.formatRelativeTime(item.timestamp()));
                    }

                    builder.append(PROFILE_SEPARATOR);
                });

        // 移除最后的分隔符
        if (builder.length() > PROFILE_SEPARATOR.length()) {
            builder.setLength(builder.length() - PROFILE_SEPARATOR.length());
        }

        return builder.toString();
    }

    /**
     * Profile 条目
     */
    private record ProfileItem(Article article, Instant timestamp, double weight) {
    }
}