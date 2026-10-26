package org.bitmagic.ifeed.infrastructure.ai.systemone;

import org.springframework.ai.model.Model;

public interface SystemOneModel extends Model<SystemOneRequest, SystemOneResponse> {
    @Override
    SystemOneResponse call(SystemOneRequest request);
}
