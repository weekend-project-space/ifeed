package org.bitmagic.ifeed.application.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.retrieval.DocScore;
import org.bitmagic.ifeed.application.retrieval.RetrievalContext;
import org.bitmagic.ifeed.application.retrieval.RetrievalHandler;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
@RequiredArgsConstructor
@Slf4j
public class Bm25RetrievalHandler  implements RetrievalHandler {

    private final JdbcTemplate jdbcTemplate;
    private final String sql;

    @Override
    public boolean supports(RetrievalContext context) {
        return context.getQuery() != null && !context.getQuery().isEmpty();
    }

    @Override
    public List<DocScore> handle(RetrievalContext context) {
        log.debug("Executing BM25 retrieval: {}", context.getQuery());

        List<DocScore> rows = jdbcTemplate.query(sql,
                (rs, rowNum) -> new DocScore(
                        rs.getLong("id"),
                        rs.getDouble("score"),
                        rs.getTimestamp("pubDate").toInstant(),
                        rs.getString("title")),
                context.getQuery(),
                context.isIncludeGlobal(),
                context.getUserId(),
                context.getTopK()
        );
        return rows;
    }
}
