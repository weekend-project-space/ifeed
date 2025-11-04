package org.bitmagic.ifeed.application.recommendation.recall.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 召回引擎的请求参数，封装用户、场景、召回数量以及过滤器等信息。
 */
public record RecallRequest(Integer userId,
                            String scene,
                            int topK,
                            Map<String, Object> filters,
                            boolean debug,
                            Instant requestTime) {

    public RecallRequest {
        Objects.requireNonNull(userId, "userId");
        scene = scene == null ? "default" : scene;
        topK = Math.max(topK, 1);
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        requestTime = requestTime == null ? Instant.now() : requestTime;
    }

    public static RecallRequest of(Integer userId, String scene, int topK) {
        return new RecallRequest(userId, scene, topK, Map.of(), false, Instant.now());
    }
}
