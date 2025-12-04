package org.bitmagic.ifeed.application.recommendation.recall.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.model.FusionContext;
import org.bitmagic.ifeed.application.recommendation.recall.model.ItemCandidate;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallPlan;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallRequest;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallResponse;
import org.bitmagic.ifeed.application.recommendation.recall.model.StrategyId;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 多路召回调度引擎，负责并发执行各策略并完成结果融合。
 */
@Slf4j
@RequiredArgsConstructor
public class RecallEngine {

    private final StrategyRegistry registry;
    private final RecallPlanner planner;
    private final RecallFusion fusion;
    private final UserContextFactory contextFactory;
    private final Executor executor;
    private final ArticleRepository articleRepository;
    private final AdaptiveScoreMapper adaptiveScoreMapper = new AdaptiveScoreMapper();

    public RecallResponse recall(RecallRequest request) {
        long start = System.nanoTime();

        // 构建用户上下文并生成召回计划
        UserContext context = contextFactory.create(request);
        RecallPlan plan = planner.plan(request, registry.available(request.scene()));
        Map<StrategyId, CompletableFuture<List<ItemCandidate>>> futures = new EnumMap<>(StrategyId.class);

        plan.quotas().forEach((id, quota) -> {
            if (quota <= 0) {
                return;
            }
            // 各策略并发执行，异常时降级为空结果
            futures.put(id, CompletableFuture.supplyAsync(() -> {
                        long start0 = System.currentTimeMillis();
                        List<ItemCandidate> list = registry.get(id).recall(context, quota);
                        log.info("{} time: {}ms quota:{} size:{}", id, System.currentTimeMillis() - start0, quota, list.size());
                        return list;
                    }, executor)
                    .exceptionally(ex -> {
                        log.warn("Recall strategy {} failed: {}", id, ex.getMessage());
                        return List.of();
                    }));
        });

        Map<StrategyId, List<ItemCandidate>> channelResults = new EnumMap<>(StrategyId.class);
        futures.forEach((id, future) -> channelResults.put(id, future.join()));
        channelResults.forEach((strategyId, list) -> {
            if (log.isDebugEnabled()) {
                Map<Long, String> id2title = articleRepository.findArticleSummariesByIds(list.stream().map(ItemCandidate::itemId).toList()).stream().collect(Collectors.toMap(ArticleSummaryView::articleId, ArticleSummaryView::title));
                list.forEach(item -> {
                    log.debug("recall {}-{} title: {} score:{}=>{}", strategyId, item.itemId(), id2title.get(item.itemId()), item.score(), adaptiveScoreMapper.map(item.score()));
                });
            }
            channelResults.put(strategyId, list.stream().map(i -> i.withScore(adaptiveScoreMapper.map(i.score()))).collect(Collectors.toList()));
        });
        // 将各通道结果交给融合层进行重排、多样化
        List<ItemCandidate> fused = fusion.fuse(channelResults, new FusionContext(request, plan.fusionConfig()));
        Duration latency = Duration.ofNanos(System.nanoTime() - start);

        return new RecallResponse(fused, channelResults, latency, Map.of());
    }
}
