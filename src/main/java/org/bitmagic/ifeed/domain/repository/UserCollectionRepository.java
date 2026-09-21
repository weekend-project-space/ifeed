package org.bitmagic.ifeed.domain.repository;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.CollectionFolderResponse;
import org.bitmagic.ifeed.api.response.CollectionItemResponse;
import org.bitmagic.ifeed.api.response.CollectionStateResponse;
import org.bitmagic.ifeed.domain.record.ArticleSummary;
import org.bitmagic.ifeed.domain.record.CollectionFolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserCollectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<CollectionFolder> findFolder(Long userId, UUID folderUid, boolean lock) {
        String sql = "SELECT * FROM user_collection_folders WHERE user_id = ? AND uid = ?"
                + (lock ? " FOR UPDATE" : "");
        return jdbcTemplate.query(sql, this::mapFolder, userId, folderUid).stream().findFirst();
    }

    public CollectionFolderResponse createFolder(Long userId, String name) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO user_collection_folders (user_id, uid, name) VALUES (?, ?, ?) RETURNING *
                """, this::mapFolder, userId, UUID.randomUUID(), name).response();
    }

    public CollectionFolderResponse renameFolder(Long userId, Long folderId, String name) {
        return jdbcTemplate.queryForObject("""
                UPDATE user_collection_folders
                SET updated_at = CASE WHEN name <> ? THEN CURRENT_TIMESTAMP ELSE updated_at END, name = ?
                WHERE user_id = ? AND id = ? RETURNING *
                """, this::mapFolder, name, name, userId, folderId).response();
    }

    public void deleteFolder(Long userId, Long folderId) {
        jdbcTemplate.update("UPDATE user_collections SET folder_id = NULL WHERE user_id = ? AND folder_id = ?",
                userId, folderId);
        jdbcTemplate.update("DELETE FROM user_collection_folders WHERE user_id = ? AND id = ?", userId, folderId);
    }

    public Page<CollectionFolderResponse> listFolders(Long userId, BehaviorPageQuery query) {
        var total = jdbcTemplate.queryForObject("SELECT count(*) FROM user_collection_folders WHERE user_id = ?",
                Long.class, userId);
        var content = jdbcTemplate.query("SELECT * FROM user_collection_folders WHERE user_id = ? ORDER BY "
                        + query.orderBy() + " LIMIT ? OFFSET ?",
                (resultSet, rowNumber) -> mapFolder(resultSet, rowNumber).response(),
                userId, query.pageable().getPageSize(), query.pageable().getOffset());
        return new PageImpl<>(content, query.pageable(), total);
    }

    public CollectionStateResponse save(Long userId, Long articleId, Long folderId, boolean folderSpecified) {
        jdbcTemplate.update("""
                INSERT INTO user_collections (user_id, article_id, folder_id) VALUES (?, ?, ?)
                ON CONFLICT (user_id, article_id) DO UPDATE
                SET folder_id = CASE WHEN ? THEN EXCLUDED.folder_id ELSE user_collections.folder_id END
                """, userId, articleId, folderId, folderSpecified);
        return jdbcTemplate.queryForObject("""
                SELECT article.uid AS article_uid, folder.uid AS folder_uid, collection.collected_at
                FROM user_collections collection
                JOIN articles article ON article.id = collection.article_id
                LEFT JOIN user_collection_folders folder ON folder.id = collection.folder_id
                WHERE collection.user_id = ? AND collection.article_id = ?
                """, (resultSet, rowNumber) -> new CollectionStateResponse(
                resultSet.getObject("article_uid", UUID.class), resultSet.getObject("folder_uid", UUID.class),
                resultSet.getTimestamp("collected_at").toInstant()), userId, articleId);
    }

    public void importAll(Long userId, Map<Long, Instant> collections) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO user_collections (user_id, article_id, collected_at) VALUES (?, ?, ?)
                ON CONFLICT (user_id, article_id) DO NOTHING
                """, collections.entrySet(), 500, (statement, entry) -> {
            statement.setLong(1, userId);
            statement.setLong(2, entry.getKey());
            statement.setTimestamp(3, Timestamp.from(entry.getValue()));
        });
    }

    public void delete(Long userId, Long articleId) {
        jdbcTemplate.update("DELETE FROM user_collections WHERE user_id = ? AND article_id = ?", userId, articleId);
    }

    public boolean exists(Long userId, Long articleId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM user_collections WHERE user_id = ? AND article_id = ?)",
                Boolean.class, userId, articleId));
    }

    public Set<Long> findArticleIds(Long userId, List<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(new NamedParameterJdbcTemplate(jdbcTemplate).queryForList("""
                SELECT article_id FROM user_collections
                WHERE user_id = :userId AND article_id IN (:articleIds)
                """, Map.of("userId", userId, "articleIds", articleIds), Long.class));
    }

    public Page<CollectionItemResponse> list(Long userId, Long folderId, boolean filterFolder, BehaviorPageQuery query) {
        String filter = " WHERE collection.user_id = ?";
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        if (filterFolder) {
            if (folderId == null) {
                filter += " AND collection.folder_id IS NULL";
            } else {
                filter += " AND collection.folder_id = ?";
                parameters.add(folderId);
            }
        }
        var total = jdbcTemplate.queryForObject("SELECT count(*) FROM user_collections collection" + filter,
                Long.class, parameters.toArray());
        parameters.add(query.pageable().getPageSize());
        parameters.add(query.pageable().getOffset());
        var content = jdbcTemplate.query("""
                SELECT collection.id, collection.collected_at, article.uid, article.title, article.summary,
                       article.thumbnail, feed.title AS feed_title, folder.uid AS folder_uid
                FROM user_collections collection
                JOIN articles article ON article.id = collection.article_id
                LEFT JOIN feeds feed ON feed.id = article.feed_id
                LEFT JOIN user_collection_folders folder ON folder.id = collection.folder_id
                """ + filter + " ORDER BY " + query.orderBy() + " LIMIT ? OFFSET ?",
                (resultSet, rowNumber) -> new CollectionItemResponse(resultSet.getString("uid"),
                        resultSet.getString("title"), resultSet.getString("feed_title"),
                        resultSet.getString("thumbnail"), resultSet.getString("summary"),
                        resultSet.getTimestamp("collected_at").toInstant(), resultSet.getObject("folder_uid", UUID.class)),
                parameters.toArray());
        return new PageImpl<>(content, query.pageable(), total);
    }

    public List<ArticleSummary> findRecent(Long userId, Instant since, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String filter = " WHERE collection.user_id = ?";
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        if (since != null) {
            filter += " AND collection.collected_at >= ?";
            parameters.add(Timestamp.from(since));
        }
        parameters.add(limit);
        return jdbcTemplate.query("""
                SELECT article.id AS article_id, article.uid, article.title, article.summary,
                       article.thumbnail, feed.title AS feed_title, article.pub_date, article.tags, article.category
                FROM user_collections collection
                JOIN articles article ON article.id = collection.article_id
                LEFT JOIN feeds feed ON feed.id = article.feed_id
                """ + filter + " ORDER BY collection.collected_at DESC, collection.id ASC LIMIT ?",
                (resultSet, rowNumber) -> new ArticleSummary(resultSet.getObject("uid", UUID.class),
                        resultSet.getLong("article_id"), resultSet.getString("title"), resultSet.getString("summary"),
                        resultSet.getString("thumbnail"), resultSet.getString("feed_title"),
                        resultSet.getTimestamp("pub_date").toInstant(), resultSet.getString("tags"),
                        resultSet.getString("category")), parameters.toArray());
    }

    private CollectionFolder mapFolder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CollectionFolder(resultSet.getLong("id"), new CollectionFolderResponse(
                resultSet.getObject("uid", UUID.class), resultSet.getString("name"),
                resultSet.getTimestamp("created_at").toInstant(), resultSet.getTimestamp("updated_at").toInstant()));
    }
}
