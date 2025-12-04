package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.core.AdaptiveScoreMapper;
import org.bitmagic.ifeed.application.recommendation.recall.spi.InvertedIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.config.properties.SearchRetrievalProperties;
import org.bitmagic.ifeed.infrastructure.FreshnessCalculator;
import org.bitmagic.ifeed.infrastructure.retrieval.DocScore;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalPipeline;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.MultiChannelRetrievalPipeline;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.TextSearchRetrievalHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 JPA 的倒排索引实现，支持按类目或作者获取最新文章集合。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaInvertedIndex implements InvertedIndex {

    private final RetrievalPipeline retrievalPipeline;

    private final FreshnessCalculator freshnessCalculator;

    private final AdaptiveScoreMapper adaptiveScoreMapper = new AdaptiveScoreMapper();


    @Autowired
    public JpaInvertedIndex(TextSearchRetrievalHandler textSearchRetrievalHandler, FreshnessCalculator freshnessCalculator, SearchRetrievalProperties properties) {
        this.retrievalPipeline = new MultiChannelRetrievalPipeline(properties.getFreshnessTimeWeight(), properties.getFreshnessLambda())
                .addHandler(textSearchRetrievalHandler, 1);
        this.freshnessCalculator = freshnessCalculator;
    }

    @Override
    public List<ScoredId> query(List<UserPreferenceService.AttributePreference> attributes, int k) {
        if (attributes == null || attributes.isEmpty()) {
            return List.of();
        }

        // 按权重排序，权重高的优先
        List<UserPreferenceService.AttributePreference> sorted = attributes.stream()
                .sorted(Comparator.comparingDouble(UserPreferenceService.AttributePreference::weight).reversed())
                .toList();


        // 分别查询每个属性，然后加权合并
        Map<Long, Double> weightedScores = new HashMap<>();
        Map<Long, Map<String, Object>> metadataMap = new HashMap<>();

        int topK = ((Double) (k / attributes.size() * 1.5)).intValue();

        for (UserPreferenceService.AttributePreference attr : sorted) {
            RetrievalContext context = RetrievalContext.builder()
                    .includeGlobal(true)
                    .query(attr.attributeValue())
                    .topK(topK)
                    .build();

            List<DocScore> scores = retrievalPipeline.execute(context);

            for (DocScore score : scores) {
                double wa = attr.attributeKey().equals("feedTitle") ? 0.5 : 0.1;
                // 加权累加分数
                double weightedScore = freshnessCalculator.calculate(score.pubDate()) * wa + (score.score() * adaptiveScoreMapper.map(Math.max(attr.weight(), 0.1))) * (1 - wa);
                weightedScores.merge(score.docId(), weightedScore, Double::sum);
                log.debug("atrr:{} doc:{} score:{} ", attr.attributeValue(), score.docId(), weightedScore);
                // 保存元数据
                metadataMap.putIfAbsent(score.docId(), Map.of("source", attr, "docScore", score));
            }
        }

        // 排序并返回 top-k
        List<ScoredId> topList = weightedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(k)
                .map(e -> new ScoredId(
                        e.getKey(),
                        adaptiveScoreMapper.map(e.getValue()),
                        metadataMap.getOrDefault(e.getKey(), Map.of())
                ))
                .toList();
        return topList;
    }

}
