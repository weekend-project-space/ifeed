package org.bitmagic.ifeed.infrastructure.recall;

import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.record.ArticleSummary;
import org.bitmagic.ifeed.domain.record.ReadHistoryArticle;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.bitmagic.ifeed.infrastructure.recall.data.UserBehaviorDataAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadHistoryRecommendationTest {

    @Mock
    private UserReadHistoryRepository historyRepository;
    @Mock
    private UserBehaviorDataAccessor dataAccessor;
    @Mock
    private UserCollectionRepository collectionRepository;
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
    void sequenceWorksWithoutMongoDocumentAndUsesNumericArticleIds() {
        var store = new PgUserSequenceStore(historyRepository, dataAccessor);
        ReflectionTestUtils.setField(store, "lookback", 200);
        ReflectionTestUtils.setField(store, "fetchMultiplier", 2);
        ReflectionTestUtils.setField(store, "maxFetchLimit", 1000);
        ReflectionTestUtils.setField(store, "recencyDecayFactor", 1.0);
        when(historyRepository.findRecent(7L, null, 200)).thenReturn(List.of(history));
        when(dataAccessor.getUserBehavior(7)).thenReturn(Optional.empty());

        var interactions = store.recentInteractions(7, 10);

        assertEquals(1, interactions.size());
        assertEquals(history.article().articleId().longValue(), interactions.getFirst().itemId());
        assertEquals(history.readAt(), interactions.getFirst().timestamp());
        assertEquals(1.0, interactions.getFirst().weight());
        verify(dataAccessor, never()).batchMapArticleIds(anyList());
    }

    @Test
    void preferenceUsesPostgresHistoryEvenWhenMongoDocumentIsAbsent() {
        var service = new UserInterestProfileService(collectionRepository, historyRepository, keywordExtractor);
        when(historyRepository.findRecent(7L, null, 20)).thenReturn(List.of(history));

        var preferences = service.topAttributes(7, 20, 10);

        assertTrue(preferences.stream().anyMatch(preference -> preference.attributeKey().equals("feedTitle")
                && preference.attributeValue().equals("Feed")));
        assertTrue(preferences.stream().anyMatch(preference -> preference.attributeKey().equals("category")
                && preference.attributeValue().equals("Technology")));
        verifyNoInteractions(dataAccessor);
    }

    @Test
    void staleMongoReadHistoryIsNotUsed() {
        var service = new UserInterestProfileService(collectionRepository, historyRepository, keywordExtractor);
        when(historyRepository.findRecent(7L, null, 20)).thenReturn(List.of());

        assertTrue(service.topAttributes(7, 20, 10).isEmpty());

        verifyNoInteractions(dataAccessor);
    }
}
