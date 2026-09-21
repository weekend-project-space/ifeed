package org.bitmagic.ifeed.infrastructure.migration;

import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.record.ArticleTitle;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBehaviorMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private UserReadHistoryRepository historyRepository;
    @Mock
    private UserCollectionRepository collectionRepository;

    private UserBehaviorMigration migration;

    @BeforeEach
    void setUp() {
        migration = new UserBehaviorMigration(mongoTemplate, userRepository, articleRepository,
                historyRepository, collectionRepository);
    }

    @Test
    void deduplicatesMapsInternalIdsAndReportsInvalidAndMissingRecords() {
        var articleUid = UUID.randomUUID();
        var missingUid = UUID.randomUUID();
        var newer = Instant.parse("2026-09-21T08:00:00Z");
        var document = UserBehaviorDocument.builder().id("7").readHistory(new ArrayList<>(List.of(
                reference(articleUid.toString(), newer.minusSeconds(60)),
                reference(articleUid.toString(), newer),
                reference(missingUid.toString(), newer),
                reference("invalid", newer),
                reference(articleUid.toString(), null)))).build();
        document.getReadHistory().add(null);
        var closed = new AtomicBoolean();
        when(mongoTemplate.stream(any(Query.class), eq(UserBehaviorDocument.class)))
                .thenReturn(Stream.of(document).onClose(() -> closed.set(true)));
        when(userRepository.existsById(7)).thenReturn(true);
        when(articleRepository.findIdByUIdIn(anyCollection()))
                .thenReturn(List.of(new ArticleTitle(articleUid, 3_000_000_001L, "Article")));

        var result = migration.migrateHistory();

        verify(historyRepository).upsertAll(7L, Map.of(3_000_000_001L, newer));
        assertEquals(1, result.users());
        assertEquals(1, result.upsertedRecords());
        assertEquals(3, result.invalidRecords());
        assertEquals(1, result.missingArticles());
        assertEquals(1, result.duplicateRecords());
        assertEquals(6, document.getReadHistory().size());
        assertTrue(closed.get());
        verify(mongoTemplate, only()).stream(any(Query.class), eq(UserBehaviorDocument.class));
    }

    @Test
    void skipsMissingOrInvalidUsers() {
        when(mongoTemplate.stream(any(Query.class), eq(UserBehaviorDocument.class))).thenReturn(Stream.of(
                UserBehaviorDocument.builder().id("invalid").build(),
                UserBehaviorDocument.builder().id("0").build(),
                UserBehaviorDocument.builder().id("7").build()));

        var result = migration.migrateHistory();

        assertEquals(2, result.invalidUsers());
        assertEquals(1, result.missingUsers());
        assertEquals(0, result.upsertedRecords());
        verifyNoInteractions(articleRepository, historyRepository);
    }

    @Test
    void databaseFailureStopsMigrationAndClosesCursor() {
        var document = UserBehaviorDocument.builder().id("7").readHistory(List.of(
                reference(UUID.randomUUID().toString(), Instant.now()))).build();
        var closed = new AtomicBoolean();
        when(mongoTemplate.stream(any(Query.class), eq(UserBehaviorDocument.class)))
                .thenReturn(Stream.of(document).onClose(() -> closed.set(true)));
        when(userRepository.existsById(7)).thenReturn(true);
        when(articleRepository.findIdByUIdIn(anyCollection()))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));

        assertThrows(DataAccessResourceFailureException.class, migration::migrateHistory);

        assertTrue(closed.get());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void collectionMigrationKeepsFirstCollectionTimeAndDoesNotTouchHistory() {
        var articleUid = UUID.randomUUID();
        var first = Instant.parse("2026-09-21T08:00:00Z");
        var document = UserBehaviorDocument.builder().id("7").collections(List.of(
                reference(articleUid.toString(), first.plusSeconds(60)), reference(articleUid.toString(), first))).build();
        when(mongoTemplate.stream(any(Query.class), eq(UserBehaviorDocument.class))).thenReturn(Stream.of(document));
        when(userRepository.existsById(7)).thenReturn(true);
        when(articleRepository.findIdByUIdIn(anyCollection()))
                .thenReturn(List.of(new ArticleTitle(articleUid, 42L, "Article")));

        var result = migration.migrateCollections();

        verify(collectionRepository).importAll(7L, Map.of(42L, first));
        verifyNoInteractions(historyRepository);
        assertEquals(1, result.upsertedRecords());
        assertEquals(1, result.duplicateRecords());
    }

    private UserBehaviorDocument.ArticleRef reference(String articleId, Instant timestamp) {
        return UserBehaviorDocument.ArticleRef.builder().articleId(articleId).timestamp(timestamp).build();
    }
}
