package org.bitmagic.ifeed.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.MixFeedRequest;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.response.MixFeedDetailResponse;
import org.bitmagic.ifeed.api.response.MixFeedListResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.model.MixFeed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.service.AuthService;
import org.bitmagic.ifeed.domain.service.MixFeedService;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mix-feeds")
@RequiredArgsConstructor
public class MixFeedController {

    private final MixFeedService mixFeedService;
    private final AuthService authService;
    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<MessageResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody MixFeedRequest request) {
        User user = resolveUser(principal);
        MixFeed mixFeed = mixFeedService.create(user, request.name(), request.description(), request.icon(),
                request.isPublic() != null && request.isPublic(), request.filterConfig());
        subscriptionService.subscribe(user, SubscriptionRequest.of(mixFeed.getUid().toString()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("MixFeed created."));
    }

    @GetMapping
    public ResponseEntity<List<MixFeedListResponse>> listMyMixFeeds(@AuthenticationPrincipal UserPrincipal principal) {
        ensureAuthenticated(principal);
        List<MixFeed> mixFeeds = mixFeedService.getMyMixFeeds(principal.getId());
        List<MixFeedListResponse> responses = mixFeeds.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MixFeedDetailResponse> getDetail(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable String id) {
        ensureAuthenticated(principal);
        var detail = mixFeedService.getDetail(IdentifierUtils.parseUuid(id, "mix feed id"), principal.getId());
        var subscribed = mixFeedService.isSubscribed(principal.getId(), detail.mixFeed());
        return ResponseEntity.ok(toDetailResponse(detail, subscribed));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable String id,
                                                  @Valid @RequestBody MixFeedRequest request) {
        mixFeedService.update(IdentifierUtils.parseUuid(id, "mix feed id"), principal.getId(),
                request.name(), request.description(), request.icon(), request.isPublic(), request.filterConfig());
        return ResponseEntity.ok(new MessageResponse("MixFeed updated."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable String id) {
        User user = resolveUser(principal);
        mixFeedService.delete(IdentifierUtils.parseUuid(id, "mix feed id"), user.getId());
        return ResponseEntity.ok(new MessageResponse("MixFeed deleted."));
    }

    @GetMapping("/public")
    public ResponseEntity<Page<MixFeedListResponse>> listPublicMixFeeds(@PageableDefault(direction = Sort.Direction.DESC, sort = "subscriberCount") Pageable pageable) {
        Page<MixFeed> mixFeeds = mixFeedService.getPublicMixFeeds(pageable);
        return ResponseEntity.ok(mixFeeds.map(this::toListResponse));
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private User resolveUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authService.findUserById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private MixFeedListResponse toListResponse(MixFeed mixFeed) {
        return new MixFeedListResponse(
                mixFeed.getUid().toString(),
                mixFeed.getName(),
                mixFeed.getDescription(),
                mixFeed.getIcon(),
                mixFeed.getSubscriberCount(),
                mixFeed.getIsPublic(),
                mixFeed.getCreatedAt());
    }

    private MixFeedDetailResponse toDetailResponse(MixFeedService.MixFeedDetail detail, boolean subscribed) {
        MixFeed mixFeed = detail.mixFeed();
        return new MixFeedDetailResponse(
                mixFeed.getUid().toString(),
                mixFeed.getName(),
                mixFeed.getDescription(),
                mixFeed.getIcon(),
                detail.subscriberCount(),
                detail.articleCount(),
                mixFeed.config(),
                subscribed,
                mixFeed.getIsPublic(),
                mixFeed.getCreatedAt(),
                mixFeed.getUpdatedAt());
    }
}
