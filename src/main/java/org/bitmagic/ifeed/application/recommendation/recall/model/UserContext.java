package org.bitmagic.ifeed.application.recommendation.recall.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;

/**
 * 召回阶段使用的用户上下文信息，携带最近行为、场景以及动态属性。
 */
public record UserContext(Integer userId,
                          String scene,
                          List<SequenceStore.UserInteraction> interactions,
                          Map<String, Object> attributes,
                          Instant requestTime) {

    public UserContext {
        Objects.requireNonNull(userId, "userId");
        scene = scene == null ? "default" : scene;
        interactions = interactions == null ? List.of() : List.copyOf(interactions);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestTime = requestTime == null ? Instant.now() : requestTime;
    }

    public Integer getUserId() {
        return userId;
    }

    public Object attribute(String key) {
        return attributes.get(key);
    }

    public Set<Long> recentItemIds() {
        return interactions.stream()
                .map(SequenceStore.UserInteraction::itemId)
                .collect(Collectors.toSet());
    }
}
