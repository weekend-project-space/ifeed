package org.bitmagic.ifeed.infrastructure.recall;

import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.domain.record.ArticleSummary;
import org.bitmagic.ifeed.domain.record.ReadHistoryArticle;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.domain.repository.UserLikeRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadHistoryRecommendationTest {

    @Mock
    private UserReadHistoryRepository historyRepository;
    @Mock
    private UserCollectionRepository collectionRepository;
    @Mock
    private UserLikeRepository likeRepository;
    @Mock
    private KeywordExtractor keywordExtractor;

    private ReadHistoryArticle history;

    @BeforeEach
    void setUp() {
        var article = new ArticleSummary(UUID.randomUUID(), 3_000_000_001L, "Article", null, null,
                "Feed", Instant.now(), null, "Technology");
        history = new ReadHistoryArticle(article, Instant.now());
    }

    @Test
    void sequenceUsesNumericArticleIdsFromPostgresHistory() {
        var store = sequenceStore();
        when(historyRepository.findRecent(7L, null, 200)).thenReturn(List.of(history));

        var interactions = store.recentInteractions(7, 10);

        assertEquals(1, interactions.size());
        assertEquals(history.article().articleId().longValue(), interactions.getFirst().itemId());
        assertEquals(history.readAt(), interactions.getFirst().timestamp());
        assertEquals(1.0, interactions.getFirst().weight());
    }

    @Test
    void preferenceUsesPostgresHistory() {
        var service = preferenceService();
        when(historyRepository.findRecent(7L, null, 20)).thenReturn(List.of(history));

        var preferences = service.topAttributes(7, 20, 10);

        assertTrue(preferences.stream().anyMatch(preference -> preference.attributeKey().equals("feedTitle")
                && preference.attributeValue().equals("Feed")));
        assertTrue(preferences.stream().anyMatch(preference -> preference.attributeKey().equals("category")
                && preference.attributeValue().equals("Technology")));
    }

    @Test
    void emptyPostgresReadHistoryProducesNoPreferences() {
        var service = preferenceService();
        when(historyRepository.findRecent(7L, null, 20)).thenReturn(List.of());

        assertTrue(service.topAttributes(7, 20, 10).isEmpty());

    }

    @Test
    void sequenceBonusesAreBatchedAndKeepReadOrderTimestamps() {
        var store = sequenceStore();
        var entries = List.of(history, reading(3_000_000_002L), reading(3_000_000_003L), reading(3_000_000_004L));
        var articleIds = entries.stream().map(entry -> entry.article().articleId()).toList();
        when(historyRepository.findRecent(7L, null, 200)).thenReturn(entries);
        when(collectionRepository.findArticleIds(7L, articleIds)).thenReturn(Set.of(articleIds.get(2), articleIds.get(3)));
        when(likeRepository.findArticleIds(7L, articleIds)).thenReturn(Set.of(articleIds.get(1), articleIds.get(3)));
        var interactions = store.recentInteractions(7, 4);

        assertEquals(articleIds, interactions.stream().map(SequenceStore.UserInteraction::itemId).toList());
        assertEquals(entries.stream().map(ReadHistoryArticle::readAt).toList(),
                interactions.stream().map(SequenceStore.UserInteraction::timestamp).toList());
        assertEquals(1.0, interactions.get(0).weight(), 1e-9);
        assertEquals(3.0 * Math.exp(-1.0 / 4), interactions.get(1).weight(), 1e-9);
        assertEquals(3.0 * Math.exp(-2.0 / 4), interactions.get(2).weight(), 1e-9);
        assertEquals(5.0 * Math.exp(-3.0 / 4), interactions.get(3).weight(), 1e-9);
        assertEquals(0.0, interactions.get(1).durationSeconds());
        assertEquals(0.0, interactions.get(3).durationSeconds());
        verify(collectionRepository).findArticleIds(7L, articleIds);
        verify(likeRepository).findArticleIds(7L, articleIds);
        verifyNoMoreInteractions(collectionRepository, likeRepository);
    }

    @Test
    void cancelledLikesAndCollectionsStopBoostingTheNextSequence() {
        var store = sequenceStore();
        var ids = List.of(history.article().articleId());
        when(historyRepository.findRecent(7L, null, 200)).thenReturn(List.of(history));
        when(likeRepository.findArticleIds(7L, ids)).thenReturn(Set.copyOf(ids), Set.of());
        when(collectionRepository.findArticleIds(7L, ids)).thenReturn(Set.copyOf(ids), Set.of());

        assertEquals(5.0, store.recentInteractions(7, 1).getFirst().weight());
        assertEquals(1.0, store.recentInteractions(7, 1).getFirst().weight());
    }

    @Test
    void sequenceOnlyChecksReturnedReadArticlesAndKeepsTheConfiguredWindow() {
        var store = sequenceStore();
        ReflectionTestUtils.setField(store, "windowDays", 30);
        ReflectionTestUtils.setField(store, "likeBonus", 4.0);
        ReflectionTestUtils.setField(store, "collectionBonus", 0.0);
        var start = Instant.now().minus(Duration.ofDays(30));
        when(historyRepository.findRecent(eq(7L), any(Instant.class), eq(200))).thenReturn(List.of(history, reading(3_000_000_002L)));
        var ids = List.of(history.article().articleId());
        when(likeRepository.findArticleIds(7L, ids)).thenReturn(Set.copyOf(ids));
        when(collectionRepository.findArticleIds(7L, ids)).thenReturn(Set.copyOf(ids));

        var interactions = store.recentInteractions(7, 1);

        assertEquals(1, interactions.size());
        assertEquals(5.0, interactions.getFirst().weight());
        verify(historyRepository).findRecent(eq(7L), argThat(cutoff -> !cutoff.isBefore(start)
                && !cutoff.isAfter(Instant.now().minus(Duration.ofDays(30)))), eq(200));
        verify(likeRepository).findArticleIds(7L, ids);
        verify(collectionRepository).findArticleIds(7L, ids);
        verifyNoMoreInteractions(likeRepository, collectionRepository);
    }

    @Test
    void emptyHistoryDoesNotReplaceTheSequenceWithLikesOrCollections() {
        var store = sequenceStore();
        assertTrue(store.recentInteractions(null, 10).isEmpty());
        assertTrue(store.recentInteractions(0, 10).isEmpty());
        assertTrue(store.recentInteractions(7, 0).isEmpty());
        verifyNoInteractions(historyRepository);

        assertTrue(store.recentInteractions(7, 10).isEmpty());

        verifyNoInteractions(collectionRepository, likeRepository);
    }

    @Test
    void preferenceCombinesSignalsWithoutCountingAnArticleMoreThanOnce() {
        var service = preferenceService();
        var read = article(3_000_000_001L);
        var liked = article(3_000_000_002L);
        var collected = article(3_000_000_003L);
        var both = article(3_000_000_004L);
        when(historyRepository.findRecent(7L, null, 20)).thenReturn(List.of(
                new ReadHistoryArticle(read, Instant.now()), new ReadHistoryArticle(both, Instant.now())));
        when(collectionRepository.findRecent(7L, null, 20)).thenReturn(List.of(collected, both));
        when(likeRepository.findRecent(7L, null, 20)).thenReturn(List.of(liked, both));

        var preferences = service.topAttributes(7, 20, 100);

        assertPreference(preferences, "feedTitle", read.feedTitle(), 1.0 / 12);
        assertPreference(preferences, "feedTitle", liked.feedTitle(), 3.0 / 12);
        assertPreference(preferences, "feedTitle", collected.feedTitle(), 3.0 / 12);
        assertPreference(preferences, "feedTitle", both.feedTitle(), 5.0 / 12);
        assertPreference(preferences, "category", both.category(), 5.0 / 12);
        assertPreference(preferences, "tag", "Tag " + both.articleId(), 5.0 / 12);
    }

    @Test
    void likesWithoutReadHistoryContributeKeywordsAndEntitiesWithinConfiguredWindow() {
        var service = preferenceService();
        ReflectionTestUtils.setField(service, "likeWindowDays", 90);
        ReflectionTestUtils.setField(service, "extractKeywords", true);
        ReflectionTestUtils.setField(service, "extractEntities", true);
        var liked = article(3_000_000_002L);
        var start = Instant.now().minus(Duration.ofDays(90));
        when(likeRepository.findRecent(eq(7L), any(Instant.class), eq(5))).thenReturn(List.of(liked));
        when(keywordExtractor.extractKeywords(liked.title(), liked.summary())).thenReturn(List.of("Database"));
        when(keywordExtractor.extractEntities(liked.title(), liked.summary())).thenReturn(List.of("PostgreSQL"));

        var preferences = service.topAttributes(7, 5, 10);

        assertPreference(preferences, "feedTitle", liked.feedTitle(), 1.0);
        assertPreference(preferences, "keyword", "Database", 1.0);
        assertPreference(preferences, "entity", "PostgreSQL", 1.0);
        verify(likeRepository).findRecent(eq(7L), argThat(cutoff -> !cutoff.isBefore(start)
                && !cutoff.isAfter(Instant.now().minus(Duration.ofDays(90)))), eq(5));
    }

    @Test
    void cancelledLikesNoLongerContributeToPreferenceAndInvalidUsersDoNotQuery() {
        var service = preferenceService();
        assertTrue(service.topAttributes(null, 20, 10).isEmpty());
        assertTrue(service.topAttributes(0, 20, 10).isEmpty());
        assertTrue(service.topAttributes(7, 20, 0).isEmpty());
        verifyNoInteractions(historyRepository, likeRepository, collectionRepository);
        when(likeRepository.findRecent(7L, null, 20)).thenReturn(List.of(history.article()), List.of());

        assertFalse(service.topAttributes(7, 20, 10).isEmpty());
        assertTrue(service.topAttributes(7, 20, 10).isEmpty());
    }

    private PgUserSequenceStore sequenceStore() {
        var store = new PgUserSequenceStore(historyRepository, collectionRepository, likeRepository);
        ReflectionTestUtils.setField(store, "lookback", 200);
        ReflectionTestUtils.setField(store, "fetchMultiplier", 2);
        ReflectionTestUtils.setField(store, "maxFetchLimit", 1000);
        ReflectionTestUtils.setField(store, "recencyDecayFactor", 1.0);
        ReflectionTestUtils.setField(store, "collectionBonus", 2.0);
        ReflectionTestUtils.setField(store, "likeBonus", 2.0);
        return store;
    }

    private UserInterestProfileService preferenceService() {
        var service = new UserInterestProfileService(collectionRepository, historyRepository, keywordExtractor, likeRepository);
        ReflectionTestUtils.setField(service, "collectionBonus", 2.0);
        ReflectionTestUtils.setField(service, "likeBonus", 2.0);
        return service;
    }

    private ArticleSummary article(long articleId) {
        return new ArticleSummary(UUID.randomUUID(), articleId, "Article " + articleId, "Summary", null,
                "Feed " + articleId, Instant.now(), "[\"Tag " + articleId + "\"]", "Category " + articleId);
    }

    private ReadHistoryArticle reading(long articleId) {
        return new ReadHistoryArticle(article(articleId), history.readAt().minusSeconds(articleId - history.article().articleId()));
    }

    private void assertPreference(List<UserPreferenceService.AttributePreference> preferences, String key, String value, double weight) {
        var matches = preferences.stream().filter(preference -> preference.attributeKey().equals(key)
                && preference.attributeValue().equals(value)).toList();
        assertEquals(1, matches.size());
        assertEquals(weight, matches.getFirst().weight(), 1e-9);
    }
}
