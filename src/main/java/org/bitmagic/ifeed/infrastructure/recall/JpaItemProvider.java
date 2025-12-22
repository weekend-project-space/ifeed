package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemProvider;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.bitmagic.ifeed.domain.repository.ArticleEnrichmentRepository;
import org.bitmagic.ifeed.domain.spec.ArticleEnrichmentSpec;
import org.bitmagic.ifeed.infrastructure.score.ScoreMerger;
import org.springframework.cache.annotation.Cacheable;
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

    private final ArticleEnrichmentRepository articleEnrichmentRepository;

    //   优先查询用户订阅内容
    @Cacheable(cacheNames = "ITEMS", key = "#userContext.userId() + '_' + #type.name() + '_' + #k", unless = "#result == null")
    @Override
    public List<ScoredId> ls(UserContext userContext, ScoredLsType type, Integer k) {
        long currentTimeMillis = System.currentTimeMillis();
        int pageSize = k / 3 * 2;
        PageRequest pageable = ScoredLsType.LATEST.equals(type) ? PageRequest.of(0, pageSize, Sort.by(Sort.Order.desc("id"))) : PageRequest.ofSize(pageSize);
        List<ArticleEnrichment> content = new ArrayList<>(articleEnrichmentRepository.findByUser(userContext.getUserId(), pageable).getContent());
        if (ScoredLsType.RANDOM.equals(type) || ScoredLsType.HOT.equals(type)) {
            content.addAll(articleEnrichmentRepository.findByUser(null, PageRequest.ofSize(k / 3)).getContent());
        }
        log.debug("{} time: {}", type.name(), System.currentTimeMillis() - currentTimeMillis);
        List<ScoredId> scoredIds = content.stream().map(article -> new ScoredId(article.getId(), ScoreMerger.gradeToScore(article.getRating().name()), Map.of("rating", article.getRating()))).toList();
        return scoredIds;
    }
}
