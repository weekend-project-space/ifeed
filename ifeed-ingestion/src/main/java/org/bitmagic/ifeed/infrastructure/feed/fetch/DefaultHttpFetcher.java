package org.bitmagic.ifeed.infrastructure.feed.fetch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.bitmagic.ifeed.config.properties.RssFetcherProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultHttpFetcher implements HttpFetcher {

    private static final int MAX_FEED_BYTES = 10 * 1024 * 1024; // 10MB
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (compatible; RssBot/1.0)";

    private final OkHttpClient rssHttpClient;
    private final RssFetcherProperties properties;

    @Override
    public byte[] fetch(String feedUrl) throws IOException, InterruptedException {
        int attempt = 0;
        IOException lastError = null;

        while (attempt < properties.getMaxRetries()) {
            attempt++;
            try {
                log.debug("Fetching RSS (attempt {}/{}): {}", attempt, properties.getMaxRetries(), feedUrl);
                Request request = new Request.Builder()
                        .url(feedUrl)
                        .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                        .header("User-Agent", DEFAULT_USER_AGENT)
                        .build();

                try (Response response = rssHttpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code());
                    }
                    ResponseBody body = response.body();
                    byte[] bytes = readBodySafe(body);
                    log.debug("Fetched {} bytes from {}", bytes.length, feedUrl);
                    return bytes;
                }

            } catch (IOException e) {
                if (isNonRetryable(e)) {
                    throw e;
                }
                lastError = e;
                log.warn("Attempt {}/{} failed for {}: {}", attempt, properties.getMaxRetries(), feedUrl, e.toString());
                if (attempt < properties.getMaxRetries()) {
                    backoff(attempt);
                }
            }
        }

        throw new IOException("Exhausted retries for " + feedUrl, lastError);
    }

    private boolean isNonRetryable(IOException e) {
        String msg = e.getMessage();
        return msg != null && msg.startsWith("HTTP 4");
    }

    private byte[] readBodySafe(ResponseBody body) throws IOException {
        if (body == null) return new byte[0];
        try (InputStream is = body.byteStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
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

    private void backoff(int attempt) throws InterruptedException {
        long delay = Math.min(1000L * (1L << (attempt - 1)), 8000);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}
