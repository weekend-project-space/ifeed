package org.bitmagic.ifeed.infrastructure.text.search;

import java.util.Map;

/**
 * 文档实体
 */
public record Document(
        Long id,
        Integer feedId,
        String content,
        Map<String, Object> metadata
) {
    public Document {
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    public Document(Long id, Integer feedId, String content) {
        this(id, feedId, content, Map.of());
    }
}
