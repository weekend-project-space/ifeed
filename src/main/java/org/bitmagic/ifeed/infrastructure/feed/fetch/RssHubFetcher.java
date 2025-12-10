package org.bitmagic.ifeed.infrastructure.feed.fetch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.RssFetcherProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.bitmagic.ifeed.config.properties.RssFetcherProperties.Cache.CACHE_NAME;

@Slf4j
@Component("httpFetcher")
@RequiredArgsConstructor
public class RssHubFetcher implements HttpFetcher {

    private static final String RSS_HUB_CONFIG_PATH = "/api/follow/config";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (compatible; RssBot/1.0)";

    // 控制 RSSHub 故障转移的最大尝试次数
    private static final int MAX_RSS_HUB_FALLBACK_ATTEMPTS = 3;

    // 缓存域名的 RSSHub 检测结果，避免重复检测
    private final ConcurrentHashMap<String, Boolean> rssHubCache = new ConcurrentHashMap<>();

    private final DefaultHttpFetcher defaultHttpFetcher;
    private final HttpClient rssHttpClient;
    private final RssFetcherProperties properties;
    private final Random random = new Random();

    @Override
    @Cacheable(cacheNames = CACHE_NAME, key = "#feedUrl", unless = "#result == null")
    public byte[] fetch(String feedUrl) throws IOException, InterruptedException {
        Set<String> triedUrls = new HashSet<>();
        return fetchWithFallback(feedUrl, triedUrls, 0);
    }

    private byte[] fetchWithFallback(String feedUrl, Set<String> triedUrls, int fallbackAttempt)
            throws IOException, InterruptedException {

        // 限制 RSSHub 故障转移次数，避免过多重试
        if (fallbackAttempt >= MAX_RSS_HUB_FALLBACK_ATTEMPTS) {
            log.warn("Reached maximum RSSHub fallback attempts ({}), giving up", MAX_RSS_HUB_FALLBACK_ATTEMPTS);
            throw new IOException("Exhausted RSSHub fallback attempts for " + feedUrl);
        }

        triedUrls.add(feedUrl);

        try {
            // 使用原始的 DefaultHttpFetcher 进行获取
            log.debug("Fetching with RSSHub fallback attempt {}/{}", fallbackAttempt + 1, MAX_RSS_HUB_FALLBACK_ATTEMPTS);
            return defaultHttpFetcher.fetch(feedUrl);

        } catch (IOException e) {
            log.warn("Failed to fetch from {} (fallback attempt {}/{}): {}",
                    feedUrl, fallbackAttempt + 1, MAX_RSS_HUB_FALLBACK_ATTEMPTS, e.getMessage());

            // 检测是否为 RSSHub 实例
            if (isRssHub(feedUrl)) {
                log.info("Detected RSSHub failure, attempting fallback to alternate instance");
                String alternativeUrl = getAlternativeRssHubUrl(feedUrl, triedUrls);

                if (alternativeUrl != null) {
                    log.info("Retrying with alternative RSSHub: {} (attempt {}/{})",
                            alternativeUrl, fallbackAttempt + 2, MAX_RSS_HUB_FALLBACK_ATTEMPTS);
                    return fetchWithFallback(alternativeUrl, triedUrls, fallbackAttempt + 1);
                } else {
                    log.warn("No alternative RSSHub instances available");
                }
            }

            // 如果不是 RSSHub 或没有可用的备选实例，抛出原始异常
            throw e;
        }
    }

    private boolean isRssHub(String feedUrl) {
        try {
            URI uri = URI.create(feedUrl);
            String baseUrl = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1) {
                baseUrl += ":" + uri.getPort();
            }

            // 检查缓存
            Boolean cached = rssHubCache.get(baseUrl);
            if (cached != null) {
                log.debug("RSSHub check for {} found in cache: {}", baseUrl, cached);
                return cached;
            }

            String configUrl = baseUrl + RSS_HUB_CONFIG_PATH;
            log.debug("Checking if {} is RSSHub via {}", feedUrl, configUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(configUrl))
                    .timeout(properties.getReadTimeout())
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = rssHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean isRssHub = response.statusCode() == 200;

            // 缓存结果
            rssHubCache.put(baseUrl, isRssHub);
            log.debug("{} is {}RSSHub (cached)", baseUrl, isRssHub ? "" : "NOT ");
            return isRssHub;

        } catch (Exception e) {
            log.debug("Failed to check RSSHub config for {}: {}", feedUrl, e.toString());
            // 检测失败时不缓存，下次可以重试
            return false;
        }
    }

    private String getAlternativeRssHubUrl(String originalUrl, Set<String> triedUrls) {
        List<String> rsshubList = properties.getRsshubList();
        if (rsshubList == null || rsshubList.isEmpty()) {
            log.debug("No RSSHub list configured");
            return null;
        }

        try {
            URI originalUri = URI.create(originalUrl);
            String path = originalUri.getRawPath();
            if (originalUri.getRawQuery() != null) {
                path += "?" + originalUri.getRawQuery();
            }

            // 构建所有可用的备选 URL
            String finalPath = path;
            List<String> availableInstances = rsshubList.stream()
                    .map(instance -> buildAlternativeUrl(instance, finalPath))
                    .filter(url -> url != null && !triedUrls.contains(url))
                    .toList();

            if (availableInstances.isEmpty()) {
                log.debug("All RSSHub instances have been tried");
                return null;
            }

            // 随机选择一个
            String selectedUrl = availableInstances.get(random.nextInt(availableInstances.size()));
            log.debug("Selected alternative RSSHub: {}", selectedUrl);
            return selectedUrl;

        } catch (Exception e) {
            log.error("Failed to generate alternative RSSHub URL: {}", e.toString());
            return null;
        }
    }

    private String buildAlternativeUrl(String instance, String path) {
        try {
            // 处理配置中可能只有域名的情况
            if (!instance.startsWith("http://") && !instance.startsWith("https://")) {
                instance = "https://" + instance;
            }

            URI instanceUri = URI.create(instance);
            String scheme = instanceUri.getScheme();
            String host = instanceUri.getHost();
            int port = instanceUri.getPort();

            StringBuilder url = new StringBuilder(scheme).append("://").append(host);
            if (port != -1) {
                url.append(":").append(port);
            }
            url.append(path);

            return url.toString();
        } catch (Exception e) {
            log.warn("Failed to build alternative URL for instance {}: {}", instance, e.getMessage());
            return null;
        }
    }
}