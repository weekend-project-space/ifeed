package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.spi.InvertedIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.config.properties.SearchRetrievalProperties;
import org.bitmagic.ifeed.infrastructure.retrieval.DocScore;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalPipeline;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.Bm25RetrievalHandler;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.MultiChannelRetrievalPipeline;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

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
    public JpaInvertedIndex(VectorStoreTurbo vectorStore, NamedParameterJdbcTemplate  jdbcTemplate, SearchRetrievalProperties properties) {
        this.retrievalPipeline = new MultiChannelRetrievalPipeline(properties.getFreshnessTimeWeight(), properties.getFreshnessLambda())
                .addHandler(new Bm25RetrievalHandler(jdbcTemplate), properties.getBm25Weight());
//        .addHandler(new VectorRetrievalHandler(vectorStore, properties.getSimilarityThreshold()), properties.getVectorWeight());
    }

    @Override
    public List<ScoredId> query(List<UserPreferenceService.AttributePreference> attributes, int k) {
        String attrs = attributes.stream().map(UserPreferenceService.AttributePreference::attributeValue).collect(Collectors.joining(" OR "));
        List<DocScore> scores = retrievalPipeline.execute(RetrievalContext.builder().includeGlobal(true).query(attrs).topK(k).build());
        return scores.stream().map(docScore -> new ScoredId(docScore.docId(), docScore.score(), Map.of())).toList();
    }

}
