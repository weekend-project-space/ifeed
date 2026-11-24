package org.bitmagic.ifeed.infrastructure.ai.systemone;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SystemOneAnswer.Noul.class, name = "noul"),
        @JsonSubTypes.Type(value = SystemOneAnswer.Choice.class, name = "choice"),
        @JsonSubTypes.Type(value = SystemOneAnswer.Score.class, name = "score")
})
public sealed interface SystemOneAnswer {

    record Noul(Double noul) implements SystemOneAnswer {
        public Noul {
            requireFinite(noul, "noul");
        }
    }

    record Choice(String choice, Map<String, Double> probabilities, Double confidence) implements SystemOneAnswer {
        public Choice {
            Assert.hasText(choice, "Choice answer must not be blank");
            probabilities = copyProbabilities(probabilities);
            requireFinite(confidence, "confidence");
        }
    }

    record Score(Double score, Map<String, String> legend, Map<String, Double> probabilities,
                 Double confidence) implements SystemOneAnswer {
        public Score {
            requireFinite(score, "score");
            Assert.notEmpty(legend, "Score legend must not be empty");
            legend.forEach((key, description) -> {
                Assert.hasText(key, "Legend keys must not be blank");
                Assert.hasText(description, "Legend descriptions must not be blank");
            });
            legend = Collections.unmodifiableMap(new LinkedHashMap<>(legend));
            probabilities = copyProbabilities(probabilities);
            requireFinite(confidence, "confidence");
        }
    }

    private static void requireFinite(Double value, String name) {
        Assert.isTrue(value != null && Double.isFinite(value), "SystemOne " + name + " must be a finite number");
    }

    private static Map<String, Double> copyProbabilities(Map<String, Double> probabilities) {
        Assert.notEmpty(probabilities, "Answer probabilities must not be empty");
        probabilities.forEach((key, probability) -> {
            Assert.hasText(key, "Probability keys must not be blank");
            requireFinite(probability, "probability");
            Assert.isTrue(probability >= 0 && probability <= 1, "Probabilities must be between 0 and 1");
        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(probabilities));
    }
}
