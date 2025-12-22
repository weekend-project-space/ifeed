package org.bitmagic.ifeed.application.recommendation.reranker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.domain.record.ArticleContent;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yangrd
 * @date 2025/12/6
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class ReRankerService {


    private final ArticleRepository articleRepository;

    public List<ItemCandidate> reranker(UserContext userContext, List<ItemCandidate> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
//       移除后面一样标题的内容
        List<ItemCandidate> candidates = deduplication(userContext, items);
        return candidates.stream().sorted(Comparator.comparingDouble(ItemCandidate::score).reversed()).toList();

    }

    private List<ItemCandidate> deduplication(UserContext context, List<ItemCandidate> items) {
//      title去重
        Set<String> itemTitles = context.recentItemTitles();
        Map<Long, String> id2title = articleRepository.findArticleContentByIds(items.stream().map(ItemCandidate::itemId).toList()).stream().collect(Collectors.toMap(ArticleContent::id, ArticleContent::title));
        return items.stream().filter(item -> {
            String title = id2title.get(item.itemId());
            return title != null && !itemTitles.contains(title);
        }).toList();
    }

}