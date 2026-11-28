package org.bitmagic.ifeed.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.OpmlImportConfirmRequest;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.api.response.*;
import org.bitmagic.ifeed.application.subscription.SubscriptionApplicationService;
import org.bitmagic.ifeed.application.subscription.SubscriptionQueryService;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionQueryService queries;
    private final SubscriptionApplicationService subscriptions;

    @PostMapping
    public ResponseEntity<MessageResponse> subscribe(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody SubscriptionRequest request) {
        subscriptions.subscribe(userId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Subscription added."));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(queries.list(userId(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> unsubscribe(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String id) {
        subscriptions.unsubscribe(userId(principal), id);
        return ResponseEntity.ok(new MessageResponse("Subscription removed."));
    }

    @PostMapping(value = "/opml/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OpmlPreviewResponse> previewOpml(@AuthenticationPrincipal UserPrincipal principal,
                                                           @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(subscriptions.preview(userId(principal), file));
    }

    @PostMapping(value = "/opml/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OpmlImportConfirmResponse> confirmOpml(@AuthenticationPrincipal UserPrincipal principal,
                                                                @Valid @RequestBody OpmlImportConfirmRequest request) {
        return ResponseEntity.ok(subscriptions.confirm(userId(principal), request));
    }

    private Integer userId(UserPrincipal principal) { return principal == null ? null : principal.getId(); }
}
