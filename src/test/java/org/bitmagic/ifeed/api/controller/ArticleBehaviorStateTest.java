package org.bitmagic.ifeed.api.controller;

import org.bitmagic.ifeed.api.response.CollectionStateResponse;
import org.bitmagic.ifeed.application.embedding.ArticleEnhancedService;
import org.bitmagic.ifeed.application.recommendation.RecommendationService;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.MixFeedRepository;
import org.bitmagic.ifeed.domain.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleBehaviorStateTest {
    @Mock private ArticleService articles;
    @Mock private ArticleEnrichmentService enrichment;
    @Mock private ArticleEnhancedService enhanced;
    @Mock private UserCollectionService collections;
    @Mock private UserLikeService likes;
    @Mock private RecommendationService recommendations;
    @Mock private FeedRepository feeds;
    @Mock private MixFeedRepository mixFeeds;
    @Mock private MixFeedService mixFeedService;
    @InjectMocks private ArticleController controller;

    private final UUID articleUid = UUID.randomUUID();
    private final Long articleId = 3_000_000_001L;

    @BeforeEach
    void setUp() {
        var feed = new Feed();
        feed.setUid(UUID.randomUUID());
        feed.setUrl("https://example.com/feed.xml");
        var article = Article.builder().id(articleId).uid(articleUid).feed(feed).build();
        when(articles.getArticle(articleUid)).thenReturn(article);
    }

    @Test
    void anonymousResponseHasNoPersonalState() {
        var response = controller.getArticle(null, articleUid.toString()).getBody();
        assertNotNull(response);
        assertFalse(response.collected());
        assertFalse(response.liked());
        assertNull(response.folderId());
        verifyNoInteractions(collections, likes);
    }

    @Test
    void authenticatedResponseContainsPublicFolderAndIndependentLikeState() {
        var folderUid = UUID.randomUUID();
        when(collections.findState(7, articleId)).thenReturn(Optional.of(
                new CollectionStateResponse(articleUid, folderUid, Instant.now())));
        when(likes.isLiked(7, articleId)).thenReturn(true);

        var response = controller.getArticle(principal(7), articleUid.toString()).getBody();

        assertTrue(response.collected());
        assertTrue(response.liked());
        assertEquals(folderUid, response.folderId());
        verify(collections).findState(7, articleId);
        verify(likes).isLiked(7, articleId);
    }

    @Test
    void defaultFolderIsDistinctFromUncollectedAndOtherUsersState() {
        when(collections.findState(7, articleId)).thenReturn(Optional.of(
                new CollectionStateResponse(articleUid, null, Instant.now())));
        var collected = controller.getArticle(principal(7), articleUid.toString()).getBody();
        var other = controller.getArticle(principal(8), articleUid.toString()).getBody();

        assertTrue(collected.collected());
        assertNull(collected.folderId());
        assertFalse(collected.liked());
        assertFalse(other.collected());
        assertNull(other.folderId());
        assertFalse(other.liked());
    }

    private UserPrincipal principal(int userId) {
        var user = new User();
        user.setId(userId);
        return new UserPrincipal(user);
    }
}
