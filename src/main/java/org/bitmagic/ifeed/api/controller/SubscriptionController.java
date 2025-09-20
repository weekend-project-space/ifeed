package org.bitmagic.ifeed.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.response.SubscriptionResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.AuthService;
import org.bitmagic.ifeed.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<MessageResponse> subscribe(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody SubscriptionRequest request) {
        var user = resolveUser(principal);
        subscriptionService.subscribe(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Subscription added."));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        var user = resolveUser(principal);
        var subscriptions = subscriptionService.getActiveSubscriptions(user).stream()
                .map(subscription -> new SubscriptionResponse(
                        subscription.getFeed().getId().toString(),
                        subscription.getFeed().getTitle(),
                        subscription.getFeed().getUrl()))
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

    private User resolveUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authService.findUserById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
