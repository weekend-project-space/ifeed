package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.converter.SubscriptionViewConverter;
import org.bitmagic.ifeed.api.response.FeedDetailResponse;
import org.bitmagic.ifeed.api.response.SubscriptionSearchResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.MixFeed;
import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.MixFeedRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.domain.service.FeedService;
import org.bitmagic.ifeed.domain.service.MixFeedService;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final MixFeedService mixFeedService;
    private final SubscriptionService subscriptionService;
    private final FeedRepository feedRepository;
    private final MixFeedRepository mixFeedRepository;
    private final UserBehaviorRepository userBehaviorRepository;

    @GetMapping("/{feedId}")
    public ResponseEntity<FeedDetailResponse> getFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String feedId) {
        ensureAuthenticated(principal);

        var uuid = IdentifierUtils.parseUuid(feedId, "feed id");
        // Try to find Feed first
        var feedOpt = feedRepository.findByUid(uuid);
        if (feedOpt.isPresent()) {
            var detail = feedService.getFeedDetail(uuid);
            var subscribed = feedService.isSubscribed(principal.getId(), detail.feed());
            return ResponseEntity.ok(toFeedResponse(detail, subscribed));
        }
        // If not found as Feed, try MixFeed
        var mixFeedOpt = mixFeedRepository.findByUid(uuid);
        if (mixFeedOpt.isPresent()) {
            var detail = mixFeedService.getDetail(uuid, principal.getId());
            var subscribed = mixFeedService.isSubscribed(principal.getId(), detail.mixFeed());
            return ResponseEntity.ok(toMixFeedResponse(detail, subscribed));
        }

        // Neither found
        throw new ApiException(HttpStatus.NOT_FOUND, "Feed or MixFeed not found");
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubscriptionSearchResponse>> search(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("query") String query,
            @RequestParam(required = false) String type) {

        List<SubscriptionSearchResponse> responses = new ArrayList<>();

        // Search Feeds (default or when type=FEED)
        var feeds = feedService.searchFeeds(query);
        var subscribedFeedIds = subscriptionService.getActiveSubscriptions(principal.getId()).stream()
                .filter(sub -> sub.getSourceType() == SourceType.FEED)
                .map(UserSubscription::getSourceId)
                .collect(Collectors.toSet());
        var subscriberCount = subscriptionService.getSubscriberCounts(feeds.stream().map(Feed::getId).toList());

        responses.addAll(feeds.stream()
                .map(feed -> SubscriptionViewConverter.toSearchResponse(feed,
                        subscribedFeedIds.contains(feed.getId()),
                        subscriberCount.get(feed.getId())))
                .toList());

        // Search MixFeeds (when type=MIX_FEED or type=ALL)
        var mixFeeds = mixFeedService.searchMixFeeds(query);
        var subscribedMixFeedIds = subscriptionService.getActiveSubscriptions(principal.getId()).stream()
                .filter(sub -> sub.getSourceType() == SourceType.MIX_FEED)
                .map(UserSubscription::getSourceId)
                .collect(Collectors.toSet());

        responses.addAll(mixFeeds.stream()
                .map(mixFeed -> SubscriptionViewConverter.toSearchResponse(mixFeed,
                        subscribedMixFeedIds.contains(mixFeed.getId())))
                .toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/lookup")
    public ResponseEntity<FeedDetailResponse> lookupFeed(@AuthenticationPrincipal UserPrincipal principal,
                                                         @RequestParam String feedUrl) {
        ensureAuthenticated(principal);
        var detail = feedService.getFeedDetailByUrl(feedUrl);
        var subscribed = feedService.isSubscribed(principal.getId(), detail.feed());
        return ResponseEntity.ok(toFeedResponse(detail, subscribed));
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private FeedDetailResponse toFeedResponse(FeedService.FeedDetail detail, boolean subscribed) {
        var feed = detail.feed();
        var failureCount = feed.getFailureCount() == null ? 0 : feed.getFailureCount();
        var host = extractHost(feed.getSiteUrl());
        return new FeedDetailResponse(
                feed.getUid().toString(),
                feed.getTitle(),
                feed.getDescription(),
                feed.getUrl(),
                feed.getSiteUrl(),
                "https://favicon.im/%s".formatted(host),
                feed.getLastFetched(),
                feed.getLastUpdated(),
                detail.articleCount(),
                detail.subscriberCount(),
                subscribed,
                failureCount,
                feed.getFetchError(),
                "FEED", null);
    }

    private FeedDetailResponse toMixFeedResponse(MixFeedService.MixFeedDetail detail, boolean subscribed) {
        MixFeed mixFeed = detail.mixFeed();
        return new FeedDetailResponse(
                mixFeed.getUid().toString(),
                mixFeed.getName(),
                mixFeed.getDescription(),
                null, // MixFeed doesn't have URL
                null, // MixFeed doesn't have siteUrl
                mixFeed.getIcon(),
                mixFeed.getLastFetched(), // MixFeed doesn't have lastFetched
                mixFeed.getLastUpdated(),
                detail.articleCount(),
                detail.subscriberCount(),
                subscribed,
                0, // MixFeed doesn't have failureCount
                null, // MixFeed doesn't have fetchError
                "MIX_FEED", mixFeed.config().getSourceFeeds().values());
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
