package org.bitmagic.ifeed.domain.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.TermUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author yangrd
 * @date 2025/11/19
 **/
@Deprecated
@Repository
@Slf4j
@RequiredArgsConstructor
public class ArticleTsvRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final static String UPDATE_VECTOR_SQL = """
            UPDATE articles SET tsv =
                  setweight(to_tsvector('simple', COALESCE(:title, '')), 'A') ||
                  setweight(to_tsvector('simple', COALESCE(:category,'')), 'A') ||
                  setweight(to_tsvector('simple', COALESCE(:author, '')), 'A') ||
                  setweight(to_tsvector('simple', COALESCE(:tags, '')), 'B') ||
                  setweight(to_tsvector('simple', COALESCE(:summary, '')), 'C') ||
                  setweight(to_tsvector('simple', COALESCE(:content, '')), 'C')
              WHERE id = :id
            """;


    public int updateSearchVector(Long id, String title, String category, String tags, String summary, String author, String content) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("title", title != null ? TermUtils.segmentStr(title) : "")
                    .addValue("category", category != null ? TermUtils.segmentStr(category) : "")
                    .addValue("tags", TermUtils.segmentStr(tags))
                    .addValue("summary", TermUtils.segmentStr(summary))
                    .addValue("author", TermUtils.segmentStr(author))
                    .addValue("content", TermUtils.segmentStr(content));

            int updated = namedParameterJdbcTemplate.update(UPDATE_VECTOR_SQL, params);

            if (updated > 0) {
                log.debug("Successfully updated search vector for article id: {}", id);
            } else {
                log.warn("No article found with id: {}", id);
            }

            return updated;
        } catch (Exception e) {
            log.error("Failed to update search vector for article id: {}", id, e);
            throw new RuntimeException("Failed to update search vector", e);
        }
    }
}
