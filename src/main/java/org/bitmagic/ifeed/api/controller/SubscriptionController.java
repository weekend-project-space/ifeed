package org.bitmagic.ifeed.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.api.converter.SubscriptionViewConverter;
import org.bitmagic.ifeed.api.request.OpmlImportConfirmRequest;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.response.OpmlImportConfirmResponse;
import org.bitmagic.ifeed.api.response.OpmlPreviewResponse;
import org.bitmagic.ifeed.api.response.SubscriptionResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.model.UserSubscriptionId;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.domain.service.AuthService;
import org.bitmagic.ifeed.domain.service.OpmlImportService;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.domain.spec.FeedSpecs;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthService authService;

    private final UserBehaviorRepository userBehaviorRepository;
    private final FeedRepository feedRepository;
    private final OpmlImportService opmlImportService;

    @PostMapping
    public ResponseEntity<MessageResponse> subscribe(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody SubscriptionRequest request) {
        var user = resolveUser(principal);
        subscriptionService.subscribe(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Subscription added."));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        UserBehaviorDocument userBehaviorDocument = userBehaviorRepository.findById(principal.getId().toString()).orElse(null);
        Map<String, Instant> feedReadTimes = Objects.nonNull(userBehaviorDocument) ? userBehaviorDocument.getReadFeedHistory().stream().collect(Collectors.toMap(UserBehaviorDocument.FeedRef::getFeedId, UserBehaviorDocument.FeedRef::getTimestamp)) : new HashMap<>();
        List<UserSubscription> userSubscriptions = subscriptionService.getActiveSubscriptions(principal.getId());
        Map<Integer, Feed> id2Feed = feedRepository.findAll(FeedSpecs.idIn(userSubscriptions.stream().map(UserSubscription::getId).map(UserSubscriptionId::getFeedId).collect(Collectors.toSet()))).stream().collect(Collectors.toMap(Feed::getId, Function.identity()));
        var subscriptions = userSubscriptions.stream()
                .map(subscription -> {
                    var feed = id2Feed.get(subscription.getId().getFeedId());
                    return SubscriptionViewConverter.toResponse(feed,
                            feedReadTimes,
                            false);
                })
                .toList();
        return ResponseEntity.ok(subscriptions);
    }


    @DeleteMapping("/{feedId}")
    public ResponseEntity<MessageResponse> unsubscribe(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable String feedId) {
        var user = resolveUser(principal);
        subscriptionService.unsubscribe(user, IdentifierUtils.parseUuid(feedId, "feed id"));
        return ResponseEntity.ok(new MessageResponse("Subscription removed."));
    }

    @PostMapping(value = "/opml/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OpmlPreviewResponse> previewOpml(@AuthenticationPrincipal UserPrincipal principal,
                                                           @RequestParam("file") MultipartFile file) {
        var user = resolveUser(principal);
        var response = opmlImportService.generatePreview(user, file);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/opml/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OpmlImportConfirmResponse> confirmOpml(@AuthenticationPrincipal UserPrincipal principal,
                                                                 @Valid @RequestBody OpmlImportConfirmRequest request) {
        var user = resolveUser(principal);
        var response = opmlImportService.confirm(user, request);
        return ResponseEntity.ok(response);
    }

    private User resolveUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authService.findUserById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

}
