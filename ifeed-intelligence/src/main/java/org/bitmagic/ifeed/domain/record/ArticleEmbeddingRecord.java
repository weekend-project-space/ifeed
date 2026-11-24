package org.bitmagic.ifeed.domain.record;

import lombok.Builder;
import lombok.NonNull;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@RegisterReflectionForBinding(ArticleEmbeddingRecord.class)
@Builder
public record ArticleEmbeddingRecord(
        @NonNull Long id,
        @NonNull float[] embedding
) {
}
