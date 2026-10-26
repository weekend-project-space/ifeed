package org.bitmagic.ifeed.config.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TopicTrackingPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class);

    @Test
    void defaultsAndSupportedCustomLimitsBindSuccessfully() {
        contextRunner.run(context -> assertThat(context).hasNotFailed());
        contextRunner.withPropertyValues("app.topic-tracking.max-categories=2", "app.topic-tracking.max-sources-per-category=20",
                        "app.topic-tracking.summary-input-max-bytes=8192", "app.topic-tracking.summary-max-tokens=512")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TopicTrackingProperties.class).getMaxCategories()).isEqualTo(2);
                    assertThat(context.getBean(TopicTrackingProperties.class).getSummaryInputMaxBytes()).isEqualTo(8192);
                    assertThat(context.getBean(TopicTrackingProperties.class).getSummaryMaxTokens()).isEqualTo(512);
                });
    }

    @Test
    void rejectsLimitsThatCannotBeSavedByTheApi() {
        for (String property : new String[]{"max-categories=0", "max-categories=4", "max-sources-per-category=0", "max-sources-per-category=31",
                "summary-content-article-limit=0", "summary-content-article-limit=31", "cluster-similarity-threshold=0",
                "cluster-similarity-threshold=1.1", "summary-text-max-length=0", "summary-text-max-length=10001",
                "summary-input-max-bytes=4095", "summary-input-max-bytes=1000001", "summary-max-tokens=0", "summary-max-tokens=32769",
                "embedding-batch-size=0", "embedding-batch-size=129", "embedding-text-max-length=0", "embedding-text-max-length=10001",
                "history-days=0", "candidate-page-size=0", "candidate-page-size=1001"}) {
            contextRunner.withPropertyValues("app.topic-tracking." + property)
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TopicTrackingProperties.class)
    static class TestConfiguration {}
}
