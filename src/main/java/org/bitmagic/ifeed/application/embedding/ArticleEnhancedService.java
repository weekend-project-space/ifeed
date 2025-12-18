package org.bitmagic.ifeed.application.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleEnrichmentRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.service.ArticleEnrichmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleEnhancedService {

    private final ArticleEmbeddingRepository vectorStore;

    private final ArticleEnrichmentRepository aeRepository;

    private final FeedRepository feedRepository;

    private final ArticleRepository articleRepository;

    private final AiProviderProperties aiProviderProperties;

    private final ArticleEnrichmentService articleEnrichmentService;

    @Transactional
    public void enhanced(Article article) {
        if (aiProviderProperties.isEnabled()) {
            ArticleEnrichment enrichment = aeRepository.findById(article.getId()).orElseGet(() -> articleEnrichmentService.enrichArticle(article));
//            d级别不需要进行向量化
            if (!enrichment.getRating().equals(ArticleEnrichment.Rating.D)) {
                log.info("嵌入文章[{}]", article.getTitle());
                vectorStore.upsert(
                        article.getFeed().getId(),
                        title(article.getFeed().getId()),
                        article.getId(),
                        article.getTitle(),
                        article.getCategory(),
                        article.getTags(),
                        Optional.ofNullable(enrichment.getAiSummary()).orElse(article.getSummary()),
                        article.getContent(),
                        article.getPublishedAt()
                );
            }
            article.setEmbeddingGenerated(true);
            articleRepository.save(article);
        }
    }

    private String title(Integer feedId) {
        return feedRepository.findById(feedId).orElseThrow().getTitle();
    }
}
