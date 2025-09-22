package org.bitmagic.ifeed.service.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
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

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
            processEntries(feed, syndFeed.getEntries());
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

    private void processEntries(Feed feed, List<SyndEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        entries.stream()
                .sorted(Comparator.comparing(this::resolvePublishedAt).reversed())
                .limit(Math.max(1, properties.getMaxItems()))
                .forEach(entry -> processEntry(feed, entry));
    }

    private void processEntry(Feed feed, SyndEntry entry) {
        var link = Optional.ofNullable(entry.getLink()).orElse(entry.getUri());
        if (!StringUtils.hasText(link)) {
            return;
        }

        if (articleRepository.existsByLink(link)) {
            return;
        }

        var rawContent = resolveContent(entry);
        var cleanedContent = contentCleaner.clean(rawContent);
        if (!StringUtils.hasText(cleanedContent)) {
            cleanedContent = entry.getTitle();
        }

        var aiContent = aiContentService.analyze(entry.getTitle(), cleanedContent);

        var article = Article.builder()
                .feed(feed)
                .title(entry.getTitle())
                .link(link)
                .author(entry.getAuthor())
                .description(Optional.ofNullable(entry.getDescription()).map(SyndContent::getValue).orElse(null))
                .publishedAt(resolvePublishedAt(entry))
                .enclosure(resolveEnclosure(entry))
                .content(cleanedContent)
                .summary(aiContent.summary())
                .category(aiContent.category())
                .tags(writeJson(aiContent.tags()))
                .embedding(writeJson(aiContent.embedding()))
                .build();

        articleRepository.save(article);
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

    private String writeJson(List<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize AI payload", e);
            return null;
        }
    }
}
