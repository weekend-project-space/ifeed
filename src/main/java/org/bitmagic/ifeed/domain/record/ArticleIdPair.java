package org.bitmagic.ifeed.domain.record;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import java.util.UUID;


@RegisterReflectionForBinding(ArticleIdPair.class)
public record ArticleIdPair(UUID uid, Long id) {
}

