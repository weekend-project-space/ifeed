package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.domain.repository.UserLikeRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.bitmagic.ifeed.infrastructure.recall.data.UserBehaviorDataAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PgUserSequenceStore implements SequenceStore {

    private final UserReadHistoryRepository historyRepository;
    private final UserBehaviorDataAccessor dataAccessor;
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

    @Value("${recall.sequence.interaction-weight-base:0.1}")
    private double interactionWeightBase;

    @Value("${recall.sequence.collection-bonus:2.0}")
    private double collectionBonus;

    @Value("${recall.sequence.like-bonus:2.0}")
    private double likeBonus;

    @Value("${recall.sequence.estimated-interaction-duration:30.0}")
    private double estimatedInteractionDuration;

    @Value("${recall.sequence.use-logarithmic-interaction:true}")
    private boolean useLogarithmicInteraction;

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
        Map<String, Long> interactionCounts = dataAccessor.getUserBehavior(userId)
                .map(dataAccessor::buildInteractionCountMap)
                .orElseGet(Map::of);
        List<UserInteraction> results = new ArrayList<>();
        for (var entry : history) {
            var article = entry.article();
            long interactionCount = interactionCounts.getOrDefault(article.id().toString(), 0L);
            double interactionWeight = useLogarithmicInteraction
                    ? Math.log1p(interactionCount) * interactionWeightBase
                    : interactionCount * interactionWeightBase;
            double recencyWeight = Math.exp(-results.size() / (recencyDecayFactor * limit));
            double preferenceWeight = (collectedIds.contains(article.articleId()) ? collectionBonus : 0.0)
                    + (likedIds.contains(article.articleId()) ? likeBonus : 0.0);
            results.add(new UserInteraction(article.articleId(), article.title(),
                    interactionCount * estimatedInteractionDuration,
                    recencyWeight * (1.0 + interactionWeight + preferenceWeight), entry.readAt()));
        }
        return results;
    }
}
