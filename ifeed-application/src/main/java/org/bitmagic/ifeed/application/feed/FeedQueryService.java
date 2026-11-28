package org.bitmagic.ifeed.application.feed;

import org.bitmagic.ifeed.api.converter.SubscriptionViewConverter;
import org.bitmagic.ifeed.api.response.FeedDetailResponse;
import org.bitmagic.ifeed.api.response.SubscriptionSearchResponse;
import org.bitmagic.ifeed.config.Const;
import org.bitmagic.ifeed.domain.model.*;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.MixFeedRepository;
import org.bitmagic.ifeed.domain.service.*;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedQueryService {
    private final FeedService feedService;
    private final MixFeedService mixFeedService;
    private final SubscriptionService subscriptionService;
    private final FeedRepository feedRepository;
    private final MixFeedRepository mixFeedRepository;


    @Transactional(readOnly = true)
    public FeedDetailResponse getFeed(Integer userId, UUID feedId) {

        var uuid = feedId;
        // Try to find Feed first
        var feedOpt = feedRepository.findByUid(uuid);
        if (feedOpt.isPresent()) {
            var detail = feedService.getFeedDetail(uuid);
            var subscribed = feedService.isSubscribed(userId, detail.feed());
            return toFeedResponse(detail, subscribed);
        }
        // If not found as Feed, try MixFeed
        var mixFeedOpt = mixFeedRepository.findByUid(uuid);
        if (mixFeedOpt.isPresent()) {
            var detail = mixFeedService.getDetail(uuid, userId);
            var subscribed = mixFeedService.isSubscribed(userId, detail.mixFeed());
            return toMixFeedResponse(detail, subscribed);
        }

        // Neither found
        throw new ApiException(HttpStatus.NOT_FOUND, "Feed or MixFeed not found");
        }

    @Transactional(readOnly = true)
    public List<SubscriptionSearchResponse> search(Integer userId, String query, String type) {
        ensureAuthenticated(userId);

        List<SubscriptionSearchResponse> responses = new ArrayList<>();

        // Search Feeds (default or when type=FEED)
        var feeds = feedService.searchFeeds(query);
        var subscribedFeedIds = subscriptionService.getActiveSubscriptions(userId).stream()
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
        var subscribedMixFeedIds = subscriptionService.getActiveSubscriptions(userId).stream()
                .filter(sub -> sub.getSourceType() == SourceType.MIX_FEED)
                .map(UserSubscription::getSourceId)
                .collect(Collectors.toSet());

        responses.addAll(mixFeeds.stream()
                .map(mixFeed -> SubscriptionViewConverter.toSearchResponse(mixFeed,
                        subscribedMixFeedIds.contains(mixFeed.getId())))
                .toList());

        return responses;
        }

    public FeedDetailResponse lookupFeed(Integer userId, String feedUrl) {
        ensureAuthenticated(userId);
        var detail = feedService.getFeedDetailByUrl(feedUrl);
        var subscribed = feedService.isSubscribed(userId, detail.feed());
        return toFeedResponse(detail, subscribed);
        }

    private void ensureAuthenticated(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private FeedDetailResponse toFeedResponse(FeedService.FeedDetail detail, boolean subscribed) {
        var feed = detail.feed();
        var failureCount = feed.getFailureCount() == null ? 0 : feed.getFailureCount();
        return new FeedDetailResponse(
                feed.getUid().toString(),
                feed.getTitle(),
                feed.getDescription(),
                feed.getUrl(),
                feed.getSiteUrl(),
                feed.getIcon(),
                feed.getLastFetched(),
                feed.getLastUpdated(),
                detail.articleCount(),
                detail.subscriberCount(),
                subscribed,
                failureCount < Const.MAX_FAILURE_COUNT ? 0 : failureCount,
                failureCount < Const.MAX_FAILURE_COUNT ? null : feed.getFetchError(),
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

}
