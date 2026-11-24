package org.bitmagic.ifeed.domain.record;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import java.util.UUID;


@RegisterReflectionForBinding(ArticleTitle.class)
public record ArticleTitle(UUID uid, Long id, String title) {
}

