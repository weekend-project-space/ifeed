package org.bitmagic.ifeed.application.subscription;

import org.bitmagic.ifeed.api.request.OpmlImportConfirmRequest;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.FeedFetchStatus;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.domain.service.OpmlImportService;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.domain.service.UserService;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionApplicationServiceTest {
    @Mock private SubscriptionService subscriptions;
    @Mock private UserService users;
    @Mock private OpmlImportService opml;
    @Mock private UserRepository userRepository;
    @InjectMocks private SubscriptionApplicationService application;
    private final User user = User.builder().id(7).currentPlan(User.Plan.PRO).build();
    private final Feed feed = Feed.builder().id(3).uid(UUID.randomUUID()).title("RSS")
            .url("https://example.com/feed.xml").build();

    @Test
    void subscriptionUsesTheLockedFreshUserAndReportsPendingFetchSeparately() {
        var request = SubscriptionRequest.of(feed.getUid().toString());
        when(userRepository.findByIdForUpdate(7)).thenReturn(Optional.of(user));
        when(subscriptions.subscribe(user, request)).thenReturn(new SubscriptionService.SubscriptionChange(feed, true, true));
        var response = application.subscribe(7, request);
        assertThat(response.feedId()).isEqualTo(feed.getUid().toString());
        assertThat(response.subscribed()).isTrue();
        assertThat(response.changed()).isTrue();
        assertThat(response.fetchStatus()).isEqualTo(FeedFetchStatus.PENDING);
        assertThat(response.lastFetched()).isNull();
        assertThat(response.message()).isEqualTo("Subscription added.");
        var order = inOrder(userRepository, subscriptions);
        order.verify(userRepository).findByIdForUpdate(7);
        order.verify(subscriptions).subscribe(user, request);
        verifyNoInteractions(users);
    }

    @Test
    void repeatedAdditionReportsTheFinalSubscribedStateWithoutAChange() {
        var request = SubscriptionRequest.of(feed.getUid().toString());
        when(userRepository.findByIdForUpdate(7)).thenReturn(Optional.of(user));
        when(subscriptions.subscribe(user, request)).thenReturn(new SubscriptionService.SubscriptionChange(feed, true, false));
        var response = application.subscribe(7, request);
        assertThat(response.subscribed()).isTrue();
        assertThat(response.changed()).isFalse();
        assertThat(response.message()).isEqualTo("Already subscribed.");
    }

    @Test
    void cancellationReturnsTheSameFeedIdentityAndFinalState() {
        when(userRepository.findByIdForUpdate(7)).thenReturn(Optional.of(user));
        when(subscriptions.unsubscribe(user, feed.getUid().toString())).thenReturn(new SubscriptionService.SubscriptionChange(feed, false, false));
        var response = application.unsubscribe(7, feed.getUid().toString());
        assertThat(response.feedId()).isEqualTo(feed.getUid().toString());
        assertThat(response.url()).isEqualTo(feed.getUrl());
        assertThat(response.subscribed()).isFalse();
        assertThat(response.changed()).isFalse();
    }

    @Test
    void importsUseTheSameUserLockSoQuotaChecksDoNotRaceSubscriptionWrites() {
        var request = mock(OpmlImportConfirmRequest.class);
        when(userRepository.findByIdForUpdate(7)).thenReturn(Optional.of(user));
        application.confirm(7, request);
        var order = inOrder(userRepository, opml);
        order.verify(userRepository).findByIdForUpdate(7);
        order.verify(opml).confirm(user, request);
        verifyNoInteractions(users);
    }

    @Test
    void invalidUsersAreRejectedBeforeAnySubscriptionMutation() {
        assertThatThrownBy(() -> application.subscribe(null, SubscriptionRequest.of(feed.getUid().toString())))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> application.unsubscribe(0, feed.getUid().toString())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> application.subscribe(99, SubscriptionRequest.of(feed.getUid().toString()))).isInstanceOf(ApiException.class);
        verifyNoInteractions(subscriptions, opml, users);
    }
}
