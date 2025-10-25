package org.bitmagic.ifeed.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.OpmlImportConfirmRequest;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.response.OpmlImportConfirmResponse;
import org.bitmagic.ifeed.api.response.OpmlPreviewResponse;
import org.bitmagic.ifeed.api.response.SubscriptionResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.entity.Feed;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.AuthService;
import org.bitmagic.ifeed.service.OpmlImportService;
import org.bitmagic.ifeed.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthService authService;

    private final UserBehaviorRepository userBehaviorRepository;
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
        var user = resolveUser(principal);
        UserBehaviorDocument userBehaviorDocument = userBehaviorRepository.findById(user.getId().toString()).orElse(null);
        Map<String, Instant> feedReadTimes = Objects.nonNull(userBehaviorDocument) ? userBehaviorDocument.getReadFeedHistory().stream().collect(Collectors.toMap(UserBehaviorDocument.FeedRef::getFeedId, UserBehaviorDocument.FeedRef::getTimestamp)) : new HashMap<>();
        var subscriptions = subscriptionService.getActiveSubscriptions(user).stream()
                .map(subscription -> {
                    var feed = subscription.getFeed();
                    var title = resolveFeedTitle(feed);
                    var feedUrl = feed.getUrl();
                    var siteUrl = feed.getSiteUrl();
                    if (siteUrl == null || siteUrl.isBlank()) {
                        siteUrl = feedUrl;
                    }
                    return new SubscriptionResponse(
                            feed.getId().toString(),
                            title,
                            feedUrl,
                            siteUrl,
                            "https://favicon.im/%s".formatted(extractHost(feed.getSiteUrl())),
                            feed.getLastFetched(),
                            feed.getLastUpdated(),
                            feedReadTimes.getOrDefault(feed.getId().toString(), Instant.EPOCH).isAfter(Objects.nonNull(feed.getLastUpdated())?feed.getLastUpdated():Instant.EPOCH)
                    );
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

    private String resolveFeedTitle(Feed feed) {
        if (feed == null) {
            return "未命名订阅";
        }
        var title = feed.getTitle();
        if (title != null && !title.isBlank()) {
            return title;
        }
        var host = extractHost(feed.getSiteUrl());
        if (host != null && !host.isBlank()) {
            return host;
        }
        host = extractHost(feed.getUrl());
        if (host != null && !host.isBlank()) {
            return host;
        }
        return "未命名订阅";
    }

    private String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            var uri = new URI(url.trim());
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri.getHost();
            }
            var path = uri.getPath();
            if (path != null && !path.isBlank()) {
                return path;
            }
        } catch (URISyntaxException ignored) {
            return url;
        }
        return url;
    }
}
