package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.converter.SubscriptionViewConverter;
import org.bitmagic.ifeed.api.response.FeedDetailResponse;
import org.bitmagic.ifeed.api.response.SubscriptionSearchResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.service.FeedService;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    private final SubscriptionService subscriptionService;

    @GetMapping("/{feedId}")
    public ResponseEntity<FeedDetailResponse> getFeed(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable String feedId) {
        ensureAuthenticated(principal);
        var detail = feedService.getFeedDetail(IdentifierUtils.parseUuid(feedId, "feed id"));
        var subscribed = feedService.isSubscribed(principal.getId(), detail.feed());
        return ResponseEntity.ok(toResponse(detail, subscribed));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubscriptionSearchResponse>> search(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @RequestParam("query") String query) {

        var feeds = feedService.searchFeeds(query);
        var subscribedFeedIds = subscriptionService.getActiveSubscriptions(principal.getId()).stream()
                .map(sub -> sub.getFeed().getId())
                .collect(Collectors.toSet());
        var subscriberCount = subscriptionService.getSubscriberCounts(feeds.stream().map(Feed::getId).toList());
        var responses = feeds.stream()
                .map(feed ->
                        SubscriptionViewConverter.toSearchResponse(feed, subscribedFeedIds.contains(feed.getId()), subscriberCount.get(feed.getId()))
                ).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/lookup")
    public ResponseEntity<FeedDetailResponse> lookupFeed(@AuthenticationPrincipal UserPrincipal principal,
                                                         @RequestParam String feedUrl) {
        ensureAuthenticated(principal);
        var detail = feedService.getFeedDetailByUrl(feedUrl);
        var subscribed = feedService.isSubscribed(principal.getId(), detail.feed());
        return ResponseEntity.ok(toResponse(detail, subscribed));
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private FeedDetailResponse toResponse(FeedService.FeedDetail detail, boolean subscribed) {
        var feed = detail.feed();
        var failureCount = feed.getFailureCount() == null ? 0 : feed.getFailureCount();
        return new FeedDetailResponse(
                feed.getUid().toString(),
                feed.getTitle(),
                feed.getUrl(),
                feed.getSiteUrl(),
                "https://favicon.im/%s".formatted(extractHost(feed.getSiteUrl())),
                feed.getLastFetched(),
                feed.getLastUpdated(),
                detail.latestPublishedAt(),
                detail.articleCount(),
                detail.subscriberCount(),
                subscribed,
                failureCount,
                feed.getFetchError()
        );
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
