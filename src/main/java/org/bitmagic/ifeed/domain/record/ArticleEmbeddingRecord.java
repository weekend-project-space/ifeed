package org.bitmagic.ifeed.domain.record;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record ArticleEmbeddingRecord(
        @NonNull Long id,
        @NonNull float[] embedding
) {
}
