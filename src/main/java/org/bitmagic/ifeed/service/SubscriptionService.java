package org.bitmagic.ifeed.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.domain.entity.Feed;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.domain.entity.UserSubscription;
import org.bitmagic.ifeed.domain.entity.UserSubscriptionId;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.net.URI;
import java.net.URISyntaxException;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final FeedRepository feedRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @Transactional
    public UserSubscription subscribe(User user, SubscriptionRequest request) {
        var normalizedUrl = request.feedUrl().trim();
        var feed = feedRepository.findByUrl(normalizedUrl)
                .orElseGet(() -> createFeed(normalizedUrl, request));

        if (!StringUtils.hasText(feed.getTitle())) {
            var resolvedTitle = resolveFeedTitle(request.title(), feed.getSiteUrl(), feed.getUrl());
            if (!resolvedTitle.equals(feed.getTitle())) {
                feed.setTitle(resolvedTitle);
                feed = feedRepository.save(feed);
            }
        }

        var existing = subscriptionRepository.findByUserAndFeed(user, feed);
        if (existing.isPresent()) {
            var subscription = existing.get();
            if (subscription.isActive()) {
                throw new ApiException(HttpStatus.CONFLICT, "Feed already subscribed");
            }
            if (subscription.getId() == null) {
                subscription.setId(new UserSubscriptionId(user.getId(), feed.getId()));
            }
            subscription.setActive(true);
            return subscriptionRepository.save(subscription);
        }

        var subscription = UserSubscription.builder()
                .id(new UserSubscriptionId(user.getId(), feed.getId()))
                .user(user)
                .feed(feed)
                .active(true)
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<UserSubscription> getActiveSubscriptions(User user) {
        return subscriptionRepository.findAllByUserAndActiveTrue(user);
    }

    @Transactional(readOnly = true)
    public Set<UUID> getActiveFeedIds(User user) {
        return new HashSet<>(subscriptionRepository.findActiveFeedIdsByUserId(user.getId()));
    }

    @Transactional(readOnly = true)
    public long getSubscriberCount(Feed feed) {
        if (feed == null) {
            return 0;
        }
        return subscriptionRepository.countByFeedAndActiveTrue(feed);
    }

    @Transactional
    public void unsubscribe(User user, UUID feedId) {
        var feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feed not found"));

        var subscription = subscriptionRepository.findByUserAndFeed(user, feed)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (!subscription.isActive()) {
            return;
        }

        subscription.setActive(false);
        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<Feed> searchFeeds(String query) {
        if (!StringUtils.hasText(query)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "query must not be empty");
        }
        var normalized = query.trim();
        return feedRepository.searchByQuery(normalized, PageRequest.of(0, 20));
    }

    private Feed createFeed(String feedUrl, SubscriptionRequest request) {
        var siteUrl = request.siteUrl();
        if (siteUrl == null || siteUrl.isBlank()) {
            siteUrl = feedUrl;
        }

        var title = resolveFeedTitle(request.title(), siteUrl, feedUrl);

        return feedRepository.save(Feed.builder()
                .url(feedUrl)
                .siteUrl(siteUrl)
                .title(title)
                .build());
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
