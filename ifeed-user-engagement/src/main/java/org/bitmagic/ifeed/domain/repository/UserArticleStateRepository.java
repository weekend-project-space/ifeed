package org.bitmagic.ifeed.domain.repository;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.record.ArticleBehaviorState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserArticleStateRepository {
    private final JdbcTemplate jdbcTemplate;

    public Map<UUID, ArticleBehaviorState> findStates(Integer userId, List<UUID> articleUids) {
        if (userId == null || userId <= 0 || articleUids.isEmpty()) return Map.of();
        Map<UUID, ArticleBehaviorState> states = new HashMap<>();
        new NamedParameterJdbcTemplate(jdbcTemplate).query("""
                SELECT article.uid, collection.article_id IS NOT NULL AS collected,
                       liked.article_id IS NOT NULL AS liked, folder.uid AS folder_uid
                FROM articles article
                LEFT JOIN user_collections collection
                    ON collection.article_id = article.id AND collection.user_id = :userId
                LEFT JOIN user_likes liked
                    ON liked.article_id = article.id AND liked.user_id = :userId
                LEFT JOIN user_collection_folders folder
                    ON folder.id = collection.folder_id AND folder.user_id = :userId
                WHERE article.uid IN (:articleUids)
                """, Map.of("userId", userId, "articleUids", articleUids), resultSet -> {
            states.put(resultSet.getObject("uid", UUID.class), new ArticleBehaviorState(
                    resultSet.getBoolean("collected"), resultSet.getBoolean("liked"),
                    resultSet.getObject("folder_uid", UUID.class)));
        });
        return states;
    }
}
