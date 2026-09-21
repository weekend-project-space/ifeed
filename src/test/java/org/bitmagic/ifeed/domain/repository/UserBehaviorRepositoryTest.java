package org.bitmagic.ifeed.domain.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "IFEED_HISTORY_TEST_JDBC_URL", matches = "jdbc:postgresql:.*")
class UserBehaviorRepositoryTest {

    private Connection connection;
    private JdbcTemplate jdbcTemplate;
    private UserReadHistoryRepository repository;
    private final UUID articleUid = UUID.randomUUID();
    private final Instant readAt = Instant.parse("2026-09-21T08:00:00Z");

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(System.getenv("IFEED_HISTORY_TEST_JDBC_URL"),
                System.getenv().getOrDefault("IFEED_HISTORY_TEST_USER", "postgres"),
                System.getenv().getOrDefault("IFEED_HISTORY_TEST_PASSWORD", "history-test"));
        connection.setAutoCommit(false);
        jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        var schema = "history_test_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE SCHEMA " + schema);
        jdbcTemplate.execute("SET LOCAL search_path TO " + schema);
        jdbcTemplate.execute("CREATE TABLE users (id INTEGER PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE feeds (id INTEGER PRIMARY KEY, title TEXT)");
        jdbcTemplate.execute("""
                CREATE TABLE articles (id BIGINT PRIMARY KEY, uid UUID UNIQUE NOT NULL, feed_id INTEGER,
                    title TEXT, summary TEXT, thumbnail TEXT, pub_date TIMESTAMPTZ NOT NULL,
                    tags TEXT, category TEXT)
                """);
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V3__migrate_read_history_to_postgres.sql"));
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V4__add_collections_and_likes.sql"));
        jdbcTemplate.update("INSERT INTO users (id) VALUES (7), (8)");
        jdbcTemplate.update("INSERT INTO feeds (id, title) VALUES (1, 'Feed')");
        jdbcTemplate.update("INSERT INTO articles (id, uid, feed_id, title, pub_date) VALUES (?, ?, 1, 'Article', now())",
                3_000_000_001L, articleUid);
        jdbcTemplate.update("INSERT INTO articles (id, uid, feed_id, title, pub_date) VALUES (?, ?, 1, 'Other', now())",
                3_000_000_002L, UUID.randomUUID());
        repository = new UserReadHistoryRepository(jdbcTemplate);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void upsertsKeepLatestTimestampAndUseLongInternalIds() {
        repository.upsert(7L, 3_000_000_001L, readAt);
        repository.upsert(7L, 3_000_000_001L, readAt.minusSeconds(60));
        repository.upsertAll(7L, Map.of(3_000_000_001L, readAt.plusSeconds(60)));

        var history = repository.findRecent(7L, null, 10);

        assertEquals(1, history.size());
        assertEquals(3_000_000_001L, history.getFirst().article().articleId());
        assertEquals(articleUid, history.getFirst().article().id());
        assertEquals(readAt.plusSeconds(60), history.getFirst().readAt());
        assertTrue(repository.findRecent(8L, null, 10).isEmpty());
        assertTrue(repository.findRecent(7L, readAt.plusSeconds(61), 10).isEmpty());
    }

    @Test
    void paginatesWithStableTieOrderAndPreservesCountsBeyondLastPage() {
        repository.upsert(7L, 3_000_000_001L, readAt);
        repository.upsert(7L, 3_000_000_002L, readAt);
        repository.upsert(8L, 3_000_000_001L, readAt);

        var first = repository.findByUserId(7L, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "readAt")));
        var second = repository.findByUserId(7L, PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "readAt")));
        var beyond = repository.findByUserId(7L, PageRequest.of(2, 1));

        assertEquals(articleUid.toString(), first.getContent().getFirst().articleId());
        assertNotEquals(first.getContent().getFirst().articleId(), second.getContent().getFirst().articleId());
        assertEquals(2, first.getTotalElements());
        assertEquals(2, beyond.getTotalElements());
        assertTrue(beyond.isEmpty());
        assertNull(first.getContent().getFirst().summary());
    }

    @Test
    void deletesAreScopedAndForeignKeysCascade() {
        repository.upsert(7L, 3_000_000_001L, readAt);
        repository.upsert(8L, 3_000_000_001L, readAt);
        repository.upsert(7L, 3_000_000_002L, readAt);

        assertEquals(1, repository.delete(7L, 3_000_000_001L));
        assertEquals(0, repository.delete(7L, 3_000_000_001L));
        assertEquals(1, repository.findRecent(8L, null, 10).size());

        jdbcTemplate.update("DELETE FROM articles WHERE id = ?", 3_000_000_001L);
        assertTrue(repository.findRecent(8L, null, 10).isEmpty());
        jdbcTemplate.update("DELETE FROM users WHERE id = 7");
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT count(*) FROM user_read_history", Long.class));
    }

    @Test
    void missingArticleCannotCreateOrphanHistory() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.upsert(7L, 999L, readAt));
    }

    @Test
    void movingCollectionsPreservesTimestampAndOmittedFolderPreservesLocation() {
        var collections = new UserCollectionRepository(jdbcTemplate);
        var folder = collections.createFolder(7L, "Folder");
        var folderId = collections.findFolder(7L, folder.folderId(), true).orElseThrow().id();
        var first = collections.save(7L, 3_000_000_001L, null, false);
        var moved = collections.save(7L, 3_000_000_001L, folderId, true);
        var unchanged = collections.save(7L, 3_000_000_001L, null, false);

        assertEquals(first.collectedAt(), moved.collectedAt());
        assertEquals(folder.folderId(), unchanged.folderId());
        assertEquals(first.collectedAt(), unchanged.collectedAt());
        assertEquals(unchanged, collections.findState(7L, 3_000_000_001L).orElseThrow());
        assertTrue(collections.findState(8L, 3_000_000_001L).isEmpty());
        var query = BehaviorPageQuery.of(PageRequest.of(0, 20), "collectedAt", Map.of("collectedAt", "collected_at"));
        assertEquals(1, collections.list(7L, folderId, true, query).getTotalElements());
        assertTrue(collections.list(7L, null, true, query).isEmpty());
        assertNull(collections.save(7L, 3_000_000_001L, null, true).folderId());
    }

    @Test
    void folderDeletionReturnsCollectionsToDefaultAndPreservesTimestamp() {
        var collections = new UserCollectionRepository(jdbcTemplate);
        var folder = collections.createFolder(7L, "Folder");
        var folderId = collections.findFolder(7L, folder.folderId(), true).orElseThrow().id();
        var collected = collections.save(7L, 3_000_000_001L, folderId, true);
        var renamed = collections.renameFolder(7L, folderId, "Renamed");
        assertEquals("Renamed", renamed.name());
        assertEquals(folder.createdAt(), renamed.createdAt());

        collections.deleteFolder(7L, folderId);

        assertTrue(collections.findFolder(7L, folder.folderId(), false).isEmpty());
        var query = BehaviorPageQuery.of(PageRequest.of(0, 20), "collectedAt", Map.of("collectedAt", "collected_at"));
        var remaining = collections.list(7L, null, true, query).getContent().getFirst();
        assertNull(remaining.folderId());
        assertNull(collections.findState(7L, 3_000_000_001L).orElseThrow().folderId());
        assertEquals(collected.collectedAt(), remaining.collectedAt());
    }

    @Test
    void foreignKeyRejectsAnotherUsersFolder() {
        var collections = new UserCollectionRepository(jdbcTemplate);
        var folder = collections.createFolder(8L, "Private");
        var folderId = collections.findFolder(8L, folder.folderId(), true).orElseThrow().id();

        assertTrue(collections.findFolder(7L, folder.folderId(), true).isEmpty());
        assertThrows(DataIntegrityViolationException.class, () -> collections.save(7L, 3_000_000_001L, folderId, true));
    }

    @Test
    void folderNamesAreUniquePerUserAndPaginated() {
        var collections = new UserCollectionRepository(jdbcTemplate);
        collections.createFolder(7L, "Same");
        collections.createFolder(8L, "Same");
        var query = BehaviorPageQuery.of(PageRequest.of(0, 20), "createdAt", Map.of("createdAt", "created_at"));

        assertEquals(1, collections.listFolders(7L, query).getTotalElements());
        assertThrows(DataIntegrityViolationException.class, () -> collections.createFolder(7L, "Same"));
    }

    @Test
    void collectionImportIsRepeatableAndNeverOverwritesExistingFolderOrTime() {
        var collections = new UserCollectionRepository(jdbcTemplate);
        collections.importAll(7L, Map.of(3_000_000_001L, readAt));
        var folder = collections.createFolder(7L, "Folder");
        var folderId = collections.findFolder(7L, folder.folderId(), true).orElseThrow().id();
        collections.save(7L, 3_000_000_001L, folderId, true);
        collections.importAll(7L, Map.of(3_000_000_001L, readAt.minusSeconds(100)));

        var saved = collections.save(7L, 3_000_000_001L, null, false);
        assertEquals(readAt, saved.collectedAt());
        assertEquals(folder.folderId(), saved.folderId());
    }

    @Test
    void likesAreIdempotentAndIndependentOfCollections() {
        var likes = new UserLikeRepository(jdbcTemplate);
        var collections = new UserCollectionRepository(jdbcTemplate);
        collections.save(7L, 3_000_000_001L, null, false);
        var first = likes.save(7L, 3_000_000_001L);
        assertTrue(likes.exists(7L, 3_000_000_001L));
        assertFalse(likes.exists(8L, 3_000_000_001L));
        assertEquals(first, likes.save(7L, 3_000_000_001L));
        var query = BehaviorPageQuery.of(PageRequest.of(0, 20), "likedAt", Map.of("likedAt", "liked_at"));
        assertEquals(1, likes.list(7L, query).getTotalElements());
        assertEquals(articleUid, likes.list(7L, query).getContent().getFirst().articleId());
        assertTrue(likes.list(8L, query).isEmpty());

        likes.delete(7L, 3_000_000_001L);
        assertFalse(likes.exists(7L, 3_000_000_001L));
        likes.delete(7L, 3_000_000_001L);

        assertTrue(likes.list(7L, query).isEmpty());
        assertTrue(collections.exists(7L, 3_000_000_001L));
    }

    @Test
    void batchPreferenceLookupsAreScopedToUserAndRequestedNumericArticleIds() {
        var likes = new UserLikeRepository(jdbcTemplate);
        var collections = new UserCollectionRepository(jdbcTemplate);
        likes.save(7L, 3_000_000_001L);
        likes.save(8L, 3_000_000_002L);
        collections.save(7L, 3_000_000_002L, null, false);
        collections.save(8L, 3_000_000_001L, null, false);
        var ids = List.of(3_000_000_001L, 3_000_000_002L, 9_000_000_000L);

        assertEquals(Set.of(3_000_000_001L), likes.findArticleIds(7L, ids));
        assertEquals(Set.of(3_000_000_002L), collections.findArticleIds(7L, ids));
        assertTrue(likes.findArticleIds(7L, List.of(3_000_000_002L)).isEmpty());
        assertTrue(collections.findArticleIds(7L, List.of(3_000_000_001L)).isEmpty());
        assertTrue(likes.findArticleIds(7L, List.of()).isEmpty());
        assertTrue(collections.findArticleIds(7L, List.of()).isEmpty());
        assertEquals(Set.of(3_000_000_001L), likes.findArticleIds(7L, List.of(3_000_000_001L, 3_000_000_001L)));

        likes.delete(7L, 3_000_000_001L);
        collections.delete(7L, 3_000_000_002L);
        assertTrue(likes.findArticleIds(7L, ids).isEmpty());
        assertTrue(collections.findArticleIds(7L, ids).isEmpty());
        assertEquals(Set.of(3_000_000_002L), likes.findArticleIds(8L, ids));
        assertEquals(Set.of(3_000_000_001L), collections.findArticleIds(8L, ids));
    }

    @Test
    void recentLikesRespectWindowLimitStableOrderingAndUserIsolation() {
        var likes = new UserLikeRepository(jdbcTemplate);
        likes.save(7L, 3_000_000_002L);
        likes.save(7L, 3_000_000_001L);
        likes.save(8L, 3_000_000_001L);
        jdbcTemplate.update("UPDATE user_likes SET liked_at = ? WHERE user_id = 7", Timestamp.from(readAt));
        jdbcTemplate.update("UPDATE articles SET tags = '[\"database\"]', category = 'Technology' WHERE id = ?", 3_000_000_001L);

        var tied = likes.findRecent(7L, null, 10);
        assertEquals(List.of(3_000_000_002L, 3_000_000_001L), tied.stream().map(article -> article.articleId()).toList());
        assertEquals(1, likes.findRecent(8L, null, 10).size());
        assertEquals(1, likes.findRecent(7L, null, 1).size());
        assertEquals(2, likes.findRecent(7L, readAt, 10).size());
        assertTrue(likes.findRecent(7L, readAt.plusSeconds(1), 10).isEmpty());
        assertTrue(likes.findRecent(7L, null, 0).isEmpty());
        assertTrue(likes.findRecent(7L, null, -1).isEmpty());

        jdbcTemplate.update("UPDATE user_likes SET liked_at = ? WHERE user_id = 7 AND article_id = ?",
                Timestamp.from(readAt.plusSeconds(60)), 3_000_000_001L);
        var latest = likes.findRecent(7L, readAt.plusSeconds(1), 1).getFirst();
        assertEquals(articleUid, latest.id());
        assertEquals(3_000_000_001L, latest.articleId());
        assertEquals("Feed", latest.feedTitle());
        assertEquals("[\"database\"]", latest.tags());
        assertEquals("Technology", latest.category());
        assertNull(latest.summary());

        likes.delete(7L, 3_000_000_001L);
        assertTrue(likes.findRecent(7L, readAt.plusSeconds(1), 1).isEmpty());
    }
}
