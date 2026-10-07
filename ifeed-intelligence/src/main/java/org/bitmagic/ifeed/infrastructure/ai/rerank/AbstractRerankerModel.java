package org.bitmagic.ifeed.infrastructure.ai.rerank;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

/**
 * @author yangrd
 * @date 2025/11/11
 **/
@Slf4j
public abstract class AbstractRerankerModel implements RerankerModel {

    @Override
    public RerankResponse call(RerankRequest request) {
        log.debug("Rerank: query='{}', docs={}", request.query(), request.documents().size());

        RerankResponse response = doRerank(request);

        var filtered = response.results().stream()
                .filter(r -> request.options().minimumRelevanceScore() == null ||
                        r.relevanceScore() >= request.options().minimumRelevanceScore())
                .sorted(Comparator.comparingDouble(RerankResult::relevanceScore).reversed())
                .limit(request.options().topN() != null ? request.options().topN() : Long.MAX_VALUE)
                .toList();

        return new RerankResponse(filtered, response.model(), response.usage());
    }

    protected abstract RerankResponse doRerank(RerankRequest request);
}