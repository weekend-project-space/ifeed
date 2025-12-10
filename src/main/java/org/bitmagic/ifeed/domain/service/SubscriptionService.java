package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.MixFeed;
import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.MixFeedRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.infrastructure.util.UrlChecker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final FeedRepository feedRepository;
    private final MixFeedRepository mixFeedRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @Transactional
    public UserSubscription subscribe(User user, SubscriptionRequest request) {
        SourceType type;
        Integer sourceId;

        // If sourceId is provided, try to find by UUID (Feed or MixFeed)
        if (StringUtils.hasText(request.feedId())) {
            UUID uuid;
            try {
                uuid = UUID.fromString(request.feedId());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid source ID format");
            }

            // Try Feed first
            var feedOpt = feedRepository.findByUid(uuid);
            if (feedOpt.isPresent()) {
                type = SourceType.FEED;
                sourceId = feedOpt.get().getId();
            } else {
                // Try MixFeed
                MixFeed mixFeed = mixFeedRepository.findByUid(uuid)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feed or MixFeed not found"));
                type = SourceType.MIX_FEED;
                sourceId = mixFeed.getId();

                // Increment MixFeed subscriber count
                mixFeedRepository.incrementSubscriberCount(sourceId);
            }
        } else if (StringUtils.hasText(request.feedUrl())) {
            // If feedUrl is provided, create or find Feed
            String feedUrl = request.feedUrl().trim();
            Assert.isTrue(UrlChecker.isValidUrl(feedUrl), "Invalid URL: " + feedUrl);
            Feed feed = feedRepository.findByUrl(feedUrl)
                    .orElseGet(() -> createAndSaveFeed(feedUrl, request));
            type = SourceType.FEED;
            sourceId = feed.getId();
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Either sourceId or feedUrl is required");
        }

        return subscriptionRepository.findByUserIdAndSourceTypeAndSourceId(user.getId(), type, sourceId)
                .map(sub -> {
                    if (sub.isActive()) {
                        throw new ApiException(HttpStatus.CONFLICT, "Already subscribed");
                    }
                    sub.setActive(true);
                    return subscriptionRepository.save(sub);
                })
                .orElseGet(() -> {
                    UserSubscription sub = UserSubscription.builder()
                            .user(user)
                            .sourceType(type)
                            .sourceId(sourceId)
                            .active(true)
                            .build();
                    return subscriptionRepository.save(sub);
                });
    }

    @Transactional(readOnly = true)
    public List<UserSubscription> getActiveSubscriptions(Integer userId) {
        return subscriptionRepository.findAllByUserIdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> getSubscriberCounts(List<Integer> feedIds) {
        if (feedIds == null || feedIds.isEmpty()) {
            return Map.of();
        }

        // 1. 批量查询
        List<Object[]> results = subscriptionRepository.countActiveSubscribersByFeedIds(feedIds);

        // 2. 转为 Map
        Map<Integer, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Long) row[1]));

        // 3. 补全缺失的 feedId → 0
        return feedIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> countMap.getOrDefault(id, 0L),
                        (a, b) -> a,
                        LinkedHashMap::new // 保持顺序
                ));
    }

    @Transactional
    public void unsubscribe(User user, String sourceIdStr) {
        // Try to resolve sourceId. Since we don't know the type from the ID string
        // alone (unless we enforce format),
        // we might need to check both or require type.
        // However, the API design says DELETE /api/subscriptions/:id where id is UUID.
        // We can check Feed first, then MixFeed. Or require type in API.
        // The design doc says: "DELETE /api/subscriptions/:id ... 后端通过查询订阅表自动识别类型"
        // But we store Integer ID in DB, not UUID. So we can't query DB by UUID
        // directly without joining.
        // We need to resolve UUID to Integer ID first.

        UUID uid = UUID.fromString(sourceIdStr);

        // Try Feed
        var feed = feedRepository.findByUid(uid);
        if (feed.isPresent()) {
            unsubscribeInternal(user, SourceType.FEED, feed.get().getId());
            return;
        }

        // Try MixFeed
        var mixFeed = mixFeedRepository.findByUid(uid);
        if (mixFeed.isPresent()) {
            unsubscribeInternal(user, SourceType.MIX_FEED, mixFeed.get().getId());
            // Decrement subscriber count
            mixFeedRepository.decrementSubscriberCount(mixFeed.get().getId());
            return;
        }

        throw new ApiException(HttpStatus.NOT_FOUND, "Subscription source not found");
    }

    private void unsubscribeInternal(User user, SourceType type, Integer sourceId) {
        var subscription = subscriptionRepository.findByUserIdAndSourceTypeAndSourceId(user.getId(), type, sourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (!subscription.isActive()) {
            return;
        }

        subscription.setActive(false);
        subscriptionRepository.save(subscription);
    }

    private Feed createAndSaveFeed(String feedUrl, SubscriptionRequest request) {
        String siteUrl = StringUtils.hasText(request.siteUrl()) ? request.siteUrl().trim() : feedUrl;
        String title = resolveFeedTitle(request.title(), siteUrl, feedUrl);

        Feed feed = Feed.builder()
                .url(feedUrl)
                .siteUrl(siteUrl)
                .title(title)
                .build();

        return feedRepository.save(feed);
    }

    private String resolveFeedTitle(String requestedTitle, String siteUrl, String feedUrl) {
        if (StringUtils.hasText(requestedTitle)) {
            return requestedTitle.trim();
        }

        var host = extractHost(siteUrl);
        if (StringUtils.hasText(host)) {
            return host;
        }

        host = extractHost(feedUrl);
        if (StringUtils.hasText(host)) {
            return host;
        }

        return "未命名订阅";
    }

    private String extractHost(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            var uri = new URI(url.trim());
            if (StringUtils.hasText(uri.getHost())) {
                return uri.getHost();
            }
            var path = uri.getPath();
            if (StringUtils.hasText(path)) {
                return path;
            }
        } catch (URISyntaxException ignored) {
            // fallback to raw url below
        }
        return url;
    }
}
