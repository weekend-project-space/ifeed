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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    private Feed createFeed(String feedUrl, SubscriptionRequest request) {
        var siteUrl = request.siteUrl();
        if (siteUrl == null || siteUrl.isBlank()) {
            siteUrl = feedUrl;
        }

        return feedRepository.save(Feed.builder()
                .url(feedUrl)
                .siteUrl(siteUrl)
                .title(request.title())
                .build());
    }
}
