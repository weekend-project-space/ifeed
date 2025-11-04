package org.bitmagic.ifeed.application.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.bitmagic.ifeed.config.properties.RecommendationProperties;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.UserEmbedding;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户Embedding构建服务（完整版、无省略）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEmbeddingService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleEmbeddingRepository articleEmbeddingRepository;
    private final ArticleRepository articleRepository;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final RecommendationProperties recommendationProperties;
    private final EmbeddingModel embeddingModel;
    private final ChatClient chatClient;
    private final AiProviderProperties aiProviderProperties;

    private static final int EMBEDDING_DIMENSION = 1024;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };

    private static final String SYS_PROMPT = """
            你是一个智能检索助手。你的任务是根据用户兴趣画像，生成一个 BM25检索用的文本关键词查询语句。要求如下：
            
            输入是用户画像，包括：
            标签偏好（带次数或频率）
            分类偏好（带次数或频率）
            用户收藏的文章（包含标题、标签、分类）
            输出是一个 纯文本关键词串，可以直接用于 BM25 检索接口。
            关键词串要求：
            每个关键词用空格分隔，全部作为查询输入 最多输出150字的长度。
            """;

    @Transactional
    public Optional<UserEmbedding> rebuildUserEmbedding(Integer userId) {
        var doc = userBehaviorRepository.findById(userId.toString()).orElse(null);
        if (doc == null) {
            log.debug("No behavior document for user {}", userId);
            return Optional.empty();
        }

        var events = EventCollector.collect(doc, recommendationProperties);
        if (events.isEmpty()) return Optional.empty();

        var recent = events.stream()
                .sorted(Comparator.comparing(BehaviorEvent::timestamp).reversed())
                .limit(recommendationProperties.getRecentBehaviorLimit())
                .toList();

        var articleIds = recent.stream().map(BehaviorEvent::articleId).collect(Collectors.toSet());
        var embeddings = articleEmbeddingRepository.findAllByIds(articleIds).stream()
                .collect(Collectors.toMap(ArticleEmbeddingRecord::id, Function.identity()));
        var articles = articleRepository.findByUidIn(articleIds);
        var articleById = articles.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Article::getUid, Function.identity()));
        var interests = articles.stream()
                .collect(Collectors.toMap(Article::getUid, ArticleInterest::from));

        var aggregated = VectorAggregator.aggregate(recent, embeddings, interests, articleById, doc, recommendationProperties);
        if (aggregated.isEmpty()) return Optional.empty();

        var persona = PersonaBuilder.build(aggregated, recommendationProperties);
        float[] finalVector = aggregated.vector();
        String llmPrompt = persona.orElseThrow().prompt();
        log.debug("user {} embedding {}", userId, llmPrompt);
        if (aiProviderProperties.isEnabled()) {
            llmPrompt = chatClient.prompt(SYS_PROMPT.formatted(llmPrompt))
                    .call().entity(String.class);
            float[] llmVec = embeddingModel.embed(llmPrompt);
            finalVector = VectorMixer.mix(llmVec, finalVector,
                    recommendationProperties.getLlmWeight(), 1 - recommendationProperties.getLlmWeight());
        }

        userEmbeddingRepository.deleteAllByIdInBatch(List.of(userId));
        var embedding = UserEmbedding.builder()
                .userId(userId)
                .embedding(finalVector)
                .content(llmPrompt)
                .updatedAt(Instant.now())
                .build();
        userEmbeddingRepository.save(embedding);
        return Optional.of(embedding);
    }

    // ===================================================================
    // ========================= 内部数据结构 =============================
    // ===================================================================

    private record BehaviorEvent(UUID articleId, double weight, Instant timestamp) {
    }

    private record ArticleInterest(List<String> tags, String category) {
        static ArticleInterest from(Article a) {
            return new ArticleInterest(Tools.parseTags(a.getTags()), Tools.normalizeCategory(a.getCategory()));
        }
    }

    private static class Stat {
        long count = 0;
        double weightSum = 0.0;
    }

    private record AuthorStat(String author, int totalArticles, int readCount, int collectedCount) {
    }

    private record CollectedArticleDetail(String title, String author, List<String> tags, String category) {
    }

    private record PersonaTopic(String name, long count, double weightSum, double dailyFrequency) {
    }

    private record UserPersona(List<PersonaTopic> tags, List<PersonaTopic> categories, String prompt) {
    }

    private record Aggregated(
            float[] vector,
            Map<String, Stat> tagStat,
            Map<String, Stat> catStat,
            Instant earliestPositive,
            List<AuthorStat> authorStats,
            List<CollectedArticleDetail> collectedDetails
    ) {
        static Aggregated empty() {
            return new Aggregated(new float[0], Map.of(), Map.of(), null, List.of(), List.of());
        }

        boolean isEmpty() {
            return vector.length == 0;
        }
    }

    // ===================================================================
    // ========================= 1. 事件收集 ==============================
    // ===================================================================
    private static class EventCollector {
        static List<BehaviorEvent> collect(UserBehaviorDocument doc, RecommendationProperties p) {
            List<BehaviorEvent> events = new ArrayList<>();
            Map<String, Double> actionWeight = Map.of(
                    "skip", p.getSkipWeight(), "dislike", p.getSkipWeight(),
                    "like", p.getCollectWeight(), "star", p.getCollectWeight()
            );

            Optional.of(doc.getReadHistory()).ifPresent(list -> list.forEach(it ->
                    Tools.uuid(it.getArticleId()).ifPresent(id ->
                            events.add(new BehaviorEvent(id, p.getReadWeight(), it.getTimestamp())))));

            Optional.of(doc.getCollections()).ifPresent(list -> list.forEach(it ->
                    Tools.uuid(it.getArticleId()).ifPresent(id ->
                            events.add(new BehaviorEvent(id, p.getCollectWeight(), it.getTimestamp())))));

            Optional.of(doc.getInteractionHistory()).ifPresent(list -> list.forEach(it -> {
                String act = Optional.of(it.getActionType()).map(s -> s.trim().toLowerCase(Locale.ROOT)).orElse("");
                double w = actionWeight.getOrDefault(act, p.getReadWeight());
                Tools.uuid(it.getArticleId()).ifPresent(id ->
                        events.add(new BehaviorEvent(id, w, it.getTimestamp())));
            }));

            return events;
        }
    }

    // ===================================================================
    // ========================= 2. 向量聚合 ==============================
    // ===================================================================
    private static class VectorAggregator {
        static Aggregated aggregate(
                List<BehaviorEvent> events,
                Map<UUID, ArticleEmbeddingRecord> embeddings,
                Map<UUID, ArticleInterest> interests,
                Map<UUID, Article> articleById,
                UserBehaviorDocument doc,
                RecommendationProperties p) {

            float[] vec = new float[EMBEDDING_DIMENSION];
            double weightSum = 0;
            var tagStat = new HashMap<String, Stat>();
            var catStat = new HashMap<String, Stat>();
            Instant earliestPos = null;
            Instant now = Instant.now();

            for (var e : events) {
                var rec = embeddings.get(e.articleId());
                if (rec == null || rec.embedding().length == 0) continue;

                double decay = Tools.decay(e.timestamp(), now, p.getDecayHalfLife());
                double w = e.weight() * decay;
                if (w == 0) continue;

                float[] src = Tools.ensureDim(rec.embedding());
                for (int i = 0; i < vec.length; i++) vec[i] += src[i] * w;
                weightSum += Math.abs(w);

                if (w > 0) {
                    var interest = interests.get(e.articleId());
                    if (interest != null) {
                        if (earliestPos == null || e.timestamp().isBefore(earliestPos)) {
                            earliestPos = e.timestamp();
                        }
                        interest.tags().forEach(t -> Tools.updateStat(tagStat, t, w));
                        Optional.ofNullable(interest.category()).ifPresent(c -> Tools.updateStat(catStat, c, w));
                    }
                }
            }

            if (weightSum == 0) return Aggregated.empty();
            Tools.normalize(vec);

            long windowDays = earliestPos == null ? 1
                    : Math.max(Duration.between(earliestPos, now).toDays(), 1);
            Instant fromTs = earliestPos == null ? now.minus(Duration.ofDays(windowDays)) : earliestPos;

            var authorStats = buildAuthorStats(articleById.values(), doc, fromTs, now);
            var collectedDetails = buildCollectedDetails(doc, articleById, fromTs, now);

            return new Aggregated(vec, tagStat, catStat, earliestPos, authorStats, collectedDetails);
        }

        private static List<AuthorStat> buildAuthorStats(
                Collection<Article> articles,
                UserBehaviorDocument doc,
                Instant fromTs, Instant now) {

            var authorTotal = new HashMap<String, Integer>();
            var authorRead = new HashMap<String, Integer>();
            var authorCollected = new HashMap<String, Integer>();

            // 1. 总文章数（发布时间在窗口内）
            for (var art : articles) {
                if (art == null || art.getPublishedAt() == null) continue;
                if (art.getPublishedAt().isBefore(fromTs) || art.getPublishedAt().isAfter(now)) continue;
                String author = Tools.normalizeAuthor(art.getAuthor());
                authorTotal.merge(author, 1, Integer::sum);
            }

            // 2. 阅读数
            Optional.of(doc.getReadHistory()).ifPresent(list -> list.forEach(it -> {
                if (it.getTimestamp() == null || it.getTimestamp().isBefore(fromTs) || it.getTimestamp().isAfter(now))
                    return;
                Tools.uuid(it.getArticleId()).ifPresent(aid -> {
                    var art = articles.stream().filter(a -> a.getUid().equals(aid)).findFirst().orElse(null);
                    if (art != null) {
                        String author = Tools.normalizeAuthor(art.getAuthor());
                        authorRead.merge(author, 1, Integer::sum);
                    }
                });
            }));

            // 3. 收藏数
            Optional.of(doc.getCollections()).ifPresent(list -> list.forEach(it -> {
                if (it.getTimestamp() == null || it.getTimestamp().isBefore(fromTs) || it.getTimestamp().isAfter(now))
                    return;
                Tools.uuid(it.getArticleId()).ifPresent(aid -> {
                    var art = articles.stream().filter(a -> a.getUid().equals(aid)).findFirst().orElse(null);
                    if (art != null) {
                        String author = Tools.normalizeAuthor(art.getAuthor());
                        authorCollected.merge(author, 1, Integer::sum);
                    }
                });
            }));

            return authorTotal.entrySet().stream()
                    .map(e -> new AuthorStat(e.getKey(),
                            e.getValue(),
                            authorRead.getOrDefault(e.getKey(), 0),
                            authorCollected.getOrDefault(e.getKey(), 0)))
                    .sorted(Comparator.comparingInt(AuthorStat::totalArticles).reversed())
                    .toList();
        }

        private static List<CollectedArticleDetail> buildCollectedDetails(
                UserBehaviorDocument doc,
                Map<UUID, Article> articleById,
                Instant fromTs, Instant now) {

            List<CollectedArticleDetail> details = new ArrayList<>();
            Optional.ofNullable(doc.getCollections()).ifPresent(list -> list.forEach(it -> {
                if (it.getTimestamp() == null || it.getTimestamp().isBefore(fromTs) || it.getTimestamp().isAfter(now))
                    return;
                Tools.uuid(it.getArticleId()).ifPresent(aid -> {
                    var art = articleById.get(aid);
                    if (art != null) {
                        details.add(new CollectedArticleDetail(
                                art.getTitle(),
                                Tools.normalizeAuthor(art.getAuthor()),
                                Tools.parseTags(art.getTags()),
                                Tools.normalizeCategory(art.getCategory())
                        ));
                    }
                });
            }));
            return details;
        }
    }

    // ===================================================================
    // ========================= 3. 人设构建 ==============================
    // ===================================================================
    private static class PersonaBuilder {
        static Optional<UserPersona> build(Aggregated agg, RecommendationProperties p) {
            if (agg.tagStat().isEmpty() && agg.catStat().isEmpty() && agg.authorStats().isEmpty())
                return Optional.empty();

            long days = agg.earliestPositive() == null ? 1
                    : Math.max(Duration.between(agg.earliestPositive(), Instant.now()).toDays(), 1);

            var tags = Tools.rank(agg.tagStat(), days, p.getPersonaTagLimit());
            var cats = Tools.rank(agg.catStat(), days, p.getPersonaCategoryLimit());

            if (tags.isEmpty() && cats.isEmpty() && agg.authorStats().isEmpty()) return Optional.empty();

            String prompt = PromptBuilder.build(tags, cats, days, agg.authorStats(), agg.collectedDetails());
            return Optional.of(new UserPersona(tags, cats, prompt));
        }
    }

    // ===================================================================
    // ========================= 4. Prompt 构建 ===========================
    // ===================================================================
    private static class PromptBuilder {
        static String build(List<PersonaTopic> tags, List<PersonaTopic> cats, long days,
                            List<AuthorStat> authors, List<CollectedArticleDetail> collected) {
            var sb = new StringBuilder();
            sb.append("用户近约").append(days).append("天的兴趣画像：");

            if (!tags.isEmpty()) {
                sb.append("\n标签偏好：").append(tags.stream().map(Tools::formatTopic).collect(Collectors.joining("；")));
            }
            if (!cats.isEmpty()) {
                sb.append("\n分类偏好：").append(cats.stream().map(Tools::formatTopic).collect(Collectors.joining("；")));
            }
            if (!authors.isEmpty()) {
                sb.append("\n作者统计：");
                authors.forEach(a -> sb.append(String.format(Locale.ROOT,
                        "\n- %s：共 %d 篇，已读 %d 篇，收藏 %d 篇",
                        a.author(), a.totalArticles(), a.readCount(), a.collectedCount())));
            }
            if (!collected.isEmpty()) {
                sb.append("\n我收藏的文章（示例）：");
                var tagCnt = new HashMap<String, Integer>();
                var catCnt = new HashMap<String, Integer>();
                collected.forEach(d -> {
                    d.tags().forEach(t -> tagCnt.merge(t, 1, Integer::sum));
                    Optional.of(d.category()).ifPresent(c -> catCnt.merge(c, 1, Integer::sum));
                    sb.append(String.format(Locale.ROOT, "\n- %s（作者：%s，分类：%s，标签：%s）",
                            d.title(), d.author(),
                            d.category() == null ? "-" : d.category(),
                            d.tags().isEmpty() ? "-" : String.join(",", d.tags())));
                });
                if (!tagCnt.isEmpty()) {
                    sb.append("\n收藏标签分布：").append(tagCnt.entrySet().stream()
                            .map(e -> e.getKey() + "×" + e.getValue())
                            .collect(Collectors.joining("；")));
                }
                if (!catCnt.isEmpty()) {
                    sb.append("\n收藏分类分布：").append(catCnt.entrySet().stream()
                            .map(e -> e.getKey() + "×" + e.getValue())
                            .collect(Collectors.joining("；")));
                }
            }
            return sb.toString();
        }
    }

    // ===================================================================
    // ========================= 5. 向量混合 ==============================
    // ===================================================================
    private static class VectorMixer {
        static float[] mix(float[] a, float[] b, float wa, float wb) {
            float[] r = new float[a.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = a[i] * wa + b[i] * wb;
            }
            return r;
        }
    }

    // ===================================================================
    // ========================= 6. 工具方法 ==============================
    // ===================================================================
    private static class Tools {
        static Optional<UUID> uuid(String s) {
            if (s == null || s.isBlank()) return Optional.empty();
            try {
                return Optional.of(UUID.fromString(s));
            } catch (Exception e) {
                log.debug("Invalid UUID: {}", s);
                return Optional.empty();
            }
        }

        static <T> Optional<T> of(T v) {
            return Optional.ofNullable(v);
        }

        static List<String> parseTags(String raw) {
            if (raw == null || raw.isBlank()) return List.of();
            try {
                return MAPPER.readValue(raw, TAGS_TYPE).stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .map(t -> t.toLowerCase(Locale.ROOT))
                        .distinct()
                        .toList();
            } catch (Exception e) {
                log.debug("Parse tags failed: {}", raw, e);
                return List.of();
            }
        }

        static String normalizeCategory(String c) {
            return of(c).map(String::trim).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(Locale.ROOT)).orElse(null);
        }

        static String normalizeAuthor(String a) {
            return of(a).map(String::trim).filter(s -> !s.isEmpty()).orElse("未知作者");
        }

        static float[] ensureDim(float[] src) {
            if (src.length == EMBEDDING_DIMENSION) return src;
            float[] r = new float[EMBEDDING_DIMENSION];
            System.arraycopy(src, 0, r, 0, Math.min(src.length, EMBEDDING_DIMENSION));
            return r;
        }

        static void normalize(float[] v) {
            double n = 0;
            for (float f : v) n += f * f;
            n = Math.sqrt(n);
            if (n > 0) for (int i = 0; i < v.length; i++) v[i] /= (float) n;
        }

        static double decay(Instant t, Instant now, Duration half) {
            if (t == null || half.isZero()) return 1.0;
            var elapsed = Duration.between(t, now);
            if (elapsed.isNegative()) return 1.0;
            double hours = elapsed.toMinutes() / 60.0;
            double halfHours = half.toMinutes() / 60.0;
            return Math.pow(0.5, hours / halfHours);
        }

        static void updateStat(Map<String, Stat> map, String k, double w) {
            var s = map.computeIfAbsent(k, x -> new Stat());
            s.count++;
            s.weightSum += w;
        }

        static List<PersonaTopic> rank(Map<String, Stat> stats, long days, int limit) {
            return stats.entrySet().stream()
                    .sorted(Comparator.comparingDouble((Map.Entry<String, Stat> e) -> e.getValue().weightSum).reversed())
                    .limit(limit)
                    .map(e -> {
                        var s = e.getValue();
                        return new PersonaTopic(e.getKey(), s.count, s.weightSum, s.count / Math.max(days, 1.0));
                    })
                    .toList();
        }

        static String formatTopic(PersonaTopic t) {
            return String.format(Locale.ROOT, "%s：%d次，%.2f次/天", t.name(), t.count(), t.dailyFrequency());
        }
    }
}
