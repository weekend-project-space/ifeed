package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.domain.repository.UserLikeRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PgUserSequenceStore implements SequenceStore {

    private final UserReadHistoryRepository historyRepository;
    private final UserCollectionRepository collectionRepository;
    private final UserLikeRepository likeRepository;

    @Value("${recall.sequence.window-days:30}")
    private int windowDays;

    @Value("${recall.sequence.lookback:200}")
    private int lookback;

    @Value("${recall.sequence.fetch-multiplier:2}")
    private int fetchMultiplier;

    @Value("${recall.sequence.max-fetch-limit:1000}")
    private int maxFetchLimit;

    @Value("${recall.sequence.recency-decay-factor:1.0}")
    private double recencyDecayFactor;

    @Value("${recall.sequence.collection-bonus:2.0}")
    private double collectionBonus;

    @Value("${recall.sequence.like-bonus:2.0}")
    private double likeBonus;

    @Override
    public List<UserInteraction> recentInteractions(Integer userId, int limit) {
        if (userId == null || userId <= 0 || limit <= 0) {
            return List.of();
        }

        int fetchLimit = (int) Math.min(Math.max((long) limit * fetchMultiplier, lookback), maxFetchLimit);
        Instant cutoff = windowDays > 0 ? Instant.now().minus(Duration.ofDays(windowDays)) : null;
        var history = historyRepository.findRecent(userId.longValue(), cutoff, fetchLimit).stream()
                .limit(limit).toList();
        if (history.isEmpty()) {
            return List.of();
        }
        var articleIds = history.stream().map(entry -> entry.article().articleId()).toList();
        var collectedIds = collectionRepository.findArticleIds(userId.longValue(), articleIds);
        var likedIds = likeRepository.findArticleIds(userId.longValue(), articleIds);
        List<UserInteraction> results = new ArrayList<>();
        for (var entry : history) {
            var article = entry.article();
            double recencyWeight = Math.exp(-results.size() / (recencyDecayFactor * limit));
            double preferenceWeight = (collectedIds.contains(article.articleId()) ? collectionBonus : 0.0)
                    + (likedIds.contains(article.articleId()) ? likeBonus : 0.0);
            results.add(new UserInteraction(article.articleId(), article.title(), 0.0,
                    recencyWeight * (1.0 + preferenceWeight), entry.readAt()));
        }
        return results;
    }
}
