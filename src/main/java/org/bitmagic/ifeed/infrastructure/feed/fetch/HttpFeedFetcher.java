package org.bitmagic.ifeed.infrastructure.feed.fetch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.RssFetcherProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.bitmagic.ifeed.config.properties.RssFetcherProperties.Cache.CACHE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpFeedFetcher implements FeedFetcher {

    private static final int MAX_FEED_BYTES = 10 * 1024 * 1024; // 10MB

    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (compatible; RssBot/1.0)";

    private final HttpClient rssHttpClient;
    private final RssFetcherProperties properties;

    @Override
    @Cacheable(cacheNames = CACHE_NAME, key = "#feedUrl", unless = "#result == null")
    public byte[] fetch(String feedUrl) throws IOException, InterruptedException {
        int attempt = 0;
        IOException lastError = null;

        while (attempt < properties.getMaxRetries()) {
            attempt++;
            try {
                log.debug("Fetching RSS (attempt {}/{}): {}", attempt, properties.getMaxRetries(), feedUrl);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(feedUrl))
                        .timeout(properties.getReadTimeout())
                        .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                        .header("User-Agent", DEFAULT_USER_AGENT)
                        .GET()
                        .build();

                HttpResponse<InputStream> response = rssHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() >= 400) {
                    throw new IOException("HTTP " + response.statusCode());
                }

                byte[] bytes = readAllBytesSafe(response.body());
                log.debug("Fetched {} bytes from {}", bytes.length, feedUrl);
                return bytes;

            } catch (IOException e) {
                lastError = e;
                log.warn("Attempt {}/{} failed for {}: {}", attempt, properties.getMaxRetries(), feedUrl, e.toString());
                if (attempt < properties.getMaxRetries()) {
                    backoff(attempt);
                }
            }
        }

        throw new IOException("Exhausted retries for " + feedUrl, lastError);
    }

    private void backoff(int attempt) throws InterruptedException {
        long delay = 1000L * (1L << (attempt - 1));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private byte[] readAllBytesSafe(InputStream inputStream) throws IOException {
        try (InputStream is = inputStream; ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            long total = 0;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
                total += len;
                if (total > MAX_FEED_BYTES) {
                    throw new IOException("Feed too large (>10MB): " + total);
                }
            }
            return baos.toByteArray();
        }
    }
}
