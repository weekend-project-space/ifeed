package org.bitmagic.ifeed.infrastructure.ai.systemone;

import org.springframework.ai.model.ModelResult;
import org.springframework.ai.model.ResultMetadata;

public record SystemOneResult(String questionName, SystemOneAnswer answer)
        implements ModelResult<SystemOneAnswer>, ResultMetadata {

    @Override
    public SystemOneAnswer getOutput() {
        return answer;
    }

    @Override
    public ResultMetadata getMetadata() {
        return this;
    }
}
