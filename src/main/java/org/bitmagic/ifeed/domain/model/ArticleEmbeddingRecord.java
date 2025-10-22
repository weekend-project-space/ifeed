package org.bitmagic.ifeed.domain.model;

import lombok.Builder;
import lombok.NonNull;

import java.util.Map;
import java.util.UUID;

@Builder
public record ArticleEmbeddingRecord(
        @NonNull UUID id,
        @NonNull float[] embedding
) {
}
