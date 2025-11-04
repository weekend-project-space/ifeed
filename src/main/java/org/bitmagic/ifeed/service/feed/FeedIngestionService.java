package org.bitmagic.ifeed.service.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.RssFetcherProperties;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.entity.Feed;
import org.bitmagic.ifeed.domain.entity.FeedFetchStatus;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.service.ai.AiContentService;
import org.bitmagic.ifeed.util.ContentCleaner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.bitmagic.ifeed.config.RssFetcherProperties.Cache.CACHE_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedIngestionService {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME
    );
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2048;


    private final FeedRepository feedRepository;
    private final ArticleRepository articleRepository;
    private final AiContentService aiContentService;
    private final ObjectMapper objectMapper;
    private final HttpClient rssHttpClient;
    private final RssFetcherProperties properties;

    @Transactional(readOnly = true)
    public List<UUID> getFeedIds(Predicate<Feed> predicate) {
        return feedRepository.findAll().stream()
                .filter(predicate)
                .map(Feed::getUid)
                .toList();
    }

    @Transactional
    public Optional<Boolean> ingestFeed(UUID feedUid) {
        return feedRepository.findByUid(feedUid).map(this::fetchFeedSafely);
    }

    private boolean fetchFeedSafely(Feed feed) {
        Instant latestContentUpdate = null;
        try {
            byte[] bodyBytes = fetchRawBytesWithRetry(feed.getUrl());
            SyndFeed syndFeed = parseFeed(bodyBytes, feed.getUrl());
            log.debug("Successfully fetched feed: {}", feed.getUrl());

            latestContentUpdate = processEntries(feed, syndFeed.getEntries());
            updateFeedInfo(feed, syndFeed);

            if (latestContentUpdate != null) {
                var current = feed.getLastUpdated();
                if (current == null || latestContentUpdate.isAfter(current)) {
                    feed.setLastUpdated(latestContentUpdate);
                }
            }

            feed.setLastFetched(Instant.now());
            feed.setLastFetchStatus(FeedFetchStatus.SUCCEEDED);
            feed.setFetchErrorAt(null);
            feed.setFetchError(null);
            feed.setFailureCount(0);
            feedRepository.save(feed);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to ingest feed: {}", feed.getUrl(), ex);
            feed.setLastFetchStatus(FeedFetchStatus.FAILED);
            feed.setFetchErrorAt(Instant.now());
            feed.setFetchError(truncate(resolveErrorMessage(ex), MAX_ERROR_MESSAGE_LENGTH));
            feed.setFailureCount(Optional.ofNullable(feed.getFailureCount()).orElse(0) + 1);
            feedRepository.save(feed);
            return false;
        }
    }

    /**
     * 带重试 + 缓存的原始字节抓取
     */
    @Cacheable(cacheNames = CACHE_NAME, key = "#feedUrl", unless = "#result == null")
    public byte[] fetchRawBytesWithRetry(String feedUrl) {
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
                        .header("User-Agent", "Mozilla/5.0 (compatible; RssBot/1.0)")
                        .GET()
                        .build();

                HttpResponse<InputStream> response = rssHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (status >= 400) {
                    throw new IOException("HTTP " + status);
                }

                byte[] bytes = readAllBytesSafe(response.body());
                log.debug("Fetched {} bytes from {}", bytes.length, feedUrl);
                return bytes;

            } catch (IOException e) {
                lastError = e;
                log.warn("Attempt {}/{} failed for {}: {}", attempt, properties.getMaxRetries(), feedUrl, e.toString());
                if (attempt < properties.getMaxRetries()) {
                    long delay = 1000L * (1L << (attempt - 1)); // 1s, 2s, 4s...
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during retry", e);
            }
        }
        throw new IllegalStateException("Exhausted retries for " + feedUrl, lastError);
    }

    /**
     * 安全读取流，避开 Content-Length 校验
     */
    private byte[] readAllBytesSafe(InputStream is) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            long total = 0;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
                total += len;
                if (total > 10 * 1024 * 1024) { // 10MB 上限
                    throw new IOException("Feed too large (>10MB): " + total);
                }
            }
            return baos.toByteArray();
        }
    }

    /**
     * 解析 RSS（主解析 + Jsoup 兜底）
     */
    private SyndFeed parseFeed(byte[] bytes, String feedUrl) {
        try {
            return buildFeed(new SyndFeedInput(), bytes);
        } catch (Exception e) {
            log.warn("Standard parsing failed for {}. Trying Jsoup fallback.", feedUrl, e);
            var fallback = buildFeedWithJsoupFallback(feedUrl, bytes);
            if (fallback != null) {
                log.info("Jsoup fallback succeeded for {}", feedUrl);
                return fallback;
            }
            throw new IllegalArgumentException("Failed to parse feed: " + feedUrl, e);
        }
    }

    private Instant processEntries(Feed feed, List<SyndEntry> entries) {
        if (entries == null || entries.isEmpty()) return null;

        var articles = entries.stream()
                .limit(Math.max(1, properties.getMaxItems()))
                .map(entry -> processEntry(feed, entry))
                .flatMap(Optional::stream)
                .toList();

        if (articles.isEmpty()) return null;

        var latest = articles.stream()
                .map(Article::getPublishedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        articleRepository.saveAllAndFlush(articles);
        return latest;
    }

    private Optional<Article> processEntry(Feed feed, SyndEntry entry) {
        var link = Optional.ofNullable(entry.getLink()).orElse(entry.getUri());
        if (!StringUtils.hasText(link)) return Optional.empty();

        var publishedAt = resolvePublishedAt(entry);
        if (articleRepository.existsByFeedAndLink(feed, link)) {
            return Optional.empty();
        }

        var rawContent = resolveContent(entry);
        var thumbnail = resolveThumbnail(entry, rawContent);
        var cleaned = ContentCleaner.clean(rawContent);
        var textContent = StringUtils.hasText(cleaned.textContent()) ? cleaned.textContent() : entry.getTitle();
        var aiContent = aiContentService.analyze(entry.getTitle(), textContent);

        var article = Article.builder()
                .feed(feed)
                .title(entry.getTitle())
                .link(link)
                .author(entry.getAuthor())
                .description(Optional.ofNullable(entry.getDescription()).map(SyndContent::getValue).orElse(null))
                .publishedAt(publishedAt)
                .enclosure(resolveEnclosure(entry))
                .thumbnail(thumbnail)
                .content(cleaned.mdContent())
                .summary(aiContent.summary())
                .category(aiContent.category())
                .tags(writeJson(aiContent.tags()))
                .embedding(null)
                .build();

        return Optional.of(article);
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }

    private void updateFeedInfo(Feed feed, SyndFeed syndFeed) {
        var fetchedTitle = syndFeed != null ? syndFeed.getTitle() : null;
        if (StringUtils.hasText(fetchedTitle)) {
            var normalizedTitle = fetchedTitle.trim();
            if (!normalizedTitle.equals(feed.getTitle())) {
                feed.setTitle(normalizedTitle);
                feed.setSiteUrl(syndFeed.getLink());
            }
            return;
        }

        if (!StringUtils.hasText(feed.getTitle())) {
            feed.setTitle(resolveFallbackTitle(feed));
        }
    }

    private String resolveFallbackTitle(Feed feed) {
        var siteTitle = extractHost(feed.getSiteUrl());
        if (StringUtils.hasText(siteTitle)) {
            return siteTitle;
        }

        var feedTitle = extractHost(feed.getUrl());
        if (StringUtils.hasText(feedTitle)) {
            return feedTitle;
        }

        return "未命名订阅";
    }

    private String extractHost(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        var trimmed = url.trim();
        try {
            var uri = new URI(trimmed);
            if (StringUtils.hasText(uri.getHost())) {
                return uri.getHost();
            }
            var path = uri.getPath();
            if (StringUtils.hasText(path)) {
                return path;
            }
        } catch (URISyntaxException ignored) {
            // fallback to trimmed url
        }

        return trimmed;
    }

    private String resolveContent(SyndEntry entry) {
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            return entry.getContents().stream()
                    .map(SyndContent::getValue)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse("");
        }
        return Optional.ofNullable(entry.getDescription())
                .map(SyndContent::getValue)
                .orElse("");
    }

    private Instant resolvePublishedAt(SyndEntry entry) {
        if (entry.getPublishedDate() != null) {
            return entry.getPublishedDate().toInstant();
        }
        if (entry.getUpdatedDate() != null) {
            return entry.getUpdatedDate().toInstant();
        }
        return Instant.now();
    }

    private String resolveEnclosure(SyndEntry entry) {
        if (entry.getEnclosures() == null || entry.getEnclosures().isEmpty()) {
            return null;
        }
        return entry.getEnclosures().get(0).getUrl();
    }

    private String resolveThumbnail(SyndEntry entry, String rawContent) {
        var baseUri = StringUtils.hasText(entry.getLink()) ? entry.getLink() : null;
        var thumbnailFromContent = extractImageFromHtml(rawContent, baseUri);
        if (StringUtils.hasText(thumbnailFromContent)) {
            return thumbnailFromContent;
        }

        if (entry.getEnclosures() == null || entry.getEnclosures().isEmpty()) {
            return null;
        }

        return entry.getEnclosures().stream()
                .map(this::resolveThumbnailFromEnclosure)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String resolveThumbnailFromEnclosure(SyndEnclosure enclosure) {
        if (enclosure == null) {
            return null;
        }
        var url = enclosure.getUrl();
        if (!StringUtils.hasText(url)) {
            return null;
        }

        var type = enclosure.getType();
        if (type != null && type.toLowerCase().startsWith("image")) {
            return url;
        }

        var lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")
                || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".webp")
                || lowerUrl.endsWith(".gif")) {
            return url;
        }

        return null;
    }

    private String extractImageFromHtml(String rawContent, String baseUri) {
        if (!StringUtils.hasText(rawContent)) {
            return null;
        }
        var document = Jsoup.parse(rawContent, baseUri == null ? "" : baseUri);
        Element image = document.selectFirst("img[src]");
        if (image == null) {
            return null;
        }
        var src = image.hasAttr("abs:src") ? image.attr("abs:src") : image.attr("src");
        return StringUtils.hasText(src) ? src.trim() : null;
    }

    private String writeJson(List<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(new TreeSet<>(values));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize AI payload", e);
            return null;
        }
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (!StringUtils.hasText(throwable.getMessage()) && throwable.getCause() != null) {
            return resolveErrorMessage(throwable.getCause());
        }
        var message = StringUtils.hasText(throwable.getMessage())
                ? throwable.getMessage()
                : throwable.getClass().getSimpleName();
        if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
            return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        return message;
    }

    private SyndFeed buildFeed(SyndFeedInput feedInput, byte[] bytes) throws IOException, FeedException {
        try (var reader = new XmlReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8.name(), true)) {
            return feedInput.build(reader);
        }
    }

    private SyndFeed buildFeedWithJsoupFallback(String feedUrl, byte[] bytes) {
        var raw = new String(bytes, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        Document document = Jsoup.parseBodyFragment(raw);

        Element channel = document.selectFirst("channel");
        if (channel != null) {
            return buildFeedFromRssChannel(channel);
        }

        Element atomFeed = document.selectFirst("feed");
        if (atomFeed != null) {
            return buildFeedFromAtom(atomFeed);
        }

        log.warn("Jsoup fallback could not identify feed structure for {}", feedUrl);
        return null;
    }

    private SyndFeed buildFeedFromRssChannel(Element channel) {
        var feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle(textOf(channel, "title"));
        feed.setLink(textOf(channel, "link"));
        feed.setDescription(textOf(channel, "description"));
        feed.setPublishedDate(parseDate(textOf(channel, "lastBuildDate")));

        var entries = channel.select("item").stream()
                .map(this::toRssEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        feed.setEntries(entries);
        return feed;
    }

    private SyndEntry toRssEntry(Element item) {
        var title = textOf(item, "title");
        var link = textOf(item, "link");
        if (!StringUtils.hasText(title) && !StringUtils.hasText(link)) {
            return null;
        }

        var entry = new SyndEntryImpl();
        entry.setTitle(title);
        entry.setLink(link);
        entry.setUri(Optional.ofNullable(textOf(item, "guid")).filter(StringUtils::hasText).orElse(link));
        entry.setAuthor(textOf(item, "author"));

        var publishedDate = firstNonEmpty(item, "pubDate", "dc\\:date", "updated");
        entry.setPublishedDate(parseDate(publishedDate));

        var description = htmlOf(item, "description");
        if (!StringUtils.hasText(description)) {
            description = htmlOf(item, "content\\:encoded");
        }
        if (StringUtils.hasText(description)) {
            var content = new SyndContentImpl();
            content.setType("text/html");
            content.setValue(description);
            entry.setDescription(content);
        }

        var enclosureUrl = attrOf(item, "enclosure", "url");
        if (StringUtils.hasText(enclosureUrl)) {
            var enclosure = new SyndEnclosureImpl();
            enclosure.setUrl(enclosureUrl);
            enclosure.setType(attrOf(item, "enclosure", "type"));
            var lengthAttr = attrOf(item, "enclosure", "length");
            if (StringUtils.hasText(lengthAttr)) {
                try {
                    enclosure.setLength(Long.parseLong(lengthAttr));
                } catch (NumberFormatException ignored) {
                    // ignore invalid length
                }
            }
            entry.setEnclosures(List.of(enclosure));
        }

        return entry;
    }

    private SyndFeed buildFeedFromAtom(Element feedElement) {
        var feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");
        feed.setTitle(textOf(feedElement, "title"));
        feed.setLink(resolveAtomLink(feedElement));
        feed.setDescription(textOf(feedElement, "subtitle"));
        feed.setPublishedDate(parseDate(firstNonEmpty(feedElement, "updated", "published")));

        var entries = feedElement.select("entry").stream()
                .map(this::toAtomEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        feed.setEntries(entries);
        return feed;
    }

    private SyndEntry toAtomEntry(Element entryElement) {
        var title = textOf(entryElement, "title");
        var link = resolveAtomLink(entryElement);
        if (!StringUtils.hasText(title) && !StringUtils.hasText(link)) {
            return null;
        }

        var entry = new SyndEntryImpl();
        entry.setTitle(title);
        entry.setLink(link);
        entry.setUri(Optional.ofNullable(textOf(entryElement, "id")).filter(StringUtils::hasText).orElse(link));
        entry.setAuthor(textOf(entryElement, "author > name"));

        var publishedDate = firstNonEmpty(entryElement, "updated", "published");
        entry.setPublishedDate(parseDate(publishedDate));

        Element contentEl = entryElement.selectFirst("content");
        if (contentEl == null) {
            contentEl = entryElement.selectFirst("summary");
        }
        if (contentEl != null && (StringUtils.hasText(contentEl.text()) || StringUtils.hasText(contentEl.html()))) {
            var content = new SyndContentImpl();
            content.setType(Optional.ofNullable(contentEl.attr("type")).filter(StringUtils::hasText).orElse("text/html"));
            content.setValue(contentEl.html());
            entry.setDescription(content);
        }

        return entry;
    }

    private String resolveAtomLink(Element parent) {
        Element link = parent.selectFirst("> link[rel=alternate][href]");
        if (link == null) {
            link = parent.selectFirst("> link[href]");
        }
        if (link == null) {
            link = parent.selectFirst("link[rel=alternate][href]");
        }
        if (link == null) {
            link = parent.selectFirst("link[href]");
        }
        if (link == null) {
            return null;
        }
        var href = link.attr("href");
        return StringUtils.hasText(href) ? href.trim() : null;
    }

    private String firstNonEmpty(Element parent, String... queries) {
        for (var query : queries) {
            var value = textOf(parent, query);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String textOf(Element parent, String cssQuery) {
        if (parent == null || !StringUtils.hasText(cssQuery)) {
            return null;
        }
        var element = parent.selectFirst(cssQuery);
        if (element == null) {
            return null;
        }
        var text = element.text();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String htmlOf(Element parent, String cssQuery) {
        if (parent == null || !StringUtils.hasText(cssQuery)) {
            return null;
        }
        var element = parent.selectFirst(cssQuery);
        if (element == null) {
            return null;
        }
        var html = element.html();
        return StringUtils.hasText(html) ? html.trim() : null;
    }

    private String attrOf(Element parent, String tagName, String attrName) {
        if (parent == null || !StringUtils.hasText(tagName) || !StringUtils.hasText(attrName)) {
            return null;
        }
        var element = parent.selectFirst(tagName + "[" + attrName + "]");
        if (element == null) {
            return null;
        }
        var value = element.attr(attrName);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Date parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        var trimmed = value.trim();

        for (var formatter : DATE_FORMATTERS) {
            try {
                return Date.from(ZonedDateTime.parse(trimmed, formatter).toInstant());
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }

        try {
            return Date.from(Instant.parse(trimmed));
        } catch (Exception ignored) {
            // fall through
        }

        try {
            var localDateTime = LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {
            // fall through
        }

        try {
            return new Date(Long.parseLong(trimmed));
        } catch (NumberFormatException ignored) {
            // fall through
        }

        return null;
    }


}
