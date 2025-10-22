package org.bitmagic.ifeed.service.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public void buildArticleEmbedding(Article article) {
        repository.upsert(
                article.getId(),
                article.getFeed().getId(),
                article.getFeed().getTitle(),
                article.getTitle(),
                article.getSummary(),
                article.getContent(),
                article.getLink(),
                article.getPublishedAt()
        );
    }
}
