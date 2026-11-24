package org.bitmagic.ifeed.infrastructure.ai.rerank.bge;

import org.bitmagic.ifeed.infrastructure.ai.rerank.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class BgeRerankerModel extends AbstractRerankerModel {

    private final RestClient restClient;

    public BgeRerankerModel(
            @Value("${app.ai.provider.reranker.base-url:http://localhost:8080}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    protected RerankResponse doRerank(RerankRequest request) {
        // 构造原始 API 请求
        BgeRerankRequest apiRequest = new BgeRerankRequest(
                request.query(),
                request.documents(),
                request.options().model(),
                "float"
        );

        // 调用本地 BGE 服务
        BgeRerankResponse apiResponse = restClient.post()
                .uri("/v1/reranking")
                .contentType(MediaType.APPLICATION_JSON)
                .body(apiRequest)
                .retrieve()
                .body(BgeRerankResponse.class);

        if (apiResponse == null || apiResponse.results() == null) {
            return new RerankResponse(List.of(), request.options().model(), new Usage(0, 0));
        }

        // 转换结果：index → document + 过滤 + topN
        List<RerankResult> results = apiResponse.results().stream()
                .filter(r -> request.options().minimumRelevanceScore() == null ||
                        r.relevanceScore() >= request.options().minimumRelevanceScore())
                .sorted((a, b) -> Double.compare(b.relevanceScore(), a.relevanceScore())) // 降序
                .limit(request.options().topN() != null ? request.options().topN() : Long.MAX_VALUE)
                .map(r -> new RerankResult(
                        request.documents().get(r.index()),
                        r.relevanceScore(),
                        r.index()
                ))
                .toList();

        return new RerankResponse(
                results,
                apiResponse.model(),
                new Usage(apiResponse.usage().promptTokens(), apiResponse.usage().totalTokens())
        );
    }
}