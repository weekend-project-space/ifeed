package org.bitmagic.ifeed.domain.model.tracking;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TopicTrackJsonMappingTest {

    private static StandardServiceRegistry registry;
    private static SessionFactoryImplementor sessionFactory;

    @BeforeAll
    static void setUp() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .applySetting("hibernate.type.json_format_mapper", "jackson")
                .build();
        sessionFactory = (SessionFactoryImplementor) new MetadataSources(registry)
                .addAnnotatedClass(TopicTrack.class)
                .addAnnotatedClass(TopicTrackSummary.class)
                .addAnnotatedClass(TopicTrackSubscription.class)
                .addAnnotatedClass(TopicTrackMatch.class)
                .buildMetadata().buildSessionFactory();
    }

    @AfterAll
    static void tearDown() {
        if (sessionFactory != null) sessionFactory.close();
        if (registry != null) StandardServiceRegistryBuilder.destroy(registry);
    }

    @ParameterizedTest
    @CsvSource({
            "track, includeKeywords",
            "track, blockedKeywords",
            "track, categories",
            "summary, articlePreviews"
    })
    void resolvesJsonJdbcMappingAndPreservesRawArrays(String entity, String attribute) {
        Class<?> entityClass = entity.equals("track") ? TopicTrack.class : TopicTrackSummary.class;
        var descriptor = sessionFactory.getRuntimeMetamodels().getMappingMetamodel().getEntityDescriptor(entityClass);
        var mapping = (BasicValuedModelPart) descriptor.findAttributeMapping(attribute);

        assertEquals(SqlTypes.JSON, mapping.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode());
        assertEquals(String.class, mapping.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass());

        WrapperOptions options = mock(WrapperOptions.class);
        when(options.getSessionFactory()).thenReturn(sessionFactory);
        var formatMapper = sessionFactory.getFastSessionServices().getJsonFormatMapper();
        for (String json : new String[]{"[]", "[\"医疗 AI\",\"屏蔽词\"]", "[{\"title\":\"文章\",\"articleId\":\"uuid\"}]"}) {
            String encoded = formatMapper.toString(json, StringJavaType.INSTANCE, options);
            assertEquals(json, encoded);
            assertEquals(json, formatMapper.fromString(encoded, StringJavaType.INSTANCE, options));
        }
    }

    @Test
    void topicQueriesCompileAgainstHibernateMappingsWithoutDatabaseAccess() {
        try (var session = sessionFactory.openSession()) {
            for (Class<?> repository : java.util.List.of(
                    org.bitmagic.ifeed.domain.repository.tracking.TopicTrackRepository.class,
                    org.bitmagic.ifeed.domain.repository.tracking.TopicTrackSummaryRepository.class,
                    org.bitmagic.ifeed.domain.repository.tracking.TopicTrackSubscriptionRepository.class)) {
                for (var method : repository.getMethods()) {
                    var query = method.getAnnotation(org.springframework.data.jpa.repository.Query.class);
                    if (query != null && !query.nativeQuery()) {
                        assertDoesNotThrow(() -> session.createQuery(query.value(), Object.class), method.getName());
                    }
                }
            }
        }
    }
}
