package org.bitmagic.ifeed.application.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallEngine;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallRequest;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallResponse;
import org.bitmagic.ifeed.application.recommendation.reranker.ReRankerService;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yangrd
 * @date 2025/11/4
 **/
@Slf4j
@RequiredArgsConstructor
@Service
public class RecallRecommendationService implements RecommendationService {
    private final RecallEngine recallEngine;
    private final ReRankerService reRankerService;
    private final ArticleService articleService;
    private final Map<Integer, List<ItemCandidate>> user2Items = new ConcurrentHashMap<>();

    @Override
    public Page<RecResponse> recommend(RecRequest request, int page, int size) {
        long start = System.currentTimeMillis();
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        List<ItemCandidate> cachedItems = user2Items.get(request.userId());
        if (safePage == 0 || cachedItems == null) {
            RecallRequest recallRequest = new RecallRequest(request.userId(), request.scene(), safeSize * 6, Collections.emptyMap(), false, Instant.now());
            RecallResponse recalled = recallEngine.recall(recallRequest);
            cachedItems = reRankerService.reranker(recalled.userContext(), recalled.items());
            cachedItems.forEach(item -> {
                log.debug("{} score: {}",item.itemId(), item.score());
            });
            user2Items.put(request.userId(), cachedItems);
        }
        Map<Long, ItemCandidate> id2Source = cachedItems.stream().collect(Collectors.toMap(ItemCandidate::itemId, Function.identity()));
        log.info("recall:{}ms", System.currentTimeMillis() - start);
        return articleService.findIds2Article(cachedItems.stream().map(ItemCandidate::itemId).toList(), safePage, safeSize).map(item -> {
            ItemCandidate candidate = id2Source.get(item.articleId());
            return new RecResponse(
                    item.id(),
                    item.title(),
                    item.summary(),
                    item.feedTitle(),
                    formatTimestamp(item.publishedAt()),
                    item.thumbnail(),
                    item.enclosure(),
                    candidate.source().name(),
                    candidate.score(),
                    candidate.reason()

            );
        });
    }
    private String formatTimestamp(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
