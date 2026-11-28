package org.bitmagic.ifeed.application.article;

import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserArticleStateRepository;
import org.bitmagic.ifeed.domain.service.*;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleQueryServiceTest {
    @Mock private ArticleService articles;
    @Mock private ArticleEnrichmentService enrichment;
    @Mock private UserCollectionService collections;
    @Mock private UserLikeService likes;
    @Mock private FeedRepository feeds;
    @Mock private UserArticleStateRepository states;
    private ArticleQueryService query;
    private final PageRequest pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        query = new ArticleQueryService(articles, enrichment, collections, likes, feeds,
                new UserArticleStateService(states));
    }

    @Test
    void rssFeedArticlesPreserveUserAndFilters() {
        UUID feedId = UUID.randomUUID();
        when(feeds.findByUid(feedId)).thenReturn(Optional.of(new Feed()));
        when(articles.listArticles(7, feedId, Set.of("java", "rss"), "tech", false, pageable))
                .thenReturn(Page.empty(pageable));

        assertThat(query.listArticles(7, feedId.toString(), " Java, rss ", " TECH ", "owner", pageable)
                .getContent()).isEmpty();

        verify(articles).listArticles(7, feedId, Set.of("java", "rss"), "tech", false, pageable);
    }

    @Test
    void aggregateOwnerListingUsesAllPersonalSubscriptionsWithoutAFeedLookup() {
        when(articles.listArticles(7, null, Set.of("rss"), "tech", false, pageable))
                .thenReturn(Page.empty(pageable));
        assertThat(query.listArticles(7, null, "RSS", "tech", "owner", pageable).getContent()).isEmpty();
        verify(articles).listArticles(7, null, Set.of("rss"), "tech", false, pageable);
        verifyNoInteractions(feeds, states);
    }

    @Test
    void unknownSourceReturnsNotFoundWithoutQueryingArticles() {
        UUID feedId = UUID.randomUUID();
        when(feeds.findByUid(feedId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> query.listArticles(7, feedId.toString(), null, null, "owner", pageable))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(articles, states);
    }

    @Test
    void anonymousGlobalListingDoesNotRequireAFeedLookup() {
        when(articles.listArticles(0, null, Set.of(), null, true, pageable))
                .thenReturn(Page.empty(pageable));

        assertThat(query.listArticles(null, null, null, null, "global", pageable).getContent()).isEmpty();

        verify(articles).listArticles(0, null, Set.of(), null, true, pageable);
        verifyNoInteractions(feeds, states);
    }

    @Test
    void invalidSourceIdIsRejectedBeforeDataAccess() {
        assertThatThrownBy(() -> query.listArticles(7, "invalid", null, null, "owner", pageable))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(feeds, articles, states);
    }
}
