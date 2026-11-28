package org.bitmagic.ifeed.api.controller;

import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.api.response.SubscriptionStateResponse;
import org.bitmagic.ifeed.application.subscription.SubscriptionApplicationService;
import org.bitmagic.ifeed.application.subscription.SubscriptionQueryService;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.FeedFetchStatus;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {
    @Mock private SubscriptionQueryService queries;
    @Mock private SubscriptionApplicationService subscriptions;
    @InjectMocks private SubscriptionController controller;
    private final String feedId = UUID.randomUUID().toString();
    private final UserPrincipal principal = new UserPrincipal(User.builder().id(7).currentPlan(User.Plan.FREE).build());

    @Test
    void creationReturnsStateAndPreservesTheMessageField() {
        var request = SubscriptionRequest.of(feedId);
        var state = state(true, true);
        when(subscriptions.subscribe(7, request)).thenReturn(state);
        var response = controller.subscribe(principal, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(state);
        assertThat(response.getBody().message()).isEqualTo("Subscription added.");
        assertThat(response.getBody().fetchStatus()).isEqualTo(FeedFetchStatus.PENDING);
    }

    @Test
    void repeatedCreationReturnsOkWithTheFinalState() {
        var request = SubscriptionRequest.of(feedId);
        when(subscriptions.subscribe(7, request)).thenReturn(state(true, false));
        var response = controller.subscribe(principal, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().subscribed()).isTrue();
        assertThat(response.getBody().changed()).isFalse();
    }

    @Test
    void cancellationDelegatesToTheSameSharedApplicationMethodAsMcp() {
        var state = state(false, true);
        when(subscriptions.unsubscribe(7, feedId)).thenReturn(state);
        var response = controller.unsubscribe(principal, feedId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(state);
        assertThat(response.getBody().subscribed()).isFalse();
        verify(subscriptions).unsubscribe(7, feedId);
    }

    private SubscriptionStateResponse state(boolean subscribed, boolean changed) {
        return new SubscriptionStateResponse("Subscription added.", feedId, "RSS", "https://example.com/feed.xml",
                subscribed, changed, FeedFetchStatus.PENDING, null, null, null);
    }
}
