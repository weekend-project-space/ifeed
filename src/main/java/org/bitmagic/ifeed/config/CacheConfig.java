package org.bitmagic.ifeed.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yangrd
 * @date 2025/10/31
 **/
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RssFetcherProperties properties) {
        var spec = "maximumSize=%d,expireAfterWrite=%ds".formatted(
                properties.getCache().getMaximumSize(),
                properties.getCache().getExpireAfterWrite().getSeconds()
        );

        CaffeineCacheManager manager = new CaffeineCacheManager(RssFetcherProperties.Cache.CACHE_NAME, "rss-feed-cache");
        manager.setCaffeine(Caffeine.from(spec));
        manager.setAllowNullValues(true);
        return manager;
    }
}
