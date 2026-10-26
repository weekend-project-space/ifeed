package org.bitmagic.ifeed.infrastructure.ai.systemone;

import org.springframework.ai.model.AbstractResponseMetadata;
import org.springframework.ai.model.ResponseMetadata;

public final class SystemOneResponseMetadata extends AbstractResponseMetadata implements ResponseMetadata {
    public SystemOneResponseMetadata(String model, SystemOneUsage usage) {
        map.put("model", model);
        map.put("usage", usage);
    }
}
