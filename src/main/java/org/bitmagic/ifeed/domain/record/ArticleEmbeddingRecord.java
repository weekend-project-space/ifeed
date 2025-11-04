package org.bitmagic.ifeed.domain.record;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder
public record ArticleEmbeddingRecord(
        @NonNull UUID id,
        @NonNull float[] embedding
) {
}
