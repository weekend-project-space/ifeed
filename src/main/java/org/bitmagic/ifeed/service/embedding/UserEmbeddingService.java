package org.bitmagic.ifeed.service.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.RecommendationProperties;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.entity.UserEmbedding;
import org.bitmagic.ifeed.domain.model.ArticleEmbeddingRecord;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户Embedding构建服务，结合行为、文章向量与标签信息生成用户画像。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEmbeddingService {

    // 行为动作的默认权重，体现正负反馈强弱
    private static final double DEFAULT_READ_WEIGHT = 1.0;
    private static final double DEFAULT_COLLECT_WEIGHT = 2.0;
    private static final double DEFAULT_SKIP_WEIGHT = -0.6;
    // 向量维度、时间衰减等建模参数
    private static final Duration DECAY_HALF_LIFE = Duration.ofHours(24);
    private static final int EMBEDDING_DIMENSION = 1024;
    private static final int PERSONA_TAG_LIMIT = 5;
    private static final int PERSONA_CATEGORY_LIMIT = 3;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };
    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleEmbeddingRepository articleEmbeddingRepository;
    private final ArticleRepository articleRepository;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final RecommendationProperties recommendationProperties;
    private final EmbeddingModel embeddingModel;

    /**
     * 读取用户行为并重新生成Embedding与人设信息。
     */
    @Transactional
    public Optional<UserEmbedding> rebuildUserEmbedding(UUID userId) {
        // 先获取存储的行为文档，若不存在则无需继续
        var document = userBehaviorRepository.findById(userId.toString()).orElse(null);
        if (document == null) {
            log.debug("No behavior document found for user {}", userId);
            return Optional.empty();
        }
        // 汇总阅读、收藏、互动等事件并按时间排序
        var events = collectEvents(document);
        if (events.isEmpty()) {
            log.debug("No events available to build embedding for user {}", userId);
            return Optional.empty();
        }
        // 仅保留最近发生的事件，强调最新意图
        events.sort(Comparator.comparing(BehaviorEvent::timestamp).reversed());
        var limit = recommendationProperties.getRecentBehaviorLimit();
        var limitedEvents = events.stream().limit(limit).toList();

        // 批量加载文章向量和标签信息，后续用于聚合偏好
        var articleIds = limitedEvents.stream().map(BehaviorEvent::articleId).collect(Collectors.toSet());
        var articleEmbeddings = articleEmbeddingRepository.findAllByIds(articleIds).stream()
                .collect(Collectors.toMap(ArticleEmbeddingRecord::id, Function.identity()));
        var articleInterests = buildArticleInterests(articleRepository.findByIdIn(articleIds));
        if (articleEmbeddings.isEmpty()) {
            log.debug("No article embeddings found for user {}", userId);
            return Optional.empty();
        }

        var now = Instant.now();
        float[] accumulator = new float[EMBEDDING_DIMENSION];
        double weightSum = 0.0;
        var tagStats = new HashMap<String, Stat>();
        var categoryStats = new HashMap<String, Stat>();
        Instant earliestPositiveEvent = null;

        for (var event : limitedEvents) {
            var articleRecord = articleEmbeddings.get(event.articleId());
            if (articleRecord == null || articleRecord.embedding().length == 0) {
                continue;
            }
            var weight = event.weight() * decayFactor(event.timestamp(), now);
            if (weight == 0.0) {
                continue;
            }
            var vector = ensureDimension(articleRecord.embedding());
            for (int i = 0; i < accumulator.length; i++) {
                accumulator[i] += vector[i] * weight;
            }
            weightSum += Math.abs(weight);

            if (weight > 0.0) {
                var interest = articleInterests.get(event.articleId());
                if (interest != null) {
                    if (event.timestamp() != null
                            && (earliestPositiveEvent == null || event.timestamp().isBefore(earliestPositiveEvent))) {
                        earliestPositiveEvent = event.timestamp();
                    }
                    for (var tag : interest.tags()) {
                        updateStat(tagStats, tag, weight);
                    }
                    if (interest.category() != null) {
                        updateStat(categoryStats, interest.category(), weight);
                    }
                }
            }
        }

        if (weightSum == 0.0) {
            log.debug("Weighted sum is zero for user {}", userId);
            return Optional.empty();
        }

        normalize(accumulator);
        userEmbeddingRepository.deleteAllByIdInBatch(List.of(userId));
        // 计算作者统计与收藏详情，用于丰富用户画像提示词
        var articlesList = articleRepository.findByIdIn(articleIds);
        var articleById = articlesList.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(Article::getId, Function.identity()));

        long windowDays = earliestPositiveEvent == null ? 1
                : Math.max(Duration.between(earliestPositiveEvent, now).toDays(), 1);
        Instant fromTs = earliestPositiveEvent == null ? now.minus(Duration.ofDays(windowDays)) : earliestPositiveEvent;

        // 作者统计: author -> AuthorStat(totalArticles, readCount, collectedCount)
        var authorCountMap = new HashMap<String, Integer>();
        for (var art : articlesList) {
            if (art == null)
                continue;
            if (art.getPublishedAt() == null)
                continue;
            if (art.getPublishedAt().isBefore(fromTs) || art.getPublishedAt().isAfter(now))
                continue;
            var author = art.getAuthor() == null ? "未知作者" : art.getAuthor().trim();
            if (author.isEmpty())
                author = "未知作者";
            authorCountMap.put(author, authorCountMap.getOrDefault(author, 0) + 1);
        }

        var authorReadCount = new HashMap<String, Integer>();
        if (!CollectionUtils.isEmpty(document.getReadHistory())) {
            for (var item : document.getReadHistory()) {
                if (item == null || item.getTimestamp() == null)
                    continue;
                if (item.getTimestamp().isBefore(fromTs) || item.getTimestamp().isAfter(now))
                    continue;
                toUuid(item.getArticleId()).ifPresent(aid -> {
                    var art = articleById.get(aid);
                    if (art != null) {
                        var author = art.getAuthor() == null ? "未知作者" : art.getAuthor().trim();
                        if (author.isEmpty())
                            author = "未知作者";
                        authorReadCount.put(author, authorReadCount.getOrDefault(author, 0) + 1);
                    }
                });
            }
        }

        var authorCollectedCount = new HashMap<String, Integer>();
        var collectedDetails = new ArrayList<CollectedArticleDetail>();
        if (!CollectionUtils.isEmpty(document.getCollections())) {
            for (var col : document.getCollections()) {
                if (col == null || col.getTimestamp() == null)
                    continue;
                if (col.getTimestamp().isBefore(fromTs) || col.getTimestamp().isAfter(now))
                    continue;
                toUuid(col.getArticleId()).ifPresent(aid -> {
                    var art = articleById.get(aid);
                    if (art != null) {
                        var author = art.getAuthor() == null ? "未知作者" : art.getAuthor().trim();
                        if (author.isEmpty())
                            author = "未知作者";
                        authorCollectedCount.put(author, authorCollectedCount.getOrDefault(author, 0) + 1);
                        var tags = parseTags(art.getTags());
                        collectedDetails.add(new CollectedArticleDetail(art.getTitle(), author, tags,
                                normalizeCategory(art.getCategory())));
                    }
                });
            }
        }

        // 将统计结果转换为可展示的结构
        var authorStats = authorCountMap.entrySet().stream()
                .map(e -> new AuthorStat(e.getKey(), e.getValue(), authorReadCount.getOrDefault(e.getKey(), 0),
                        authorCollectedCount.getOrDefault(e.getKey(), 0)))
                .sorted((a, b) -> Integer.compare(b.totalArticles(), a.totalArticles()))
                .collect(Collectors.toList());


        var persona = buildUserPersona(tagStats, categoryStats, windowDays, authorStats, collectedDetails);

        String prompt = persona.map(UserPersona::prompt).orElse(null);
        if (Objects.nonNull(prompt)) {
            float[] embed = embeddingModel.embed(prompt);
            accumulator = minix(embed, accumulator, 0.7f, 0.3f);
        }

        var embeddingBuilder = UserEmbedding.builder()
                .userId(userId)
                .embedding(accumulator)
                .content(prompt)
                .updatedAt(now);

        var embedding = embeddingBuilder.build();
        userEmbeddingRepository.save(embedding);
        return Optional.of(embedding);
    }

    @Transactional(readOnly = true)
    public Optional<UserEmbedding> getUserEmbedding(UUID userId) {
        return userEmbeddingRepository.findById(userId);
    }

    private float[] minix(float[] embed1, float[] embed2, float weight1, float weight2) {
        float[] accumulator = new float[embed1.length];
        for (int i = 0; i < accumulator.length; i++) {
            accumulator[i] = embed1[i] * weight1;
        }
        for (int i = 0; i < accumulator.length; i++) {
            accumulator[i] += embed2[i] * weight2;
        }
        return accumulator;
    }

    private List<BehaviorEvent> collectEvents(UserBehaviorDocument document) {
        List<BehaviorEvent> events = new ArrayList<>();
        if (!CollectionUtils.isEmpty(document.getReadHistory())) {
            for (var item : document.getReadHistory()) {
                toUuid(item.getArticleId()).ifPresent(articleId -> events
                        .add(new BehaviorEvent(articleId, DEFAULT_READ_WEIGHT, item.getTimestamp())));
            }
        }
        if (!CollectionUtils.isEmpty(document.getCollections())) {
            for (var item : document.getCollections()) {
                toUuid(item.getArticleId()).ifPresent(articleId -> events
                        .add(new BehaviorEvent(articleId, DEFAULT_COLLECT_WEIGHT, item.getTimestamp())));
            }
        }
        if (!CollectionUtils.isEmpty(document.getInteractionHistory())) {
            for (var interaction : document.getInteractionHistory()) {
                var normalized = interaction.getActionType() == null ? ""
                        : interaction.getActionType().trim().toLowerCase(Locale.ROOT);
                double weight = switch (normalized) {
                    case "skip", "dislike" -> DEFAULT_SKIP_WEIGHT;
                    case "like", "star" -> DEFAULT_COLLECT_WEIGHT;
                    default -> DEFAULT_READ_WEIGHT;
                };
                toUuid(interaction.getArticleId()).ifPresent(
                        articleId -> events.add(new BehaviorEvent(articleId, weight, interaction.getTimestamp())));
            }
        }
        return events;
    }

    private float[] ensureDimension(float[] source) {
        if (source.length == EMBEDDING_DIMENSION) {
            return source;
        }
        float[] resized = new float[EMBEDDING_DIMENSION];
        System.arraycopy(source, 0, resized, 0, Math.min(source.length, EMBEDDING_DIMENSION));
        return resized;
    }

    private void normalize(float[] vector) {
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }

    private double decayFactor(Instant eventTime, Instant now) {
        if (eventTime == null) {
            return 1.0;
        }
        var elapsed = Duration.between(eventTime, now);
        if (elapsed.isNegative()) {
            return 1.0;
        }
        double halfLifeHours = DECAY_HALF_LIFE.toMinutes() / 60.0;
        double elapsedHours = elapsed.toMinutes() / 60.0;
        if (halfLifeHours == 0.0) {
            return 1.0;
        }
        return Math.pow(0.5, elapsedHours / halfLifeHours);
    }

    private Optional<UUID> toUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            log.debug("Failed to parse UUID from {}", value);
            return Optional.empty();
        }
    }

    private record BehaviorEvent(UUID articleId, double weight, Instant timestamp) {
    }

    private Map<UUID, ArticleInterest> buildArticleInterests(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return Map.of();
        }
        var interests = new HashMap<UUID, ArticleInterest>(articles.size());
        for (var article : articles) {
            if (article == null || article.getId() == null) {
                continue;
            }
            interests.put(article.getId(),
                    new ArticleInterest(parseTags(article.getTags()), normalizeCategory(article.getCategory())));
        }
        return interests;
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        var normalized = category.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            var tags = OBJECT_MAPPER.readValue(raw, TAGS_TYPE);
            if (tags == null || tags.isEmpty()) {
                return List.of();
            }
            return tags.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .map(tag -> tag.toLowerCase(Locale.ROOT))
                    .distinct()
                    .toList();
        } catch (Exception ex) {
            log.debug("Failed to parse tags for persona building: {}", raw, ex);
            return List.of();
        }
    }

    private void updateStat(Map<String, Stat> stats, String key, double weight) {
        var stat = stats.computeIfAbsent(key, unused -> new Stat());
        stat.count++;
        stat.weightSum += weight;
    }

    private Optional<UserPersona> buildUserPersona(Map<String, Stat> tagStats,
                                                   Map<String, Stat> categoryStats,
                                                   long windowDays,
                                                   List<AuthorStat> authorStats,
                                                   List<CollectedArticleDetail> collectedDetails) {
        if ((tagStats.isEmpty() && categoryStats.isEmpty() && authorStats.isEmpty())) {
            return Optional.empty();
        }

        var topTags = rankTopics(tagStats, windowDays, PERSONA_TAG_LIMIT);
        var topCategories = rankTopics(categoryStats, windowDays, PERSONA_CATEGORY_LIMIT);
        if (topTags.isEmpty() && topCategories.isEmpty() && authorStats.isEmpty()) {
            return Optional.empty();
        }

        var prompt = buildPersonaPrompt(topTags, topCategories, windowDays, authorStats, collectedDetails);
        return Optional.of(new UserPersona(topTags, topCategories, prompt));
    }

    private List<PersonaTopic> rankTopics(Map<String, Stat> stats, long windowDays, int limit) {
        return stats.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().weightSum, a.getValue().weightSum))
                .limit(limit)
                .map(entry -> new PersonaTopic(
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().weightSum,
                        entry.getValue().count / Math.max(windowDays, 1.0)))
                .collect(Collectors.toList());
    }

    private String buildPersonaPrompt(List<PersonaTopic> tags,
                                      List<PersonaTopic> categories,
                                      long windowDays,
                                      List<AuthorStat> authorStats,
                                      List<CollectedArticleDetail> collectedDetails) {
        var builder = new StringBuilder();
        builder.append("用户近约").append(windowDays).append("天的兴趣画像：");
        if (!tags.isEmpty()) {
            builder.append("\n标签偏好：")
                    .append(tags.stream()
                            .map(this::formatTopic)
                            .collect(Collectors.joining("；")));
        }
        if (!categories.isEmpty()) {
            builder.append("\n分类偏好：")
                    .append(categories.stream()
                            .map(this::formatTopic)
                            .collect(Collectors.joining("；")));
        }

        if (!authorStats.isEmpty()) {
            builder.append("\n作者统计：");
            for (var a : authorStats) {
                builder.append(String.format(Locale.ROOT, "\n- %s：共 %d 篇，已读 %d 篇，收藏 %d 篇", a.author(),
                        a.totalArticles(), a.readCount(), a.collectedCount()));
            }
        }

        if (!collectedDetails.isEmpty()) {
            builder.append("\n我收藏的文章（示例）：");
            // 汇总收藏中的标签和分类数量
            var tagCount = new HashMap<String, Integer>();
            var categoryCount = new HashMap<String, Integer>();
            for (var d : collectedDetails) {
                for (var t : d.tags()) {
                    tagCount.put(t, tagCount.getOrDefault(t, 0) + 1);
                }
                if (d.category() != null) {
                    categoryCount.put(d.category(), categoryCount.getOrDefault(d.category(), 0) + 1);
                }
                builder.append(String.format(Locale.ROOT, "\n- %s（作者：%s，分类：%s，标签：%s）", d.title(), d.author(),
                        d.category() == null ? "-" : d.category(),
                        d.tags().isEmpty() ? "-" : String.join(",", d.tags())));
            }
            if (!tagCount.isEmpty()) {
                builder.append("\n收藏标签分布：");
                builder.append(tagCount.entrySet().stream().map(e -> e.getKey() + "×" + e.getValue())
                        .collect(Collectors.joining("；")));
            }
            if (!categoryCount.isEmpty()) {
                builder.append("\n收藏分类分布：");
                builder.append(categoryCount.entrySet().stream().map(e -> e.getKey() + "×" + e.getValue())
                        .collect(Collectors.joining("；")));
            }
        }

        builder.append("\n请推荐与上述偏好高度相关的内容。优先推荐最近几天的内容。");
        return builder.toString();
    }

    private record AuthorStat(String author, int totalArticles, int readCount, int collectedCount) {
    }

    private record CollectedArticleDetail(String title, String author, List<String> tags, String category) {
    }

    private String formatTopic(PersonaTopic topic) {
        return String.format(Locale.ROOT, "%s：%d次，%.2f次/天",
                topic.name(),
                topic.count(),
                topic.dailyFrequency());
    }

    private static class Stat {
        private long count;
        private double weightSum;
    }

    private record ArticleInterest(List<String> tags, String category) {
    }

    private record PersonaTopic(String name, long count, double weightSum, double dailyFrequency) {
    }

    private record UserPersona(List<PersonaTopic> tags,
                               List<PersonaTopic> categories,
                               String prompt) {
    }
}
