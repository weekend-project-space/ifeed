package org.bitmagic.ifeed.infrastructure.ai.systemone;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.ai.model.ModelOptions;
import org.springframework.ai.model.ModelRequest;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record SystemOneRequest(String state, Map<String, SystemOneQuestion> questions) implements ModelRequest<String> {

    public SystemOneRequest {
        Assert.hasText(state, "SystemOne state must not be blank");
        Assert.notEmpty(questions, "SystemOne questions must not be empty");
        questions.forEach((name, question) -> {
            Assert.hasText(name, "Question names must not be blank");
            Assert.notNull(question, "Questions must not be null");
        });
        questions = Collections.unmodifiableMap(new LinkedHashMap<>(questions));
    }

    @Override
    @JsonIgnore
    public String getInstructions() {
        return state;
    }

    @Override
    @JsonIgnore
    public ModelOptions getOptions() {
        return null;
    }
}
