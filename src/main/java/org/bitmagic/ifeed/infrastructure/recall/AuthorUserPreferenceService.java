package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于近期阅读历史作者的用户偏好服务，用于推荐常看的作者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorUserPreferenceService implements UserPreferenceService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleRepository articleRepository;

    @Value("${recall.u2a.author-window-days:30}")
    private int windowDays;

    @Value("${recall.u2a.author-lookback:200}")
    private int lookback;

    @Override
    public List<AttributePreference> topAttributes(Integer userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }

        return userBehaviorRepository.findById(userId.toString())
                .map(document -> computeTopAuthors(document, limit))
                .orElse(List.of());
    }

    private List<AttributePreference> computeTopAuthors(UserBehaviorDocument document, int limit) {
        List<UserBehaviorDocument.ArticleRef> history = document.getReadHistory();
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        Instant cutoff = resolveCutoff();
        int fetchLimit = Math.max(Math.max(limit, lookback), limit * 5);

        List<UserBehaviorDocument.ArticleRef> recent = history.stream()
                .filter(ref -> ref.getArticleId() != null)
                .filter(ref -> cutoff == null || ref.getTimestamp() == null || !ref.getTimestamp().isBefore(cutoff))
                .sorted(Comparator.comparing(UserBehaviorDocument.ArticleRef::getTimestamp,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(fetchLimit)
                .toList();

        if (recent.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = recent.stream()
                .map(UserBehaviorDocument.ArticleRef::getArticleId)
                .map(this::safeUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, Article> articleMap = articleRepository.findByUidIn(ids).stream()
                .collect(Collectors.toMap(Article::getUid, article -> article));

        Map<String, Long> counts = new LinkedHashMap<>();
        for (UserBehaviorDocument.ArticleRef ref : recent) {
            UUID uuid = safeUuid(ref.getArticleId());
            if (uuid == null) {
                continue;
            }

            Article article = articleMap.get(uuid);
            if (article == null) {
                continue;
            }

            String author = normalizeAuthor(article.getAuthor());
            if (author == null) {
                continue;
            }

            counts.merge(author, 1L, Long::sum);
        }

        if (counts.isEmpty()) {
            return List.of();
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new AttributePreference("author", entry.getKey(), entry.getValue()))
                .toList();
    }

    private Instant resolveCutoff() {
        if (windowDays <= 0) {
            return null;
        }
        return Instant.now().minus(Duration.ofDays(windowDays));
    }

    private UUID safeUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            log.debug("Ignore invalid articleId {} in read history", value);
            return null;
        }
    }

    private String normalizeAuthor(String author) {
        if (author == null) {
            return null;
        }
        String trimmed = author.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
