package org.bitmagic.ifeed.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.tracking.TopicTrackConfigRequest;
import org.bitmagic.ifeed.api.tracking.TopicTrackResponse;
import org.bitmagic.ifeed.api.tracking.TopicTrackSuggestionResponse;
import org.bitmagic.ifeed.api.tracking.TopicTrackSummaryResponse;
import org.bitmagic.ifeed.api.tracking.TopicTrackCursorPage;
import org.bitmagic.ifeed.application.tracking.TopicTrackingService;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/topic-tracks")
@RequiredArgsConstructor
public class TopicTrackingController {

    private final TopicTrackingService topicTrackingService;

    @GetMapping("/limits")
    public java.util.Map<String, Integer> limits() {
        return topicTrackingService.limits();
    }

    @GetMapping("/status")
    public java.util.List<org.bitmagic.ifeed.api.tracking.TopicTrackScanResponse> scanStates(@RequestParam java.util.List<UUID> topicIds) {
        return topicTrackingService.scanStates(topicIds);
    }

    @GetMapping("/{topicId}")
    public TopicTrackResponse detail(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID topicId) {
        return topicTrackingService.getTopic(principal == null ? null : principal.getId(), topicId);
    }

    @GetMapping("/suggestions")
    public TopicTrackSuggestionResponse suggest(@AuthenticationPrincipal UserPrincipal principal,
                                                @RequestParam String keyword) {
        ensureAuthenticated(principal);
        return topicTrackingService.suggest(principal.getId(), keyword);
    }

    @PostMapping
    public ResponseEntity<TopicTrackResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody TopicTrackConfigRequest request) {
        ensureAuthenticated(principal);
        var result = topicTrackingService.create(principal.getId(), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.topic());
    }

    @GetMapping
    public Page<TopicTrackResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                         @PageableDefault(size = 50, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return topicTrackingService.listTracks(principal == null ? null : principal.getId(), pageable);
    }

    @PutMapping("/{topicId}")
    public TopicTrackResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable UUID topicId,
                                     @Valid @RequestBody TopicTrackConfigRequest request) {
        ensureAuthenticated(principal);
        return topicTrackingService.update(principal.getId(), topicId, request);
    }

    @PostMapping("/{topicId}/subscribe")
    public TopicTrackResponse subscribe(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID topicId) {
        ensureAuthenticated(principal);
        return topicTrackingService.subscribe(principal.getId(), topicId);
    }

    @DeleteMapping("/{topicId}/subscribe")
    public TopicTrackResponse unsubscribe(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID topicId) {
        ensureAuthenticated(principal);
        return topicTrackingService.unsubscribe(principal.getId(), topicId);
    }

    @PostMapping("/{topicId}/disable")
    public TopicTrackResponse disable(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID topicId) {
        ensureAuthenticated(principal);
        return topicTrackingService.disable(principal.getId(), topicId);
    }

    @GetMapping("/following")
    public Page<TopicTrackResponse> followedTopics(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PageableDefault(size = 50, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ensureAuthenticated(principal);
        return topicTrackingService.listFollowedTracks(principal.getId(), pageable);
    }

    @GetMapping("/summaries")
    public TopicTrackCursorPage<TopicTrackSummaryResponse> summaries(@AuthenticationPrincipal UserPrincipal principal,
                                                     @RequestParam(required = false) UUID topicId,
                                                     @RequestParam(defaultValue = "false") boolean following,
                                                     @RequestParam(required = false) UUID cursor,
                                                     @RequestParam(defaultValue = "20") int size) {
        if (following) ensureAuthenticated(principal);
        return topicTrackingService.listSummaries(principal == null ? null : principal.getId(), topicId, following, cursor, size);
    }

    @GetMapping("/summaries/{summaryId}/articles")
    public TopicTrackCursorPage<TopicTrackSummaryResponse.ArticlePreview> summaryArticles(@PathVariable UUID summaryId,
                                                                                         @RequestParam(required = false) UUID cursor,
                                                                                         @RequestParam(defaultValue = "30") int size) {
        return topicTrackingService.listSummaryArticles(summaryId, cursor, size);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
