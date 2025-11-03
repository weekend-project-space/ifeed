package org.bitmagic.ifeed.service.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.AiProviderProperties;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleEmbeddingService {

    private final ArticleEmbeddingRepository repository;

    private final AiProviderProperties aiProviderProperties;

    @Transactional
    public void buildArticleEmbedding(Article article) {
        if (aiProviderProperties.isEnabled()) {
            repository.upsert(
                    article.getFeed().getUid(),
                    article.getFeed().getTitle(),
                    article.getUid(),
                    article.getTitle(),
                    article.getCategory(),
                    article.getTags(),
                    article.getSummary(),
                    article.getContent(),
                    article.getLink(),
                    article.getPublishedAt()
            );
        }
    }
}
