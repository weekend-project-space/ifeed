package org.bitmagic.ifeed.infrastructure.ai.systemone;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SystemOneQuestion.Noul.class, name = "noul"),
        @JsonSubTypes.Type(value = SystemOneQuestion.Choice.class, name = "choice"),
        @JsonSubTypes.Type(value = SystemOneQuestion.Score.class, name = "score")
})
public sealed interface SystemOneQuestion {

    String instructions();

    static Noul noul(String instructions) {
        return new Noul(instructions);
    }

    static Choice choice(String instructions, Map<String, String> criteria) {
        return new Choice(instructions, criteria);
    }

    static Score score(String instructions, List<String> criteria) {
        return new Score(instructions, criteria);
    }

    record Noul(String instructions) implements SystemOneQuestion {
        public Noul {
            Assert.hasText(instructions, "Question instructions must not be blank");
        }
    }

    record Choice(String instructions, Map<String, String> criteria) implements SystemOneQuestion {
        public Choice {
            Assert.hasText(instructions, "Question instructions must not be blank");
            Assert.notEmpty(criteria, "Choice criteria must not be empty");
            criteria.forEach((key, description) -> {
                Assert.hasText(key, "Choice keys must not be blank");
                Assert.hasText(description, "Choice descriptions must not be blank");
            });
            criteria = Collections.unmodifiableMap(new LinkedHashMap<>(criteria));
        }
    }

    record Score(String instructions, List<String> criteria) implements SystemOneQuestion {
        public Score {
            Assert.hasText(instructions, "Question instructions must not be blank");
            Assert.notEmpty(criteria, "Score criteria must not be empty");
            criteria.forEach(description -> Assert.hasText(description, "Score descriptions must not be blank"));
            criteria = List.copyOf(criteria);
        }
    }
}
