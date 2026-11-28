package org.bitmagic.ifeed.application.article;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ArticleEnrichResponse;
import org.bitmagic.ifeed.application.embedding.ArticleEnhancedService;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleEnrichmentApplicationService {
    private final ArticleService articleService;
    private final ArticleEnhancedService articleEnhancedService;

    public ArticleEnrichResponse enrich(UUID articleId) {
        var article = articleService.getArticle(articleId);
        var enrichment = articleEnhancedService.enrichSync(article);
        return new ArticleEnrichResponse(enrichment.getAiSummary(), enrichment.getMindMap());
    }

}
