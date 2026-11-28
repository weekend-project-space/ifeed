package org.bitmagic.ifeed.domain.service;

import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    @Mock private FeedRepository feeds;
    @Mock private UserSubscriptionRepository subscriptions;
    @InjectMocks private SubscriptionService service;
    private final User user = User.builder().id(7).currentPlan(User.Plan.FREE).build();
    private final Feed feed = Feed.builder().id(3).uid(UUID.randomUUID()).build();

    @Test
    void subscribeByUuidCreatesRssSubscriptionAndCountsOnlyRssQuota() {
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        when(subscriptions.save(any(UserSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.subscribe(user, SubscriptionRequest.of(feed.getUid().toString()));

        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getSourceType()).isEqualTo(SourceType.FEED);
        assertThat(result.getSourceId()).isEqualTo(feed.getId());
        assertThat(result.isActive()).isTrue();
        verify(subscriptions).countByUserAndSourceTypeAndActiveTrue(user, SourceType.FEED);
    }

    @Test
    void unknownSourceCannotCreateASubscription() {
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subscribe(user, SubscriptionRequest.of(feed.getUid().toString())))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(subscriptions, never()).save(any());
    }

    @Test
    void inactiveRssSubscriptionCanBeReactivated() {
        var previous = UserSubscription.builder().user(user).sourceType(SourceType.FEED)
                .sourceId(feed.getId()).active(false).build();
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        when(subscriptions.findByUserIdAndSourceTypeAndSourceId(7, SourceType.FEED, 3))
                .thenReturn(Optional.of(previous));
        when(subscriptions.save(previous)).thenReturn(previous);

        assertThat(service.subscribe(user, SubscriptionRequest.of(feed.getUid().toString()))).isSameAs(previous);
        assertThat(previous.isActive()).isTrue();
    }

    @Test
    void quotaStillPreventsNewRssSubscriptions() {
        when(subscriptions.countByUserAndSourceTypeAndActiveTrue(user, SourceType.FEED)).thenReturn(60L);

        assertThatThrownBy(() -> service.subscribe(user, SubscriptionRequest.of(feed.getUid().toString())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("subscription limit");
        verifyNoInteractions(feeds);
        verify(subscriptions, never()).save(any());
    }

    @Test
    void unsubscribeOnlyChangesTheCurrentUsersRssSubscription() {
        var subscription = UserSubscription.builder().user(user).sourceType(SourceType.FEED)
                .sourceId(feed.getId()).active(true).build();
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        when(subscriptions.findByUserIdAndSourceTypeAndSourceId(7, SourceType.FEED, 3))
                .thenReturn(Optional.of(subscription));

        service.unsubscribe(user, feed.getUid().toString());

        assertThat(subscription.isActive()).isFalse();
        verify(subscriptions).save(subscription);
    }

    @Test
    void unknownSourceCannotUnsubscribeAnUnrelatedFeed() {
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unsubscribe(user, feed.getUid().toString()))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(subscriptions);
    }

    @Test
    void activeSubscriptionQueryFiltersRssBeforeMaterializingRows() {
        when(subscriptions.findByUserIdAndSourceTypeAndActiveTrue(7, SourceType.FEED)).thenReturn(List.of());

        assertThat(service.getActiveSubscriptions(7)).isEmpty();

        verify(subscriptions).findByUserIdAndSourceTypeAndActiveTrue(7, SourceType.FEED);
    }
}
