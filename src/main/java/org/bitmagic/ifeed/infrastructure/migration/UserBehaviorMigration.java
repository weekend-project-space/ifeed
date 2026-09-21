package org.bitmagic.ifeed.infrastructure.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ifeed.migration.user-behavior.enabled", havingValue = "true")
public class UserBehaviorMigration implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final UserReadHistoryRepository historyRepository;
    private final UserCollectionRepository collectionRepository;

    @Override
    public void run(ApplicationArguments arguments) {
        log.info("Starting MongoDB to PostgreSQL user behavior migration");
        log.info("Read history migration completed: {}", migrateHistory());
        log.info("Collection migration completed: {}", migrateCollections());
    }

    public MigrationResult migrateHistory() {
        return migrate("readHistory", false);
    }

    public MigrationResult migrateCollections() {
        return migrate("collections", true);
    }

    private MigrationResult migrate(String field, boolean collections) {
        var query = Query.query(Criteria.where(field).exists(true)).cursorBatchSize(100);
        query.fields().include(field);
        long users = 0;
        long upsertedRecords = 0;
        long invalidUsers = 0;
        long missingUsers = 0;
        long invalidRecords = 0;
        long missingArticles = 0;
        long duplicateRecords = 0;
        try (var documents = mongoTemplate.stream(query, UserBehaviorDocument.class)) {
            var iterator = documents.iterator();
            while (iterator.hasNext()) {
                var document = iterator.next();
                Integer userId;
                try {
                    userId = Integer.valueOf(document.getId());
                    if (userId <= 0) {
                        invalidUsers++;
                        continue;
                    }
                } catch (NumberFormatException exception) {
                    invalidUsers++;
                    continue;
                }
                if (!userRepository.existsById(userId)) {
                    missingUsers++;
                    continue;
                }
                users++;
                Map<UUID, Instant> timestamps = new HashMap<>();
                var references = collections ? document.getCollections() : document.getReadHistory();
                if (references != null) {
                    for (var reference : references) {
                        if (reference == null || reference.getArticleId() == null || reference.getTimestamp() == null) {
                            invalidRecords++;
                            continue;
                        }
                        UUID articleUid;
                        try {
                            articleUid = UUID.fromString(reference.getArticleId());
                        } catch (IllegalArgumentException exception) {
                            invalidRecords++;
                            continue;
                        }
                        if (timestamps.containsKey(articleUid)) {
                            duplicateRecords++;
                        }
                        timestamps.merge(articleUid, reference.getTimestamp(), (existing, incoming) -> {
                            if (collections) {
                                return existing.isBefore(incoming) ? existing : incoming;
                            }
                            return existing.isAfter(incoming) ? existing : incoming;
                        });
                    }
                }
                var articleUids = new ArrayList<>(timestamps.keySet());
                for (int offset = 0; offset < articleUids.size(); offset += 500) {
                    var batch = articleUids.subList(offset, Math.min(offset + 500, articleUids.size()));
                    var articles = articleRepository.findIdByUIdIn(batch);
                    Map<Long, Instant> history = new HashMap<>();
                    for (var article : articles) {
                        history.put(article.id(), timestamps.get(article.uid()));
                    }
                    missingArticles += batch.size() - history.size();
                    if (!history.isEmpty()) {
                        if (collections) {
                            collectionRepository.importAll(userId.longValue(), history);
                        } else {
                            historyRepository.upsertAll(userId.longValue(), history);
                        }
                        upsertedRecords += history.size();
                    }
                }
                if (users % 100 == 0) {
                    log.info("User behavior migration progress: field={}, users={}, upsertedRecords={}",
                            field, users, upsertedRecords);
                }
            }
        }
        return new MigrationResult(users, upsertedRecords, invalidUsers, missingUsers,
                invalidRecords, missingArticles, duplicateRecords);
    }

    public record MigrationResult(long users, long upsertedRecords, long invalidUsers, long missingUsers,
                                  long invalidRecords, long missingArticles, long duplicateRecords) {
    }
}
