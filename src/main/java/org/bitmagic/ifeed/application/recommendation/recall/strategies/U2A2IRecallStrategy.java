package org.bitmagic.ifeed.application.recommendation.recall.strategies;

import org.bitmagic.ifeed.application.recommendation.recall.core.RecallStrategy;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.InvertedIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户到属性再到物品（U2A2I）召回：根据用户偏好属性命中倒排索引。
 */
@Component
@ConditionalOnBean({UserPreferenceService.class, InvertedIndex.class})
public class U2A2IRecallStrategy implements RecallStrategy {

    private final UserPreferenceService preferenceService;
    private final InvertedIndex invertedIndex;
    private final int attributeLimit;

    public U2A2IRecallStrategy(UserPreferenceService preferenceService,
                               InvertedIndex invertedIndex,
                               @Value("${recall.u2a2i.attribute-limit:5}") int attributeLimit) {
        this.preferenceService = preferenceService;
        this.invertedIndex = invertedIndex;
        this.attributeLimit = attributeLimit;
    }

    @Override
    public StrategyId id() {
        return StrategyId.U2A2I;
    }

    @Override
    public List<ItemCandidate> recall(UserContext context, int limit) {
        List<UserPreferenceService.AttributePreference> attributes = preferenceService.topAttributes(context.userId(), attributeLimit);
        if (attributes.isEmpty()) {
            return List.of();
        }

        // 将属性得分映射到倒排索引，快速筛出候选物品
        List<ScoredId> hits = invertedIndex.query(attributes, limit);
        return hits.stream()
                .map(hit -> ItemCandidate.of(hit.id(), hit.score(), id()))
                .toList();
    }
}
