package org.bitmagic.ifeed.domain.repository.tracking;

import jakarta.persistence.EntityManager;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrack;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrackSummary;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrackSubscription;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "IFEED_TEST_PG_URL", matches = ".+")
class TopicTrackSummaryRepositoryTest {
    private SingleConnectionDataSource dataSource;
    private StandardServiceRegistry registry;
    private SessionFactoryImplementor sessionFactory;
    private EntityManager entityManager;
    private TopicTrackSummaryRepository repository;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() throws Exception {
        var connection = DriverManager.getConnection(System.getenv("IFEED_TEST_PG_URL"),
                System.getenv().getOrDefault("IFEED_TEST_PG_USER", "postgres"),
                System.getenv().getOrDefault("IFEED_TEST_PG_PASSWORD", ""));
        connection.setAutoCommit(false);
        dataSource = new SingleConnectionDataSource(connection, true);
        jdbc = new JdbcTemplate(dataSource);
        String schema = "topic_page_test_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE SCHEMA " + schema);
        jdbc.execute("SET LOCAL search_path TO " + schema);
        jdbc.execute("""
                CREATE TABLE topic_tracks(id bigint PRIMARY KEY, topic_uid uuid, config_version integer);
                CREATE TABLE topic_track_summaries(id bigint PRIMARY KEY, summary_uid uuid, topic_track_id bigint,
                    topic_name text, keyword text, summary text, config_version integer, article_count integer,
                    created_at timestamptz, titles_only boolean);
                CREATE TABLE topic_track_subscriptions(id bigint PRIMARY KEY, topic_track_id bigint, user_id integer);
                CREATE TABLE feeds(id integer PRIMARY KEY, title text, icon text);
                CREATE TABLE articles(id bigint PRIMARY KEY, uid uuid, title text, link text, feed_id integer, pub_date timestamptz);
                CREATE TABLE topic_track_summary_articles(summary_id bigint, article_id bigint, PRIMARY KEY(summary_id,article_id));
                """);
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.datasource", dataSource)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .build();
        sessionFactory = (SessionFactoryImplementor) new MetadataSources(registry)
                .addAnnotatedClass(TopicTrack.class).addAnnotatedClass(TopicTrackSummary.class)
                .addAnnotatedClass(TopicTrackSubscription.class).buildMetadata().buildSessionFactory();
        entityManager = sessionFactory.createEntityManager();
        repository = new JpaRepositoryFactory(entityManager).getRepository(TopicTrackSummaryRepository.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (entityManager != null) entityManager.close();
        if (sessionFactory != null) sessionFactory.close();
        if (registry != null) StandardServiceRegistryBuilder.destroy(registry);
        if (dataSource != null) {
            try { dataSource.getConnection().rollback(); }
            finally { dataSource.destroy(); }
        }
    }

    @Test
    void summariesWithIdenticalTimestampsPageByIdAndIgnoreNewerInsertionsAndOldConfigs() {
        UUID topicUid = UUID.randomUUID();
        jdbc.update("INSERT INTO topic_tracks VALUES (1, ?, 2)", topicUid);
        jdbc.update("INSERT INTO topic_tracks VALUES (2, ?, 1)", UUID.randomUUID());
        jdbc.update("INSERT INTO topic_track_subscriptions VALUES (1,1,42)");
        for (long id = 1; id <= 5; id++) {
            insertSummary(id, id == 5 ? 2 : 1, id == 4 ? 1 : 2);
        }
        var first = repository.findSummaries(null, 42, null, null, PageRequest.of(0, 2));
        assertEquals(List.of(3L, 2L), first.stream().map(TopicTrackSummaryRepository.SummaryRow::getId).toList());
        var anchor = first.getLast();
        insertSummary(6, 1, 2);
        var second = repository.findSummaries(null, 42, anchor.getCreatedAt(), anchor.getId(), PageRequest.of(0, 2));
        assertEquals(List.of(1L), second.stream().map(TopicTrackSummaryRepository.SummaryRow::getId).toList());
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), anchor.getCreatedAt());
        assertEquals(anchor.getId(), repository.findActiveSummary(anchor.getSummaryUid()).orElseThrow().getId());
        assertEquals(4, repository.findSummaries(topicUid, null, null, null, PageRequest.of(0, 10)).size());
    }

    @Test
    void articleBatchesRespectSummaryMembershipCursorAndDeletion() {
        UUID firstUid = UUID.randomUUID();
        jdbc.update("INSERT INTO feeds VALUES (1,'来源',null)");
        for (long id = 1; id <= 8; id++) {
            jdbc.update("INSERT INTO articles VALUES (?, ?, '文章', 'https://example.com', 1, '2026-10-01T00:00:00Z')",
                    id, id == 1 ? firstUid : UUID.randomUUID());
            jdbc.update("INSERT INTO topic_track_summary_articles VALUES (10, ?)", id);
        }
        var first = repository.findArticles(10L, 0L, PageRequest.of(0, 6));
        assertEquals(6, first.size());
        assertEquals(firstUid, first.getFirst().getArticleId());
        assertEquals("来源", first.getFirst().getFeedName());
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), first.getFirst().getPublishedAt());
        assertEquals(1L, repository.findArticleCursor(10L, firstUid).orElseThrow());
        assertTrue(repository.findArticleCursor(20L, firstUid).isEmpty());
        jdbc.update("DELETE FROM articles WHERE id=7");
        var next = repository.findArticles(10L, first.getLast().getId(), PageRequest.of(0, 30));
        assertEquals(List.of(8L), next.stream().map(TopicTrackSummaryRepository.ArticleRow::getId).toList());
    }

    private void insertSummary(long id, long topicId, int configVersion) {
        jdbc.update("""
                INSERT INTO topic_track_summaries VALUES (?, ?, ?, '话题', '关键词', '总结', ?, 100,
                    '2026-10-01T00:00:00Z', true)
                """, id, UUID.randomUUID(), topicId, configVersion);
    }
}
