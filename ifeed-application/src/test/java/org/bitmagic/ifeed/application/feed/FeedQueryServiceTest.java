package org.bitmagic.ifeed.application.feed;

import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.service.FeedService;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedQueryServiceTest {
    @Mock private FeedService feeds;
    @Mock private SubscriptionService subscriptions;
    @InjectMocks private FeedQueryService query;

    @Test
    void rssDetailIncludesCurrentUsersSubscriptionAndSourceCounts() {
        var feed = feed();
        when(feeds.getFeedDetail(feed.getUid())).thenReturn(new FeedService.FeedDetail(feed, 20, 3, null));
        when(feeds.isSubscribed(7, feed)).thenReturn(true);

        var detail = query.getFeed(7, feed.getUid());

        assertThat(detail.feedId()).isEqualTo(feed.getUid().toString());
        assertThat(detail.type()).isEqualTo("FEED");
        assertThat(detail.articleCount()).isEqualTo(20);
        assertThat(detail.subscriberCount()).isEqualTo(3);
        assertThat(detail.subscribed()).isTrue();
    }

    @Test
    void unknownSourcePreservesNotFoundFromRssService() {
        var feedId = UUID.randomUUID();
        when(feeds.getFeedDetail(feedId)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Feed not found"));

        assertThatThrownBy(() -> query.getFeed(7, feedId)).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(subscriptions);
    }

    @Test
    void searchReturnsRssFeedsWithPersonalSubscriptionState() {
        var feed = feed();
        var subscription = UserSubscription.builder().sourceType(SourceType.FEED).sourceId(feed.getId()).build();
        when(feeds.searchFeeds("RSS")).thenReturn(List.of(feed));
        when(subscriptions.getActiveSubscriptions(7)).thenReturn(List.of(subscription));
        when(subscriptions.getSubscriberCounts(List.of(feed.getId()))).thenReturn(Map.of(feed.getId(), 3L));

        assertThat(query.search(7, "RSS")).singleElement().satisfies(row -> {
            assertThat(row.feedId()).isEqualTo(feed.getUid().toString());
            assertThat(row.type()).isEqualTo("FEED");
            assertThat(row.subscribed()).isTrue();
            assertThat(row.subscriberCount()).isEqualTo(3);
        });
    }

    @Test
    void searchRequiresAnAuthenticatedUser() {
        assertThatThrownBy(() -> query.search(null, "RSS")).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(feeds, subscriptions);
    }

    private Feed feed() {
        return Feed.builder().id(1).uid(UUID.randomUUID()).title("RSS")
                .url("https://example.com/feed.xml").siteUrl("https://example.com").build();
    }
}
