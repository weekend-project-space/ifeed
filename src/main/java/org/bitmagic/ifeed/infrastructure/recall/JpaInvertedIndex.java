package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.spi.InvertedIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.config.properties.SearchRetrievalProperties;
import org.bitmagic.ifeed.infrastructure.retrieval.DocScore;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalPipeline;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.TextSearchRetrievalHandler;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.MultiChannelRetrievalPipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 JPA 的倒排索引实现，支持按类目或作者获取最新文章集合。
 */
@Component
@RequiredArgsConstructor
public class JpaInvertedIndex implements InvertedIndex {

    private final RetrievalPipeline retrievalPipeline;


    @Autowired
    public JpaInvertedIndex(TextSearchRetrievalHandler textSearchRetrievalHandler, SearchRetrievalProperties properties) {
        this.retrievalPipeline = new MultiChannelRetrievalPipeline(properties.getFreshnessTimeWeight(), properties.getFreshnessLambda())
                .addHandler(textSearchRetrievalHandler, 1);
    }

    @Override
    public List<ScoredId> query(List<UserPreferenceService.AttributePreference> attributes, int k) {
//        String attrs = attributes.stream().map(UserPreferenceService.AttributePreference::attributeValue).collect(Collectors.joining(" OR "));
//        List<DocScore> scores = retrievalPipeline.execute(RetrievalContext.builder().includeGlobal(true).query(attrs).topK(k).build());
//        return scores.stream().map(docScore -> new ScoredId(docScore.docId(), docScore.score(), Map.of())).toList();
        if (attributes == null || attributes.isEmpty()) {
            return List.of();
        }

        // 按权重排序，权重高的优先
        List<UserPreferenceService.AttributePreference> sorted = attributes.stream()
                .sorted(Comparator.comparingDouble(UserPreferenceService.AttributePreference::weight).reversed())
                .toList();

        double normalizationFactor = 1 / (!sorted.isEmpty() ? sorted.iterator().next().weight() : 1);

        // 分别查询每个属性，然后加权合并
        Map<Long, Double> weightedScores = new HashMap<>();
        Map<Long, Map<String, Object>> metadataMap = new HashMap<>();

        for (UserPreferenceService.AttributePreference attr : sorted) {
            RetrievalContext context = RetrievalContext.builder()
                    .includeGlobal(true)
                    .query(attr.attributeValue())
                    .topK(k)
                    .build();

            List<DocScore> scores = retrievalPipeline.execute(context);

            for (DocScore score : scores) {
                // 加权累加分数
                double weightedScore = score.score() * attr.weight() * normalizationFactor;
                weightedScores.merge(score.docId(), weightedScore, Double::sum);

                // 保存元数据
                metadataMap.putIfAbsent(score.docId(), Map.of());
            }
        }

        // 排序并返回 top-k
        return weightedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(k)
                .map(e -> new ScoredId(
                        e.getKey(),
                        e.getValue(),
                        metadataMap.getOrDefault(e.getKey(), Map.of())
                ))
                .toList();
    }

}
