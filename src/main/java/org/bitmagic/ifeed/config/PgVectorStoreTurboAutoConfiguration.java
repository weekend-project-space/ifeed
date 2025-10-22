package org.bitmagic.ifeed.config;

import org.bitmagic.ifeed.config.vectore.PgVectorStoreTurbo;
import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreProperties;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
@Configuration
public class PgVectorStoreTurboAutoConfiguration {

    @Bean
    @Primary
    public PgVectorStoreTurbo vectorStoreTurbo(PgVectorStore pgVectorStore, JdbcTemplate jdbcTemplate, PgVectorStoreProperties properties) {
        return new PgVectorStoreTurbo(pgVectorStore, jdbcTemplate, properties.getSchemaName(), properties.getTableName());
    }
}


