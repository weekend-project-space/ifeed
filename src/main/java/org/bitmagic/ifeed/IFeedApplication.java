package org.bitmagic.ifeed;

import org.bitmagic.ifeed.config.properties.RssFetcherProperties;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.infrastructure.FreshnessCalculator;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.TextSearchRetrievalHandler;
import org.bitmagic.ifeed.infrastructure.retrieval.impl.VectorRetrievalHandler;
import org.bitmagic.ifeed.infrastructure.text.search.pg.PgTextSearchStore;
import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
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
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@EnableAsync
public class IFeedApplication {

    @Configuration
    public static class IFeedMvcConfigurer implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {

            registry.addMapping("/api/**")
                    .allowedOrigins("https://www.ifeed.cc", "https://ifeed.cc", "http://localhost:5173", "http://192.168.8.57:5173")
                    .allowedMethods("PUT", "DELETE", "POST", "GET", "PATCH", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true).maxAge(3600);

            // Add more mappings...
        }

        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            // 将所有路由转发到 index.html
            registry.addViewController("/{spring:\\w+}")
                    .setViewName("forward:/index.html");
            registry.addViewController("/**/{spring:\\w+}")
                    .setViewName("forward:/index.html");
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
    public TextSearchRetrievalHandler textSearchRetrievalHandler(PgTextSearchStore pgTextSearchStore) {
        return new TextSearchRetrievalHandler(pgTextSearchStore);
    }

    @Bean
    public VectorRetrievalHandler vectorRetrievalHandler(VectorStoreTurbo vectorStoreTurbo, UserSubscriptionRepository userSubscriptionRepository) {
        return new VectorRetrievalHandler(vectorStoreTurbo, userSubscriptionRepository);
    }

    @Bean
    public OkHttpClient rssHttpClient(RssFetcherProperties properties) {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .followRedirects(true)
                .connectionPool(new ConnectionPool(
                        properties.getThreadPoolSize(), 30, TimeUnit.SECONDS))
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
