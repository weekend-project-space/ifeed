package org.bitmagic.ifeed.infrastructure.ai.systemone;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.ai.model.ModelResponse;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SystemOneResponse(String model, Map<String, SystemOneAnswer> answers, SystemOneUsage usage)
        implements ModelResponse<SystemOneResult> {

    public SystemOneResponse {
        Assert.hasText(model, "SystemOne response must include a model");
        Assert.notEmpty(answers, "SystemOne response must include answers");
        answers.forEach((name, answer) -> {
            Assert.hasText(name, "Answer names must not be blank");
            Assert.notNull(answer, "Answers must not be null");
        });
        answers = Collections.unmodifiableMap(new LinkedHashMap<>(answers));
        Assert.notNull(usage, "SystemOne response must include token usage");
    }

    @Override
    @JsonIgnore
    public SystemOneResult getResult() {
        var entry = answers.entrySet().iterator().next();
        return new SystemOneResult(entry.getKey(), entry.getValue());
    }

    @Override
    @JsonIgnore
    public List<SystemOneResult> getResults() {
        return answers.entrySet().stream().map(entry -> new SystemOneResult(entry.getKey(), entry.getValue())).toList();
    }

    @Override
    @JsonIgnore
    public SystemOneResponseMetadata getMetadata() {
        return new SystemOneResponseMetadata(model, usage);
    }

    public SystemOneAnswer.Noul noul(String name) {
        return answer(name, SystemOneAnswer.Noul.class);
    }

    public SystemOneAnswer.Choice choice(String name) {
        return answer(name, SystemOneAnswer.Choice.class);
    }

    public SystemOneAnswer.Score score(String name) {
        return answer(name, SystemOneAnswer.Score.class);
    }

    private <Answer extends SystemOneAnswer> Answer answer(String name, Class<Answer> type) {
        SystemOneAnswer answer = answers.get(name);
        Assert.notNull(answer, "SystemOne answer not found: " + name);
        Assert.isTrue(type.isInstance(answer), "Unexpected SystemOne answer type for: " + name);
        return type.cast(answer);
    }
}
