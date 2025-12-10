package org.bitmagic.ifeed.application.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
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

    private final FeedRepository feedRepository;

    private final ArticleRepository articleRepository;

    private final AiProviderProperties aiProviderProperties;

    @Transactional
    public void buildArticleEmbedding(Article article) {
        if (aiProviderProperties.isEnabled()) {
            repository.upsert(
                    article.getFeed().getId(),
                    title(article.getFeed().getId()),
                    article.getId(),
                    article.getTitle(),
                    article.getCategory(),
                    article.getTags(),
                    article.getSummary(),
                    article.getContent(),
                    article.getLink(),
                    article.getPublishedAt()
            );
            article.setEmbeddingGenerated(true);
            articleRepository.save(article);
        }
    }

    private String title(Integer feedId) {
        return feedRepository.findById(feedId).orElseThrow().getTitle();
    }
}
