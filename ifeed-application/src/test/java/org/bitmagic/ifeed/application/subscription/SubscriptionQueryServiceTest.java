package org.bitmagic.ifeed.application.subscription;

import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserReadFeedRepository;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionQueryServiceTest {
    @Mock private SubscriptionService subscriptions;
    @Mock private UserReadFeedRepository readTimes;
    @Mock private FeedRepository feeds;
    @InjectMocks private SubscriptionQueryService query;
    private final UUID feedId = UUID.randomUUID();
    private final UUID secondFeedId = UUID.randomUUID();
    private final Instant updatedAt = Instant.parse("2026-11-28T00:00:00Z");

    @Test
    void returnsRssFeedsWithTheCurrentUsersReadState() {
        sources();
        var result = query.list(7);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).feedId()).isEqualTo(feedId.toString());
        assertThat(result.get(0).type()).isEqualTo("FEED");
        assertThat(result.get(0).isRead()).isTrue();
        assertThat(result.get(1).feedId()).isEqualTo(secondFeedId.toString());
        assertThat(result.get(1).type()).isEqualTo("FEED");
        assertThat(result.get(1).isRead()).isFalse();
        verify(subscriptions).getActiveSubscriptions(7);
        verify(readTimes).findReadTimes(7L);
    }

    @Test
    void pagedMcpViewUsesTheSameSourceOrderAsTheWebList() {
        sources();
        var page = query.list(7, PageRequest.of(1, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).singleElement().satisfies(row -> assertThat(row.feedId()).isEqualTo(secondFeedId.toString()));
        var beyondEnd = query.list(7, PageRequest.of(3, 10));
        assertThat(beyondEnd.getContent()).isEmpty();
        assertThat(beyondEnd.getTotalElements()).isEqualTo(2);
    }

    @Test
    void emptySubscriptionsDoNotLoadUnrelatedSources() {
        when(readTimes.findReadTimes(7L)).thenReturn(Map.of());
        when(subscriptions.getActiveSubscriptions(7)).thenReturn(List.of());
        assertThat(query.list(7)).isEmpty();
        verifyNoInteractions(feeds);
    }

    @Test
    void missingSourcesAreOmittedAsInTheExistingList() {
        var source = subscription(1);
        when(readTimes.findReadTimes(7L)).thenReturn(Map.of());
        when(subscriptions.getActiveSubscriptions(7)).thenReturn(List.of(source));
        when(feeds.findAll(anySpecification())).thenReturn(List.of());
        assertThat(query.list(7)).isEmpty();
    }

    @Test
    void requiresAnAuthenticatedUserBeforeReadingPersonalData() {
        assertThatThrownBy(() -> query.list(null)).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> query.list(0)).isInstanceOf(ApiException.class);
        verifyNoInteractions(subscriptions, readTimes, feeds);
    }

    private void sources() {
        var feed = new Feed();
        feed.setId(1); feed.setUid(feedId); feed.setTitle("RSS");
        feed.setSiteUrl("https://example.com"); feed.setLastUpdated(updatedAt);
        var secondFeed = new Feed();
        secondFeed.setId(2); secondFeed.setUid(secondFeedId); secondFeed.setTitle("Second RSS"); secondFeed.setLastUpdated(updatedAt);
        var rssSubscription = subscription(1);
        var secondSubscription = subscription(2);
        when(readTimes.findReadTimes(7L)).thenReturn(Map.of(feedId.toString(), updatedAt.plusSeconds(1)));
        when(subscriptions.getActiveSubscriptions(7)).thenReturn(List.of(rssSubscription, secondSubscription));
        when(feeds.findAll(anySpecification())).thenReturn(List.of(secondFeed, feed));
    }

    private UserSubscription subscription(int id) {
        return UserSubscription.builder().sourceType(SourceType.FEED).sourceId(id).build();
    }

    @SuppressWarnings("unchecked")
    private Specification<Feed> anySpecification() { return any(Specification.class); }
}
