package org.bitmagic.ifeed.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.OpmlImportConfirmRequest;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.api.response.*;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.domain.service.AuthService;
import org.bitmagic.ifeed.domain.service.OpmlImportService;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
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
                    return toSubscriptionResponse(feed, feedReadTimes);
                })
                .toList();
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubscriptionSearchResponse>> search(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @RequestParam("query") String query) {
        var user = resolveUser(principal);
        UserBehaviorDocument userBehaviorDocument = userBehaviorRepository.findById(user.getId().toString()).orElse(null);
        Map<String, Instant> feedReadTimes = Objects.nonNull(userBehaviorDocument) ? userBehaviorDocument.getReadFeedHistory().stream().collect(Collectors.toMap(UserBehaviorDocument.FeedRef::getFeedId, UserBehaviorDocument.FeedRef::getTimestamp)) : new HashMap<>();
        var feeds = subscriptionService.searchFeeds(query);
        var subscribedFeedIds = subscriptionService.getActiveSubscriptions(user).stream()
                .map(sub -> sub.getFeed().getUid().toString())
                .collect(Collectors.toSet());
        var responses = feeds.stream()
                .map(feed -> {
                    var subscriberCount = subscriptionService.getSubscriberCount(feed);
                    return toSubscriptionSearchResponse(feed, feedReadTimes, subscribedFeedIds.contains(feed.getUid().toString()), subscriberCount);
                })
                .toList();
        return ResponseEntity.ok(responses);
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

    private SubscriptionResponse toSubscriptionResponse(Feed feed, Map<String, Instant> feedReadTimes) {
        if (feed == null) {
            return new SubscriptionResponse(
                    null,
                    "未命名订阅",
                    null,
                    null,
                    "https://favicon.im/",
                    null,
                    null,
                    false,
                    0,
                    null
            );
        }
        var title = resolveFeedTitle(feed);
        var feedUrl = feed.getUrl();
        var siteUrl = feed.getSiteUrl();
        if (siteUrl == null || siteUrl.isBlank()) {
            siteUrl = feedUrl;
        }
        var failureCount = Optional.ofNullable(feed.getFailureCount()).orElse(0);
        var readTimestamp = feedReadTimes.getOrDefault(feed.getUid().toString(), Instant.EPOCH);
        var effectiveLastUpdated = Objects.nonNull(feed.getLastUpdated()) ? feed.getLastUpdated() : Instant.EPOCH;
        var avatarHost = extractHost(siteUrl);
        if (avatarHost == null || avatarHost.isBlank()) {
            avatarHost = extractHost(feedUrl);
        }
        return new SubscriptionResponse(
                feed.getUid().toString(),
                title,
                feedUrl,
                siteUrl,
                "https://favicon.im/%s".formatted(avatarHost != null ? avatarHost : ""),
                feed.getLastFetched(),
                feed.getLastUpdated(),
                readTimestamp.isAfter(effectiveLastUpdated),
                failureCount < 3 ? 0 : failureCount,
                failureCount < 3 ? null : feed.getFetchError()
        );
    }


    private SubscriptionSearchResponse toSubscriptionSearchResponse(Feed feed, Map<String, Instant> feedReadTimes, boolean subscribed, long subscriberCount) {
        if (feed == null) {
            return new SubscriptionSearchResponse(
                    null,
                    "未命名订阅",
                    null,
                    null,
                    "https://favicon.im/",
                    null,
                    null,
                    subscriberCount,

                    subscribed,
                    false,
                    0,
                    null
            );
        }
        var title = resolveFeedTitle(feed);
        var feedUrl = feed.getUrl();
        var siteUrl = feed.getSiteUrl();
        if (siteUrl == null || siteUrl.isBlank()) {
            siteUrl = feedUrl;
        }
        var failureCount = Optional.ofNullable(feed.getFailureCount()).orElse(0);
        var readTimestamp = feedReadTimes.getOrDefault(feed.getUid().toString(), Instant.EPOCH);
        var effectiveLastUpdated = Objects.nonNull(feed.getLastUpdated()) ? feed.getLastUpdated() : Instant.EPOCH;
        var avatarHost = extractHost(siteUrl);
        if (avatarHost == null || avatarHost.isBlank()) {
            avatarHost = extractHost(feedUrl);
        }
        return new SubscriptionSearchResponse(
                feed.getUid().toString(),
                title,
                feedUrl,
                siteUrl,
                "https://favicon.im/%s".formatted(avatarHost != null ? avatarHost : ""),
                feed.getLastFetched(),
                feed.getLastUpdated(),
                subscriberCount,

                subscribed,
                readTimestamp.isAfter(effectiveLastUpdated),
                failureCount < 3 ? 0 : failureCount,
                failureCount < 3 ? null : feed.getFetchError()
        );
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
