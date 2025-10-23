package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.recommendation.RecommendationScope;
import org.bitmagic.ifeed.service.recommendation.RecommendationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author yangrd
 * @date 2025/10/23
 **/
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class HomeAIController {

    private final RecommendationService service;

    @GetMapping(value = "/recommendations", produces = "text/event-stream")
    public Flux<String> getRecommendations(@AuthenticationPrincipal UserPrincipal principal, @RequestParam(defaultValue = "PERSONAL") RecommendationScope scope) {
        return service.recommendations(principal.getId(), scope);
    }

}
