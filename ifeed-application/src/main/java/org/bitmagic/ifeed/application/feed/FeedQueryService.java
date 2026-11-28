package org.bitmagic.ifeed.application.feed;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.converter.SubscriptionViewConverter;
import org.bitmagic.ifeed.api.response.FeedDetailResponse;
import org.bitmagic.ifeed.api.response.SubscriptionSearchResponse;
import org.bitmagic.ifeed.config.Const;
import org.bitmagic.ifeed.domain.model.*;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.service.*;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedQueryService {
    private final FeedService feedService;
    private final SubscriptionService subscriptionService;

    @Transactional(readOnly = true)
    public FeedDetailResponse getFeed(Integer userId, UUID feedId) {
        var detail = feedService.getFeedDetail(feedId);
        var subscribed = feedService.isSubscribed(userId, detail.feed());
        return toFeedResponse(detail, subscribed);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionSearchResponse> search(Integer userId, String query) {
        ensureAuthenticated(userId);

        var feeds = feedService.searchFeeds(query);
        var subscribedFeedIds = subscriptionService.getActiveSubscriptions(userId).stream()
                .map(UserSubscription::getSourceId)
                .collect(Collectors.toSet());
        var subscriberCount = subscriptionService.getSubscriberCounts(feeds.stream().map(Feed::getId).toList());

        return feeds.stream()
                .map(feed -> SubscriptionViewConverter.toSearchResponse(feed,
                        subscribedFeedIds.contains(feed.getId()),
                        subscriberCount.get(feed.getId())))
                .toList();
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
                "FEED");
    }
}
