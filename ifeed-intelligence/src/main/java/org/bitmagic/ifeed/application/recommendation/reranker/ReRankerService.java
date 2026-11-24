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
        List<ItemCandidate> candidates = deduplicateByTitle(userContext, items);
        return candidates.stream().sorted(Comparator.comparingDouble(ItemCandidate::score).reversed()).toList();

    }

    private List<ItemCandidate> deduplicateByTitle(UserContext context, List<ItemCandidate> items) {
        // 1. 获取用户最近已读的文章标题
        Set<String> recentTitles = context.recentItemTitles();

        // 2. 获取候选文章的标题
        Map<Long, String> id2title = articleRepository
                .findArticleContentByIds(items.stream().map(ItemCandidate::itemId).toList())
                .stream()
                .collect(Collectors.toMap(ArticleContent::id, ArticleContent::title));

        // 3. 用于去重的 Set（包含已读 + 当前列表已出现的）
        Set<String> seenTitles = new HashSet<>(recentTitles);

        // 4. 过滤：去掉已读的 + 去掉列表内重复的
        return items.stream()
                .filter(item -> {
                    String title = id2title.get(item.itemId());
                    if (title == null || seenTitles.contains(title)) {
                        return false;
                    }
                    log.debug("source:{} title {} score {}", item.source(), title, item.score());
                    seenTitles.add(title); // 标记为已出现
                    return true;
                })
                .toList();
    }

}