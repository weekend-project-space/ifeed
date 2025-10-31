package org.bitmagic.ifeed.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.rss.fetcher")
public class RssFetcherProperties {

    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(10);
    private int threadPoolSize = 10;
    private int maxItems = 500;
    private int maxRetries = 3;
    private Cache cache = new Cache();

    @Data
    public static class Cache {
        public static final String CACHE_NAME = "rss-raw-cache";
        private Duration expireAfterWrite = Duration.ofMinutes(30);
        private long maximumSize = 1000;
    }
}
