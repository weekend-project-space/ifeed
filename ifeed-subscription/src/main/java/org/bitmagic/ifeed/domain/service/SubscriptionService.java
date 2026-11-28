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
    public SubscriptionChange subscribe(User user, SubscriptionRequest request) {
        if (request == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Subscription request is required");
        Feed feed;
        String feedUrl = null;
        if (StringUtils.hasText(request.feedId())) {
            feed = feedRepository.findByUid(parseFeedId(request.feedId()))
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feed not found"));
        } else if (StringUtils.hasText(request.feedUrl())) {
            feedUrl = validateUrl(request.feedUrl(), "feed URL");
            feed = feedRepository.findByUrl(feedUrl).orElse(null);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Either feedId or feedUrl is required");
        }

        var existing = feed == null ? java.util.Optional.<UserSubscription>empty()
                : subscriptionRepository.findByUserIdAndSourceTypeAndSourceId(
                        user.getId(), SourceType.FEED, feed.getId());
        if (existing.isPresent() && existing.get().isActive()) {
            return new SubscriptionChange(feed, true, false);
        }

        long usedQuota = subscriptionRepository.countByUserAndSourceTypeAndActiveTrue(user, SourceType.FEED);
        if (usedQuota >= user.getCurrentPlan().getMaxSubscriptions()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Exceeding the subscription limit, please upgrade your plan");
        }
        if (feed == null) feed = createAndSaveFeed(feedUrl, request);

        UserSubscription subscription = existing.orElseGet(UserSubscription::new);
        subscription.setUser(user);
        subscription.setSourceType(SourceType.FEED);
        subscription.setSourceId(feed.getId());
        subscription.setActive(true);
        subscriptionRepository.save(subscription);
        return new SubscriptionChange(feed, true, true);
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
    public SubscriptionChange unsubscribe(User user, String sourceIdStr) {
        Feed feed = feedRepository.findByUid(parseFeedId(sourceIdStr))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription source not found"));
        var subscription = subscriptionRepository.findByUserIdAndSourceTypeAndSourceId(
                user.getId(), SourceType.FEED, feed.getId());
        if (subscription.isEmpty() || !subscription.get().isActive()) {
            return new SubscriptionChange(feed, false, false);
        }

        subscription.get().setActive(false);
        subscriptionRepository.save(subscription.get());
        return new SubscriptionChange(feed, false, true);
    }

    public record SubscriptionChange(Feed feed, boolean subscribed, boolean changed) { }

    private UUID parseFeedId(String value) {
        try {
            UUID id = UUID.fromString(value == null ? "" : value.trim());
            if (!id.toString().equalsIgnoreCase(value.trim())) throw new IllegalArgumentException();
            return id;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid feed id", exception);
        }
    }

    private String validateUrl(String value, String field) {
        String normalized = value.trim();
        if (normalized.length() > 255 || !UrlChecker.isValidUrl(normalized)
                || URI.create(normalized).getHost() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid " + field);
        }
        return normalized;
    }

    private Feed createAndSaveFeed(String feedUrl, SubscriptionRequest request) {
        String siteUrl = StringUtils.hasText(request.siteUrl()) ? validateUrl(request.siteUrl(), "site URL") : feedUrl;
        String title = resolveFeedTitle(request.title(), siteUrl, feedUrl);
        if (title.length() > 255) throw new ApiException(HttpStatus.BAD_REQUEST, "Feed title is too long");

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
