package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.config.Const;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
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
    private final UserSubscriptionRepository subscriptionRepository;

    @Transactional
    public UserSubscription subscribe(User user, SubscriptionRequest request) {
        SourceType type = SourceType.FEED;
        Integer sourceId;
        long usedQuota = subscriptionRepository.countByUserAndSourceTypeAndActiveTrue(user, SourceType.FEED);
        int remainingQuota = (int) Math.max(0, user.getCurrentPlan().getMaxSubscriptions() - usedQuota);
        Assert.isTrue(remainingQuota > 0, "Exceeding the subscription limit, please upgrade your plan");
        // Resolve the RSS feed by UUID when feedId is provided
        if (StringUtils.hasText(request.feedId())) {
            UUID uuid;
            try {
                uuid = UUID.fromString(request.feedId());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid source ID format");
            }

            sourceId = feedRepository.findByUid(uuid)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feed not found"))
                    .getId();
        } else if (StringUtils.hasText(request.feedUrl())) {
            // If feedUrl is provided, create or find Feed
            String feedUrl = request.feedUrl().trim();
            Assert.isTrue(UrlChecker.isValidUrl(feedUrl), "Invalid URL: " + feedUrl);
            Feed feed = feedRepository.findByUrl(feedUrl)
                    .orElseGet(() -> createAndSaveFeed(feedUrl, request));
            sourceId = feed.getId();
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Either feedId or feedUrl is required");
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
        return subscriptionRepository.findByUserIdAndSourceTypeAndActiveTrue(userId, SourceType.FEED);
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
        UUID uid = UUID.fromString(sourceIdStr);
        Feed feed = feedRepository.findByUid(uid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription source not found"));
        unsubscribeInternal(user, SourceType.FEED, feed.getId());
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
                .icon(Const.FAVICON_TEMPLATE.formatted(extractHost(siteUrl)))
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
