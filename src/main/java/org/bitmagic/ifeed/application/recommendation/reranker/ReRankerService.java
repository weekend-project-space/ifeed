package org.bitmagic.ifeed.application.recommendation.reranker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.domain.record.ArticleContent;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.infrastructure.QualityScorer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yangrd
 * @date 2025/12/6
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class ReRankerService {

    private final QualityScorer qualityScorer = new QualityScorer();

    private final ArticleRepository articleRepository;

    public List<ItemCandidate> reranker(UserContext userContext, List<ItemCandidate> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemCandidate> candidates = deduplication(userContext, items);
        Map<Long, ArticleContent> id2Content = articleRepository.findArticleContentByIds(candidates.stream().map(ItemCandidate::itemId).toList()).stream().collect(Collectors.toMap(ArticleContent::id, Function.identity()));
        return candidates.stream().map(itemCandidate -> {
            ArticleContent content = id2Content.get(itemCandidate.itemId());
            // NPE Protection
            if (content == null) {
                return null;
            }
            double score = qualityScorer.score(content.content(), LocalDateTime.ofInstant(content.publishedAt(), ZoneId.systemDefault()));
            log.debug("itemId: {}, score: {}", itemCandidate.itemId(), qualityScorer.getGrade(score));
            return itemCandidate.withScore(score * itemCandidate.score());
        }).filter(Objects::nonNull).sorted(Comparator.comparingDouble(ItemCandidate::score).reversed()).toList();

    }

    private List<ItemCandidate> deduplication(UserContext context, List<ItemCandidate> items) {
//      title去重
        Set<String> itemTitles = context.recentItemTitles();
        Map<Long, String> id2title = articleRepository.findArticleSummariesByIds(items.stream().map(ItemCandidate::itemId).toList()).stream().collect(Collectors.toMap(ArticleSummaryView::articleId, ArticleSummaryView::title));
        return items.stream().filter(item -> {
            String title = id2title.get(item.itemId());
            return title != null && !itemTitles.contains(title);
        }).toList();
    }
}