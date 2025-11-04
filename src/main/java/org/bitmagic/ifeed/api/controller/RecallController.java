package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.core.RecallEngine;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallRequest;
import org.bitmagic.ifeed.application.recommendation.recall.model.RecallResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 召回调试接口，可在线探索多路召回与多样化配置效果。
 */
@RestController
@RequestMapping("/api/recall")
@RequiredArgsConstructor
@ConditionalOnBean(RecallEngine.class)
public class RecallController {

    private final RecallEngine recallEngine;

    /**
     * 查询用户在指定场景下的召回候选，支持通过参数控制交织与多样化。
     */
    @GetMapping("/candidates")
    public RecallResponse candidates(@RequestParam("userId") Integer userId,
                                     @RequestParam(value = "scene", defaultValue = "home") String scene,
                                     @RequestParam(value = "topK", defaultValue = "50") int topK,
                                     @RequestParam(value = "diversityKey", required = false) String diversityKey,
                                     @RequestParam(value = "diversityLimit", required = false) Integer diversityLimit,
                                     @RequestParam(value = "diversityFillOverflow", required = false) Boolean diversityFillOverflow,
                                     @RequestParam(value = "interleaveChannels", required = false) Boolean interleaveChannels) {
        Map<String, Object> filters = new HashMap<>();
        Optional.ofNullable(diversityKey).ifPresent(key -> filters.put("diversityKey", key));
        Optional.ofNullable(diversityLimit).ifPresent(limit -> filters.put("diversityLimit", limit));
        Optional.ofNullable(diversityFillOverflow).ifPresent(flag -> filters.put("diversityFillOverflow", flag));
        Optional.ofNullable(interleaveChannels).ifPresent(flag -> filters.put("interleaveChannels", flag));

        RecallRequest request = new RecallRequest(userId, scene, topK, filters, false, Instant.now());
        return recallEngine.recall(request);
    }
}
