package org.bitmagic.ifeed.domain.service;

import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.FeedFetchStatus;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private final Feed feed = Feed.builder().id(3).uid(UUID.randomUUID()).url("https://example.com/feed.xml").build();

    @Test
    void subscribeByUuidCreatesOnlyTheCurrentUsersRssSubscription() {
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        var result = service.subscribe(user, SubscriptionRequest.of(feed.getUid().toString()));
        assertThat(result.feed()).isSameAs(feed);
        assertThat(result.subscribed()).isTrue();
        assertThat(result.changed()).isTrue();
        var saved = ArgumentCaptor.forClass(UserSubscription.class);
        verify(subscriptions).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getSourceType()).isEqualTo(SourceType.FEED);
        assertThat(saved.getValue().getSourceId()).isEqualTo(3);
        assertThat(saved.getValue().isActive()).isTrue();
        verify(subscriptions).countByUserAndSourceTypeAndActiveTrue(user, SourceType.FEED);
    }

    @Test
    void newUrlCreatesAPendingFeedWithoutPretendingContentHasBeenFetched() {
        String url = "https://example.com/feed.xml";
        when(feeds.save(any(Feed.class))).thenAnswer(invocation -> {
            Feed created = invocation.getArgument(0);
            created.setId(3);
            created.setUid(feed.getUid());
            return created;
        });
        var result = service.subscribe(user, new SubscriptionRequest("  " + url + "  ", null, null, null));
        assertThat(result.feed().getUid()).isEqualTo(feed.getUid());
        assertThat(result.feed().getUrl()).isEqualTo(url);
        assertThat(result.feed().getTitle()).isEqualTo("example.com");
        assertThat(result.feed().getLastFetchStatus()).isEqualTo(FeedFetchStatus.PENDING);
        assertThat(result.feed().getLastFetched()).isNull();
        assertThat(result.changed()).isTrue();
        verify(feeds).findByUrl(url);
        verify(subscriptions).save(argThat(row -> row.getSourceId() == 3 && row.isActive()));
    }

    @Test
    void repeatedAddIsUnchangedWithoutCheckingOrConsumingQuota() {
        var existing = subscription(true);
        when(feeds.findByUrl(feed.getUrl())).thenReturn(Optional.of(feed));
        when(subscriptions.findByUserIdAndSourceTypeAndSourceId(7, SourceType.FEED, 3)).thenReturn(Optional.of(existing));
        var result = service.subscribe(user, new SubscriptionRequest(feed.getUrl(), null, null, null));
        assertThat(result.subscribed()).isTrue();
        assertThat(result.changed()).isFalse();
        verify(subscriptions, never()).countByUserAndSourceTypeAndActiveTrue(any(), any());
        verify(subscriptions, never()).save(any());
        verify(feeds, never()).save(any());
    }

    @Test
    void inactiveRssSubscriptionIsRestoredUsingTheSameRecord() {
        var previous = subscription(false);
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        when(subscriptions.findByUserIdAndSourceTypeAndSourceId(7, SourceType.FEED, 3)).thenReturn(Optional.of(previous));
        var result = service.subscribe(user, SubscriptionRequest.of(feed.getUid().toString()));
        assertThat(result.changed()).isTrue();
        assertThat(previous.isActive()).isTrue();
        verify(subscriptions).save(previous);
    }

    @Test
    void quotaPreventsNewSubscriptionsAndReactivation() {
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        when(subscriptions.findByUserIdAndSourceTypeAndSourceId(7, SourceType.FEED, 3)).thenReturn(Optional.of(subscription(false)));
        when(subscriptions.countByUserAndSourceTypeAndActiveTrue(user, SourceType.FEED)).thenReturn(60L);
        assertThatThrownBy(() -> service.subscribe(user, SubscriptionRequest.of(feed.getUid().toString())))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("subscription limit");
        verify(subscriptions, never()).save(any());
    }

    @Test
    void quotaFailureDoesNotCreateAnOrphanFeed() {
        when(subscriptions.countByUserAndSourceTypeAndActiveTrue(user, SourceType.FEED)).thenReturn(60L);
        assertThatThrownBy(() -> service.subscribe(user, new SubscriptionRequest(feed.getUrl(), null, null, null)))
                .isInstanceOf(ApiException.class);
        verify(feeds, never()).save(any());
        verify(subscriptions, never()).save(any());
    }

    @Test
    void unknownSourceCannotCreateASubscription() {
        assertThatThrownBy(() -> service.subscribe(user, SubscriptionRequest.of(feed.getUid().toString())))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(subscriptions);
    }

    @Test
    void repeatedUnsubscribeDoesNotFlipStateOrDeleteContent() {
        var subscription = subscription(true);
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        when(subscriptions.findByUserIdAndSourceTypeAndSourceId(7, SourceType.FEED, 3)).thenReturn(Optional.of(subscription));
        var first = service.unsubscribe(user, feed.getUid().toString());
        var repeated = service.unsubscribe(user, feed.getUid().toString());
        assertThat(first.subscribed()).isFalse();
        assertThat(first.changed()).isTrue();
        assertThat(repeated.subscribed()).isFalse();
        assertThat(repeated.changed()).isFalse();
        verify(subscriptions, times(1)).save(subscription);
        verify(feeds, never()).delete(any(Feed.class));
        verify(subscriptions, never()).delete(any(UserSubscription.class));
    }

    @Test
    void cancellingANeverSubscribedFeedDoesNotTouchAnotherUsersSubscription() {
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        var result = service.unsubscribe(user, feed.getUid().toString());
        assertThat(result.subscribed()).isFalse();
        assertThat(result.changed()).isFalse();
        verify(subscriptions).findByUserIdAndSourceTypeAndSourceId(7, SourceType.FEED, 3);
        verify(subscriptions, never()).save(any());
    }

    @Test
    void unknownSourceCannotUnsubscribeAnUnrelatedFeed() {
        assertThatThrownBy(() -> service.unsubscribe(user, feed.getUid().toString()))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(subscriptions);
    }

    @Test
    void invalidIdentifiersAndUrlsAreRejectedBeforeRepositoryAccess() {
        for (String url : List.of("file:///tmp/feed.xml", "https:///feed.xml", "https://example.com/" + "a".repeat(256))) {
            assertThatThrownBy(() -> service.subscribe(user, new SubscriptionRequest(url, null, null, null)))
                    .isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(() -> service.subscribe(user, null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.subscribe(user, SubscriptionRequest.of("bad-id"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.unsubscribe(user, "bad-id")).isInstanceOf(ApiException.class);
        verifyNoInteractions(feeds, subscriptions);
    }

    @Test
    void existingWebRequestsCanStillProvideBothUrlAndFeedId() {
        when(feeds.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        assertThat(service.subscribe(user, new SubscriptionRequest(feed.getUrl(), null, null, feed.getUid().toString())).subscribed()).isTrue();
        verify(feeds, never()).findByUrl(any());
    }

    @Test
    void activeSubscriptionQueryFiltersRssBeforeMaterializingRows() {
        when(subscriptions.findByUserIdAndSourceTypeAndActiveTrue(7, SourceType.FEED)).thenReturn(List.of());
        assertThat(service.getActiveSubscriptions(7)).isEmpty();
        verify(subscriptions).findByUserIdAndSourceTypeAndActiveTrue(7, SourceType.FEED);
    }

    private UserSubscription subscription(boolean active) {
        return UserSubscription.builder().user(user).sourceType(SourceType.FEED).sourceId(3).active(active).build();
    }
}
