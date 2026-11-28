package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.FeedDetailResponse;
import org.bitmagic.ifeed.api.response.SubscriptionSearchResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.application.feed.FeedQueryService;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {
    private final FeedQueryService feeds;

    @GetMapping("/{feedId}")
    public ResponseEntity<FeedDetailResponse> getFeed(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String feedId) {
        return ResponseEntity.ok(feeds.getFeed(userId(principal), IdentifierUtils.parseUuid(feedId, "feed id")));
    }

    @Deprecated
    @GetMapping("/search")
    public ResponseEntity<List<SubscriptionSearchResponse>> search(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("query") String query, @RequestParam(required = false) String type) {
        return ResponseEntity.ok(feeds.search(userId(principal), query, type));
    }

    @Deprecated
    @GetMapping("/lookup")
    public ResponseEntity<FeedDetailResponse> lookupFeed(@AuthenticationPrincipal UserPrincipal principal, @RequestParam String feedUrl) {
        return ResponseEntity.ok(feeds.lookupFeed(userId(principal), feedUrl));
    }

    private Integer userId(UserPrincipal principal) { return principal == null ? null : principal.getId(); }
}
