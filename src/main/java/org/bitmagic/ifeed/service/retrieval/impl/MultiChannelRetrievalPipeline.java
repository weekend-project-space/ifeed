package org.bitmagic.ifeed.service.retrieval.impl;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.service.retrieval.DocScore;
import org.bitmagic.ifeed.service.retrieval.RetrievalContext;
import org.bitmagic.ifeed.service.retrieval.RetrievalHandler;
import org.bitmagic.ifeed.service.retrieval.RetrievalPipeline;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class MultiChannelRetrievalPipeline implements RetrievalPipeline {

    private double freshnessWeight = 0.3; // 默认新鲜度占比
    private double freshnessLambda = 0.5; // 指数衰减
    private final Duration halfLife = Duration.ofHours(48); // 时间半衰期，默认2天 ;2天内权重衰减到50%
    private final List<WeightedHandler> handlers = new ArrayList<>();

    public MultiChannelRetrievalPipeline addHandler(RetrievalHandler handler, double weight) {
        this.handlers.add(new WeightedHandler(handler, weight));
        return this;
    }


    public List<Long> execute(RetrievalContext context) {
        Map<Long, DocScore> merged = new LinkedHashMap<>();
        for (WeightedHandler wh : handlers) {
            RetrievalHandler handler = wh.handler();
            double weight = wh.weight();
            if (handler.supports(context)) {
                log.debug("Handler [{}] applicable, executing...", handler.getClass().getSimpleName());
                Map<Long, DocScore> results = handler.handle(context).stream().collect(Collectors.toMap(DocScore::docId, Function.identity()));
                normalizeScores(results);
                for (var entry : results.entrySet()) {
                    Long id = entry.getKey();
                    DocScore score = entry.getValue().scale(weight);
                    merged.merge(id, score, (a, b) -> a.combine(b));
                }
            }
        }

        // Step 2️⃣: 统一计算 freshness 修正
        Map<Long, DocScore> adjusted = new LinkedHashMap<>();
        for (var entry : merged.entrySet()) {
            Long id = entry.getKey();
            DocScore doc = entry.getValue();
            double freshness = this.computeFreshnessFactor(doc.pubDate());
            double finalScore = doc.score() * (1 - freshnessWeight)
                    + freshness * freshnessWeight;

            adjusted.put(id, doc.from(finalScore));
        }


        ////        移除一样标题的内容
        Collection<DocScore> docScores = adjusted.values().stream().collect(Collectors.toMap(
                entry -> entry.meta().toString(),    // key: id
                Function.identity(),     // value: 对象本身
                (existing, replacement) -> existing  // 冲突时保留第一个
        )).values();


        // 排序：按综合得分降序
        return docScores.stream().sorted((a, b) -> Double.compare(b.score(), a.score())).map(DocScore::docId).limit(context.getTopK()).collect(Collectors.toList());
//        return adjusted.entrySet().stream()
//                .sorted((a, b) -> Double.compare(b.getValue().score(), a.getValue().score()))
//                .collect(LinkedHashMap::new,
//                        (m, e) -> m.put(e.getKey(), e.getValue()),
//                        Map::putAll);
    }

    Map<Long, DocScore> normalizeScores(Map<Long, DocScore> scores) {
        if (scores.isEmpty()) {
            return Collections.emptyMap();
        }
        double min = scores.values().stream().mapToDouble(DocScore::score).min().orElse(0.0);
        double max = scores.values().stream().mapToDouble(DocScore::score).max().orElse(0.0);
        if (Double.compare(max, min) == 0) {
            Map<Long, DocScore> normalized = new HashMap<>();
            scores.forEach((id, score) -> {
                normalized.put(id, new DocScore(score.docId(), 1.0, score.pubDate(), score.meta()));
            });
            return normalized;
        }

        double range = max - min;
        Map<Long, DocScore> normalized = new HashMap<>();
        scores.forEach((id, score) -> normalized.put(id, new DocScore(id, (score.score() - min) / range, score.pubDate(), score.meta())));
        return normalized;
    }

    double computeFreshnessFactor(Instant pubDate) {
        if (pubDate == null) return 0.0;

        double hoursAgo = Duration.between(pubDate, Instant.now()).toHours();
        return Math.pow(freshnessLambda, hoursAgo / halfLife.toHours());
    }


    private record WeightedHandler(RetrievalHandler handler, double weight) {
    }
}
