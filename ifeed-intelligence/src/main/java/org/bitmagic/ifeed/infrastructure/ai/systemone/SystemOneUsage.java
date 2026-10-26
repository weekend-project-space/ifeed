package org.bitmagic.ifeed.infrastructure.ai.systemone;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

public record SystemOneUsage(@JsonProperty("input_tokens") Integer inputTokens,
                             @JsonProperty("output_tokens") Integer outputTokens) {
    public SystemOneUsage {
        Assert.isTrue(inputTokens != null && inputTokens >= 0, "Input tokens must not be missing or negative");
        Assert.isTrue(outputTokens != null && outputTokens >= 0, "Output tokens must not be missing or negative");
    }
}
