package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于 Mongo 用户行为数据的序列存储，从阅读历史提取最近的文章 ID。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoSequenceStore implements SequenceStore {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleRepository articleRepository;

    @Override
    public List<UserInteraction> recentInteractions(Integer userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }

        return userBehaviorRepository.findById(userId.toString())
                .map(doc -> extractRecentInteractions(doc, limit))
                .orElse(List.of());
    }

    private List<UserInteraction> extractRecentInteractions(UserBehaviorDocument document, int limit) {
        if (document.getReadHistory() == null || document.getReadHistory().isEmpty()) {
            return List.of();
        }

        List<UserBehaviorDocument.ArticleRef> sorted = document.getReadHistory().stream()
                .filter(ref -> ref.getArticleId() != null)
                .sorted(Comparator.comparing(UserBehaviorDocument.ArticleRef::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit * 3L) // 多取一些防止后续映射损失
                .toList();

        if (sorted.isEmpty()) {
            return List.of();
        }

        List<UUID> uuids = sorted.stream()
                .map(UserBehaviorDocument.ArticleRef::getArticleId)
                .map(this::safeUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uuids.isEmpty()) {
            return List.of();
        }

        Map<UUID, Article> articleMap = articleRepository.findByUidIn(uuids).stream()
                .collect(Collectors.toMap(Article::getUid, article -> article));

        Map<String, Long> interactionCounts = document.getInteractionHistory() == null
                ? Map.of()
                : document.getInteractionHistory().stream()
                .filter(inter -> inter.getArticleId() != null)
                .collect(Collectors.groupingBy(UserBehaviorDocument.Interaction::getArticleId, Collectors.counting()));

        List<UserInteraction> results = new ArrayList<>();
        int index = 0;
        for (UserBehaviorDocument.ArticleRef ref : sorted) {
            UUID uuid = safeUuid(ref.getArticleId());
            if (uuid == null) {
                continue;
            }
            Article article = articleMap.get(uuid);
            if (article == null || article.getId() == null) {
                continue;
            }
            double recencyWeight = Math.exp(-index / Math.max(1.0, (double) limit));
            long interactionCount = interactionCounts.getOrDefault(ref.getArticleId(), 0L);
            double weight = recencyWeight * (1.0 + interactionCount);
            double duration = interactionCount * 30.0; // 估算互动总时长，单位秒
            Instant timestamp = ref.getTimestamp() != null ? ref.getTimestamp() : Instant.EPOCH;
            results.add(new UserInteraction(article.getId(), duration, weight, timestamp));
            index++;
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    private UUID safeUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            log.debug("Ignore invalid articleId {} in user behavior", value);
            return null;
        }
    }
}
