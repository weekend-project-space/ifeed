package org.bitmagic.ifeed.infrastructure.vector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.pgvector.PGvector;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.ai.vectorstore.pgvector.PgVectorFilterExpressionConverter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
@Slf4j
public class PgVectorStoreTurbo implements VectorStoreTurbo {

    @Delegate
    private final PgVectorStore pgVectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final String schemaName;
    private final String vectorTableName;
    private final ObjectMapper objectMapper;
    public final FilterExpressionConverter filterExpressionConverter = new PgVectorFilterExpressionConverter();

    public PgVectorStoreTurbo(PgVectorStore pgVectorStore, JdbcTemplate jdbcTemplate, String schemaName, String vectorTableName) {
        this.pgVectorStore = pgVectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.schemaName = schemaName;
        this.vectorTableName = vectorTableName;
        this.objectMapper = ((JsonMapper.Builder)JsonMapper.builder().addModules(JacksonUtils.instantiateAvailableModules())).build();
    }

    @Override
    public List<Document> similaritySearch(SearchRequestTurbo request) {
        String nativeFilterExpression = request.getFilterExpression() != null ? this.filterExpressionConverter.convertExpression(request.getFilterExpression()) : "";
        String jsonPathFilter = "";
        if (StringUtils.hasText(nativeFilterExpression)) {
            jsonPathFilter = " AND metadata::jsonb @@ '" + nativeFilterExpression + "'::jsonpath ";
        }

        double distance = 1.0 - request.getSimilarityThreshold();
        PGvector queryEmbedding = this.getQueryEmbedding(request.getEmbedding());
        return this.jdbcTemplate.query(String.format(this.getDistanceType().similaritySearchSqlTemplate, this.getFullyQualifiedTableName(), jsonPathFilter), new DocumentRowMapper(this.objectMapper), new Object[]{queryEmbedding, queryEmbedding, distance, request.getTopK()});
    }


    private PGvector getQueryEmbedding(float[] embedding) {
        return new PGvector(embedding);
    }

    private String getFullyQualifiedTableName() {
        return this.schemaName + "." + this.vectorTableName;
    }


    private static class DocumentRowMapper implements RowMapper<Document> {
        private static final String COLUMN_METADATA = "metadata";
        private static final String COLUMN_ID = "id";
        private static final String COLUMN_CONTENT = "content";
        private static final String COLUMN_DISTANCE = "distance";
        private final ObjectMapper objectMapper;

        DocumentRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        public Document mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString(COLUMN_ID);
            String content = rs.getString(COLUMN_CONTENT);
            String pgMetadata = rs.getString(COLUMN_METADATA);
            Float distance = rs.getFloat(COLUMN_DISTANCE);
            Map<String, Object> metadata = this.toMap(pgMetadata);
            metadata.put(DocumentMetadata.DISTANCE.value(), distance);
            return Document.builder().id(id).text(content).metadata(metadata).score(1.0 - (double) distance).build();
        }

        private Map<String, Object> toMap(String source) {

            try {
                return (Map) this.objectMapper.readValue(source, Map.class);
            } catch (JsonProcessingException var4) {
                throw new RuntimeException(var4);
            }
        }
    }
}
