package org.bitmagic.ifeed.infrastructure.ai.systemone;

import org.springframework.ai.model.ModelOptions;
import org.springframework.ai.model.ModelRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CppSystemOneModel implements SystemOneModel {

    private final RestClient restClient;

    private final String model;

    @Autowired
    public CppSystemOneModel(
            @Value("${app.ai.provider.systemone.base-url:http://localhost:8083}") String baseUrl,
            @Value("${app.ai.provider.systemone.model:jev}") String model,
            @Value("${app.ai.provider.systemone.connect-timeout:PT5S}") Duration connectTimeout,
            @Value("${app.ai.provider.systemone.read-timeout:PT30S}") Duration readTimeout) {
        Assert.hasText(baseUrl, "SystemOne base URL must not be blank");
        Assert.hasText(model, "SystemOne model must not be blank");
        Assert.isTrue(!connectTimeout.isNegative() && !connectTimeout.isZero(), "Connect timeout must be positive");
        Assert.isTrue(!readTimeout.isNegative() && !readTimeout.isZero(), "Read timeout must be positive");
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.model = model;
    }

    public CppSystemOneModel(RestClient restClient, String model) {
        Assert.notNull(restClient, "RestClient must not be null");
        this.model = model;
        this.restClient = restClient;
    }

    @Override
    public SystemOneResponse call(SystemOneRequest request) {
        Assert.notNull(request, "SystemOne request must not be null");
        SystemOneResponse response = restClient.post()
                .uri("/v1/systemone")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SystemOneRequest0(model, request.state(), request.questions()))
                .retrieve()
                .body(SystemOneResponse.class);
        if (response == null) throw new IllegalStateException("SystemOne returned an empty response");
        if (!response.answers().keySet().equals(request.questions().keySet())) {
            throw new IllegalStateException("SystemOne answer names do not match the requested questions");
        }
        Map<String, SystemOneAnswer> orderedAnswers = new LinkedHashMap<>();
        request.questions().forEach((name, question) -> {
            SystemOneAnswer answer = response.answers().get(name);
            boolean matchingType = switch (question) {
                case SystemOneQuestion.Noul ignored -> answer instanceof SystemOneAnswer.Noul;
                case SystemOneQuestion.Choice choice -> answer instanceof SystemOneAnswer.Choice result
                        && choice.criteria().containsKey(result.choice())
                        && choice.criteria().keySet().equals(result.probabilities().keySet());
                case SystemOneQuestion.Score score -> answer instanceof SystemOneAnswer.Score result
                        && matchesScoreCriteria(score, result);
            };
            if (!matchingType) throw new IllegalStateException("SystemOne answer does not match question: " + name);
            orderedAnswers.put(name, answer);
        });
        return new SystemOneResponse(response.model(), orderedAnswers, response.usage());
    }

    private boolean matchesScoreCriteria(SystemOneQuestion.Score question, SystemOneAnswer.Score answer) {
        Map<String, String> expectedLegend = new LinkedHashMap<>();
        for (int index = 0; index < question.criteria().size(); index++) {
            expectedLegend.put(Integer.toString(index), question.criteria().get(index));
        }
        return expectedLegend.equals(answer.legend()) && expectedLegend.keySet().equals(answer.probabilities().keySet());
    }

    public record SystemOneRequest0(String model, String state,
                                    Map<String, SystemOneQuestion> questions) implements ModelRequest<String> {
        @Override
        public String getInstructions() {
            return state;
        }

        @Override
        public ModelOptions getOptions() {
            return null;
        }
    }
}
