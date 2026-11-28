package org.bitmagic.ifeed.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.MixFeedRequest;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.response.MixFeedDetailResponse;
import org.bitmagic.ifeed.api.response.MixFeedListResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.application.feed.MixFeedApplicationService;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mix-feeds")
@RequiredArgsConstructor
public class MixFeedController {
    private final MixFeedApplicationService mixFeeds;

    @PostMapping
    public ResponseEntity<MessageResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody MixFeedRequest request) {
        mixFeeds.create(userId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("MixFeed created."));
    }

    @GetMapping
    public ResponseEntity<List<MixFeedListResponse>> listMyMixFeeds(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mixFeeds.listMyMixFeeds(userId(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MixFeedDetailResponse> getDetail(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String id) {
        return ResponseEntity.ok(mixFeeds.getDetail(userId(principal), IdentifierUtils.parseUuid(id, "mix feed id")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String id,
                                                   @Valid @RequestBody MixFeedRequest request) {
        mixFeeds.update(userId(principal), IdentifierUtils.parseUuid(id, "mix feed id"), request);
        return ResponseEntity.ok(new MessageResponse("MixFeed updated."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String id) {
        mixFeeds.delete(userId(principal), IdentifierUtils.parseUuid(id, "mix feed id"));
        return ResponseEntity.ok(new MessageResponse("MixFeed deleted."));
    }

    @GetMapping("/public")
    public ResponseEntity<Page<MixFeedListResponse>> listPublicMixFeeds(
            @PageableDefault(direction = Sort.Direction.DESC, sort = "subscriberCount") Pageable pageable) {
        return ResponseEntity.ok(mixFeeds.listPublicMixFeeds(pageable));
    }

    private Integer userId(UserPrincipal principal) { return principal == null ? null : principal.getId(); }
}
