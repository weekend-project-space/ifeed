package org.bitmagic.ifeed.application.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.bitmagic.ifeed.config.properties.RecommendationProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.UserEmbedding;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 用户画像向量构建服务，基于用户近期交互序列和时间衰减权重。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEmbeddingService {

    private static final int EMBEDDING_DIMENSION = 1024;

    private final SequenceStore sequenceStore;
    private final ArticleEmbeddingRepository articleEmbeddingRepository;
    private final ArticleRepository articleRepository;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final RecommendationProperties recommendationProperties;

    @Transactional
    public Optional<UserEmbedding> rebuildUserEmbedding(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }

        List<SequenceStore.UserInteraction> interactions = sequenceStore.recentInteractions(
                userId,
                Math.max(1, recommendationProperties.getRecentBehaviorLimit())
        );

        if (interactions.isEmpty()) {
            userEmbeddingRepository.deleteAllByIdInBatch(List.of(userId));
            return Optional.empty();
        }

        Set<Long> articleIds = interactions.stream()
                .map(SequenceStore.UserInteraction::itemId)
                .collect(Collectors.toSet());

        if (articleIds.isEmpty()) {
            userEmbeddingRepository.deleteAllByIdInBatch(List.of(userId));
            return Optional.empty();
        }

        Map<Long, ArticleEmbeddingRecord> embeddingById = articleEmbeddingRepository.findAllByIds(articleIds)
                .stream()
                .collect(Collectors.toMap(ArticleEmbeddingRecord::id, Function.identity()));

        if (embeddingById.isEmpty()) {
            log.debug("No article embeddings found for user {}", userId);
            userEmbeddingRepository.deleteAllByIdInBatch(List.of(userId));
            return Optional.empty();
        }

        List<Integer> articleIdInts = articleIds.stream()
                .map(Math::toIntExact)
                .toList();

        Map<Long, Article> articleById = StreamSupport.stream(articleRepository.findAllById(articleIdInts).spliterator(), false)
                .collect(Collectors.toMap(Article::getId, Function.identity()));

        Instant now = Instant.now();
        float[] aggregated = new float[EMBEDDING_DIMENSION];
        double totalWeight = 0.0;
        List<ProfileItem> profileItems = new ArrayList<>();

        for (SequenceStore.UserInteraction interaction : interactions) {
            ArticleEmbeddingRecord record = embeddingById.get(interaction.itemId());
            if (record == null || record.embedding() == null || record.embedding().length == 0) {
                continue;
            }

            float[] source = ensureDimension(record.embedding());
            double baseWeight = interaction.weight() > 0 ? interaction.weight() : recommendationProperties.getReadWeight();
            double decay = recencyDecay(interaction.timestamp(), now);
            double finalWeight = baseWeight * decay;

            if (finalWeight == 0.0) {
                continue;
            }

            for (int i = 0; i < aggregated.length; i++) {
                aggregated[i] += source[i] * finalWeight;
            }
            totalWeight += Math.abs(finalWeight);

            Article article = articleById.get(interaction.itemId());
            if (article != null && finalWeight > 0.0) {
                profileItems.add(new ProfileItem(article, interaction.timestamp(), finalWeight));
            }
        }

        if (totalWeight == 0.0) {
            log.debug("User {} interactions produced no usable weight", userId);
            userEmbeddingRepository.deleteAllByIdInBatch(List.of(userId));
            return Optional.empty();
        }

        normalize(aggregated);

        String profileText = buildProfileText(profileItems, now);
        if (!StringUtils.hasText(profileText)) {
            profileText = StringUtils.hasText(recommendationProperties.getDefaultProfile())
                    ? recommendationProperties.getDefaultProfile()
                    : "";
        }

        UserEmbedding embedding = UserEmbedding.builder()
                .userId(userId)
                .embedding(aggregated)
                .content(profileText)
                .updatedAt(Instant.now())
                .build();

        userEmbeddingRepository.deleteAllByIdInBatch(List.of(userId));
        userEmbeddingRepository.save(embedding);
        return Optional.of(embedding);
    }

    private float[] ensureDimension(float[] source) {
        if (source == null || source.length == 0) {
            return null;
        }
        if (source.length == EMBEDDING_DIMENSION) {
            return source.clone();
        }
        float[] target = new float[EMBEDDING_DIMENSION];
        System.arraycopy(source, 0, target, 0, Math.min(source.length, EMBEDDING_DIMENSION));
        return target;
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
            vector[i] = (float) (vector[i] / norm);
        }
    }

    private double recencyDecay(Instant timestamp, Instant now) {
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

    private String buildProfileText(List<ProfileItem> items, Instant now) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("用户近期浏览兴趣：");
        items.stream()
                .sorted(Comparator
                        .comparingDouble(ProfileItem::weight).reversed()
                        .thenComparing(ProfileItem::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .forEach(item -> {
                    Article article = item.article();
                    builder.append(article.getTitle());
                    if (StringUtils.hasText(article.getCategory())) {
                        builder.append(" ").append(article.getCategory());
                    }
                    if (StringUtils.hasText(article.getTags())) {
                        builder.append(" ").append(article.getTags());
                    }
                    if (item.timestamp() != null) {
                        long hours = Duration.between(item.timestamp(), now).toHours();
                        builder.append(" ").append(Math.max(0, hours)).append("小时前");
                    }
                    builder.append(" | ");
                });

        if (builder.length() >= 3) {
            builder.setLength(builder.length() - 3);
        }
        return builder.toString();
    }

    private record ProfileItem(Article article, Instant timestamp, double weight) {
    }
}
