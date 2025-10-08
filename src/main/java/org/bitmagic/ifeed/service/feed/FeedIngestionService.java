package org.bitmagic.ifeed.service.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rometools.utils.Strings;
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
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedIngestionService {

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

        try (InputStream is = response.body(); XmlReader reader = new XmlReader(is, StandardCharsets.UTF_8.name(), true)) {
            return new SyndFeedInput().build(reader);
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
}
