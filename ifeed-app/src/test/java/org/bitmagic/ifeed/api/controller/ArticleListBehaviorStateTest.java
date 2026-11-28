package org.bitmagic.ifeed.api.controller;

import org.bitmagic.ifeed.application.embedding.ArticleEnhancedService;
import org.bitmagic.ifeed.application.article.ArticleQueryService;
import org.bitmagic.ifeed.application.article.ArticleRecommendationQueryService;
import org.bitmagic.ifeed.application.article.ArticleEnrichmentApplicationService;
import org.bitmagic.ifeed.application.search.ArticleSearchQueryService;
import org.bitmagic.ifeed.application.recommendation.RecRequest;
import org.bitmagic.ifeed.application.recommendation.RecResponse;
import org.bitmagic.ifeed.application.recommendation.RecommendationService;
import org.bitmagic.ifeed.application.search.SearchRetrievalService;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.record.ArticleBehaviorState;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserArticleStateRepository;
import org.bitmagic.ifeed.domain.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleListBehaviorStateTest {
    @Mock private ArticleService articles;
    @Mock private ArticleEnrichmentService enrichment;
    @Mock private ArticleEnhancedService enhanced;
    @Mock private UserCollectionService collections;
    @Mock private UserLikeService likes;
    @Mock private RecommendationService recommendations;
    @Mock private SearchRetrievalService search;
    @Mock private FeedRepository feeds;
    @Mock private UserArticleStateRepository states;
    private ArticleController articleController;
    private SearchController searchController;
    private final UUID articleUid = UUID.randomUUID();
    private final UUID folderUid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var service = new UserArticleStateService(states);
        articleController = new ArticleController(
                new ArticleQueryService(articles, enrichment, collections, likes, feeds, service),
                new ArticleRecommendationQueryService(recommendations, articles, service),
                new ArticleEnrichmentApplicationService(articles, enhanced));
        searchController = new SearchController(new ArticleSearchQueryService(articles, search, service));
    }

    @Test
    void subscriptionAndFeedListsIncludePersonalState() {
        var pageable = PageRequest.of(0, 20);
        when(articles.listArticles(eq(7), isNull(), anySet(), isNull(), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(summary()), pageable, 1));
        personalState();
        var row = articleController.listArticles(principal(), null, null, null, "owner", pageable)
                .getBody().getContent().getFirst();
        assertTrue(row.collected());
        assertTrue(row.liked());
        assertEquals(folderUid, row.folderId());
        assertEquals(articleUid.toString(), row.article().id());
    }

    @Test
    void homepageRecommendationsAreEnrichedAfterRetrieval() {
        var recommendation = recommendation();
        when(recommendations.recommend(any(RecRequest.class), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(recommendation)));
        personalState();
        var row = articleController.rec(principal(), 0, 20).getBody().getContent().getFirst();
        assertTrue(row.collected());
        assertTrue(row.liked());
        assertSame(recommendation, row.article());
    }

    @Test
    void relatedRecommendationsIncludeStateAndAnonymousRequestsStayUnpersonalized() {
        when(articles.getArticle(articleUid)).thenReturn(Article.builder().id(3_000_000_001L).uid(articleUid).build());
        when(recommendations.recommend(any(RecRequest.class), eq(6))).thenReturn(List.of(recommendation()));
        personalState();
        assertTrue(articleController.rec(principal(), articleUid.toString(), 6).getBody().getFirst().liked());
        var guest = articleController.rec(null, articleUid.toString(), 6).getBody().getFirst();
        assertFalse(guest.liked());
        assertFalse(guest.collected());
        assertNull(guest.folderId());
        verify(states, times(1)).findStates(7, List.of(articleUid));
    }

    @ParameterizedTest
    @ValueSource(strings = {"keyword", "semantic"})
    void bothSearchModesReturnPersonalState(String type) {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(summary()), pageable, 1);
        if (type.equals("keyword")) {
            when(articles.searchArticles(eq(7), eq("query"), eq(true), isNull(), anySet(), isNull(), eq(pageable))).thenReturn(page);
        } else {
            when(search.hybridSearch(eq(7), eq("query"), eq(true), isNull(), anySet(), isNull(), eq(0), eq(20))).thenReturn(page);
        }
        personalState();
        var row = searchController.search(principal(), "query", type, "global", null, null, null, pageable)
                .getBody().getContent().getFirst();
        assertTrue(row.liked());
        assertTrue(row.collected());
        assertEquals(folderUid, row.folderId());
        assertEquals(articleUid.toString(), row.article().id());
    }

    private void personalState() {
        when(states.findStates(7, List.of(articleUid))).thenReturn(Map.of(articleUid, new ArticleBehaviorState(true, true, folderUid)));
    }

    private ArticleSummaryView summary() {
        return new ArticleSummaryView(articleUid, 3_000_000_001L, "Title", null, "Summary", "Feed", null,
                Instant.now(), null, null, null, null);
    }

    private RecResponse recommendation() {
        return new RecResponse(articleUid, "Title", "Summary", null, "Feed", null, null, null, null, "home", 1.0, null);
    }

    private UserPrincipal principal() {
        var user = new User();
        user.setId(7);
        return new UserPrincipal(user);
    }
}
