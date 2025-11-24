package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemProvider;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.infrastructure.FreshnessCalculator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yangrd
 * @date 2025/11/10
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class JpaItemProvider implements ItemProvider {

    private final ArticleRepository articleRepository;

    private final FreshnessCalculator freshnessCalculator;

    @Cacheable(cacheNames = "ITEMS", key = "#userContext.userId() + '_' + #type.name() + '_' + #k", unless = "#result == null")
    @Override
    public List<ScoredId> ls(UserContext userContext, ScoredLsType type, Integer k) {
        long currentTimeMillis = System.currentTimeMillis();
        PageRequest pageable = ScoredLsType.LATEST.equals(type) ? PageRequest.of(0, k, Sort.by(Sort.Order.desc("id"))) : PageRequest.ofSize(k / 2);
        List<ArticleSummaryView> all = new ArrayList<>(articleRepository.searchArticleSummaries("", null, pageable).getContent());
        all.addAll(articleRepository.searchArticleSummaries("", userContext.getUserId(), pageable).getContent());
        log.debug("{} time: {}", type.name(), System.currentTimeMillis() - currentTimeMillis);
        List<ScoredId> scoredIds = all.stream().map(article -> new ScoredId(article.articleId(), freshnessCalculator.calculate(article.publishedAt()), Map.of("title", article.title()))).toList();
        return scoredIds;
    }
}
