package org.bitmagic.ifeed.domain.record;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import java.time.Instant;

/**
 * @author yangrd
 * @date 2025/12/6
 **/

@RegisterReflectionForBinding(ArticleContent.class)
public record ArticleContent(Long id, String content, Instant publishedAt) {
}
