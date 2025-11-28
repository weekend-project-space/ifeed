package org.bitmagic.ifeed;

import org.bitmagic.ifeed.config.properties.RssFetcherProperties;
import org.bitmagic.ifeed.infrastructure.FreshnessCalculator;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.TextSearchRetrievalHandler;
import org.bitmagic.ifeed.infrastructure.text.search.pg.PgTextSearchStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@EnableAsync
public class IFeedApplication {

    @Configuration
    public static class CorsMvcConfigurer implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {

            registry.addMapping("/api/**")
                    .allowedOrigins("https://www.ifeed.cc", "http://localhost:5173")
                    .allowedMethods("PUT", "DELETE", "POST", "GET", "PATCH", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true).maxAge(3600);

            // Add more mappings...
        }

    }

    @Configuration
    public class SchedulerConfig implements SchedulingConfigurer {

        @Override
        public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
            taskRegistrar.setScheduler(taskExecutor());
        }

        @Bean
        public ExecutorService taskExecutor() {
            return Executors.newScheduledThreadPool(
                    Runtime.getRuntime().availableProcessors() * 2
            );
        }
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Bean
    public PgTextSearchStore textSearchStore(JdbcTemplate jdbcTemplate) {
        return new PgTextSearchStore(jdbcTemplate, "article_tsv_store", null);
    }

    @Bean
    public TextSearchRetrievalHandler bm25RetrievalHandler(PgTextSearchStore pgTextSearchStore) {
        return new TextSearchRetrievalHandler(pgTextSearchStore);
    }

    @Bean
    public HttpClient rssHttpClient(RssFetcherProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Bean
    FreshnessCalculator freshnessCalculator() {
        return new FreshnessCalculator(3, FreshnessCalculator.TimeUnit.DAYS);
    }

    public static void main(String[] args) {
        SpringApplication.run(IFeedApplication.class, args);
    }

}
