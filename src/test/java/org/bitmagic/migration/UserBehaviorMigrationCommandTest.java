package org.bitmagic.migration;

import com.mongodb.client.MongoClient;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.infrastructure.migration.UserBehaviorMigration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "IFEED_HISTORY_TEST_JDBC_URL", matches = "jdbc:postgresql:.*")
class UserBehaviorMigrationCommandTest {

    @Test
    void standaloneMigrationStartsWithoutWebOrScheduledApplicationServices() {
        var mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.getConverter()).thenReturn(mock(MongoConverter.class));
        when(mongoTemplate.stream(any(Query.class), eq(UserBehaviorDocument.class)))
                .thenAnswer(invocation -> Stream.empty());
        new ApplicationContextRunner()
                .withUserConfiguration(UserBehaviorMigrationCommand.MigrationConfiguration.class)
                .withBean(MongoTemplate.class, () -> mongoTemplate)
                .withBean(MongoClient.class, () -> mock(MongoClient.class))
                .withPropertyValues(
                        "ifeed.migration.user-behavior.enabled=true",
                        "spring.datasource.url=" + System.getenv("IFEED_HISTORY_TEST_JDBC_URL"),
                        "spring.datasource.username=" + System.getenv().getOrDefault("IFEED_HISTORY_TEST_USER", "postgres"),
                        "spring.datasource.password=" + System.getenv().getOrDefault("IFEED_HISTORY_TEST_PASSWORD", "history-test"),
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.flyway.enabled=false",
                        "spring.data.mongodb.database=history_test")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var migration = context.getBean(UserBehaviorMigration.class);
                    assertEquals(0, migration.migrateHistory().upsertedRecords());
                    assertEquals(0, migration.migrateCollections().upsertedRecords());
                    assertFalse(context.containsBean("feedIngestionScheduler"));
                    assertFalse(context.containsBean("userHistoryController"));
                    assertFalse(context.containsBean("userCollectionController"));
                    assertFalse(context.containsBean("userBehaviorRepository"));
                });
    }
}
