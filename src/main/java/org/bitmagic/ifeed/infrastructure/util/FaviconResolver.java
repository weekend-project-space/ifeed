package org.bitmagic.ifeed.infrastructure.util;

import java.net.URI;

/**
 * 根据 Feed 的 URL 或 Site URL 提取 favicon URL
 * @author yangrd
 * @date 2025/12/12
 **/
public class FaviconResolver {

    private static final String DEFAULT_FAVICON = "https://favicon.im/";

    /**
     * 传入 feedUrl 或 siteUrl 任意一个均可
     */
    public static String resolve(String siteUrl, String feedUrl) {
        String host = extractHost(siteUrl);
        if (host == null) {
            host = extractHost(feedUrl);
        }

        if (host == null || host.isBlank()) {
            return DEFAULT_FAVICON;
        }

        return "https://favicon.im/%s".formatted(host);
    }

    /**
     * 从 URL 提取 host
     */
    private static String extractHost(String url) {
        if (url == null || url.isBlank())
            return null;

        try {
             URI uri = new URI(url.trim());
            if (uri.getHost() != null) {
                return uri.getHost();
            }
            // 对部分裸 URL 兜底，如 "example.com/feed.xml"
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                return path.substring(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}