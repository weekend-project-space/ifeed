package org.bitmagic.ifeed.service.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.ParsingFeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.RssFetcherProperties;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.entity.Feed;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.service.ai.AiContentService;
import org.bitmagic.ifeed.service.content.ContentCleaner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedIngestionService {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME
    );

    private final FeedRepository feedRepository;
    private final ArticleRepository articleRepository;
    private final ContentCleaner contentCleaner;
    private final AiContentService aiContentService;
    private final ObjectMapper objectMapper;
    private final HttpClient rssHttpClient;
    private final RssFetcherProperties properties;

    @Transactional(readOnly = true)
    public List<UUID> getFeedIds() {
        return feedRepository.findAll().stream()
                .map(Feed::getId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void ingestFeed(UUID feedId) {
        feedRepository.findById(feedId).ifPresent(this::fetchFeedSafely);
    }

    private void fetchFeedSafely(Feed feed) {
        try {
            var syndFeed = fetchFeed(feed.getUrl());
            log.info("fetch url:{}", feed.getUrl());
            var latestContentUpdate = processEntries(feed, syndFeed.getEntries());
            updateFeedTitle(feed, syndFeed);
            if (latestContentUpdate != null) {
                var currentLastUpdated = feed.getLastUpdated();
                if (currentLastUpdated == null || latestContentUpdate.isAfter(currentLastUpdated)) {
                    feed.setLastUpdated(latestContentUpdate);
                }
            }
            feed.setLastFetched(Instant.now());
            feedRepository.save(feed);
        } catch (Exception ex) {
            log.warn("Failed to fetch feed {}", feed.getUrl(), ex);
        }
    }

    private SyndFeed fetchFeed(String feedUrl) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(feedUrl))
                .timeout(properties.getReadTimeout())
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .GET()
                .build();

        var response = rssHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Failed to fetch feed. Status code: " + response.statusCode());
        }

        byte[] bodyBytes;
        try (InputStream is = response.body()) {
            bodyBytes = is.readAllBytes();
        }

        var feedInput = new SyndFeedInput();

        try {
            return buildFeed(feedInput, bodyBytes);
        } catch (ParsingFeedException parsingFeedException) {
            log.warn("Standard feed parsing failed for {}. Trying Jsoup fallback.", feedUrl, parsingFeedException);
            var fallback = buildFeedWithJsoupFallback(feedUrl, bodyBytes);
            if (fallback != null) {
                log.info("Jsoup fallback parsing succeeded for {}", feedUrl);
                return fallback;
            }
            throw new IllegalArgumentException(new String(bodyBytes));
        }
    }

    private Instant processEntries(Feed feed, List<SyndEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        var latestPublished = entries.stream()
                .sorted(Comparator.comparing(this::resolvePublishedAt).reversed())
                .limit(Math.max(1, properties.getMaxItems()))
                .map(entry -> processEntry(feed, entry))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return latestPublished;
    }

    private Optional<Instant> processEntry(Feed feed, SyndEntry entry) {
        var link = Optional.ofNullable(entry.getLink()).orElse(entry.getUri());
        if (!StringUtils.hasText(link)) {
            return Optional.empty();
        }

        var publishedAt = resolvePublishedAt(entry);

        if (articleRepository.existsByLink(link)) {
            return Optional.ofNullable(publishedAt);
        }

        var rawContent = resolveContent(entry);
        var thumbnail = resolveThumbnail(entry, rawContent);
        var cleanedContent = contentCleaner.clean(rawContent);
        var textContent = cleanedContent.textContent();
        if (!StringUtils.hasText(cleanedContent.textContent())) {
            textContent = entry.getTitle();
        }

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
                .content(cleanedContent.mdContent())
                .summary(aiContent.summary())
                .category(aiContent.category())
                .tags(writeJson(aiContent.tags()))
                .embedding(writeJson(aiContent.embedding()))
                .build();

        articleRepository.save(article);

        return Optional.ofNullable(publishedAt);
    }

    private void updateFeedTitle(Feed feed, SyndFeed syndFeed) {
        var fetchedTitle = syndFeed != null ? syndFeed.getTitle() : null;
        if (StringUtils.hasText(fetchedTitle)) {
            var normalizedTitle = fetchedTitle.trim();
            if (!normalizedTitle.equals(feed.getTitle())) {
                feed.setTitle(normalizedTitle);
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

    private SyndFeed buildFeed(SyndFeedInput feedInput, byte[] bytes) throws Exception {
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
