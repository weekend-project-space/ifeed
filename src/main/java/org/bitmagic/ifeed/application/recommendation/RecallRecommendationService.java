package org.bitmagic.ifeed.application.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallEngine;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallRequest;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ArticleService articleService;
    private final Map<Integer, List<Long>> user2Items = new ConcurrentHashMap<>();

    @Override
    public Page<ArticleSummaryView> recommend(Integer userId, int page, int size) {
        long start = System.currentTimeMillis();

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        List<Long> cachedIds = user2Items.get(userId);
        if (safePage == 0 || cachedIds == null) {
            RecallRequest request = new RecallRequest(userId, "home", safeSize * 6, Collections.emptyMap(), false, Instant.now());
            cachedIds = recallEngine.recall(request).items().stream().map(ItemCandidate::itemId).collect(Collectors.toList());
            user2Items.put(userId, cachedIds);
        }

        log.info("recall:{}ms", System.currentTimeMillis() - start);
        return articleService.findIds2Article(cachedIds, safePage, safeSize);
    }
}
