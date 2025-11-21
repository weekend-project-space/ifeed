package org.bitmagic.ifeed.application.feed.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.infrastructure.ai.AiContentService;
import org.bitmagic.ifeed.infrastructure.util.ContentCleaner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultEntryProcessor implements EntryProcessor {

    private final ArticleRepository articleRepository;
    private final AiContentService aiContentService;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<Article> process(Feed feed, SyndEntry entry) {
        var link = Optional.ofNullable(entry.getLink()).orElse(entry.getUri());
        if (!StringUtils.hasText(link)) {
            return Optional.empty();
        }

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
                .publishedAt(resolvePublishedAt(entry))
                .enclosure(resolveEnclosure(entry))
                .enclosureType(resolveEnclosureType(entry))
                .thumbnail(thumbnail)
                .content(cleaned.mdContent())
                .summary(aiContent.summary())
                .category(aiContent.category())
                .tags(writeJson(aiContent.tags()))
                .embeddingGenerated(false)
                .aiGenerated(false)
                .build();

        return Optional.of(article);
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
        if (entry.getUpdatedDate() != null) {
            return entry.getUpdatedDate().toInstant();
        }
        if (entry.getPublishedDate() != null) {
            return entry.getPublishedDate().toInstant();
        }
        return Instant.now();
    }

    private String resolveEnclosure(SyndEntry entry) {
        if (entry.getEnclosures() == null || entry.getEnclosures().isEmpty()) {
            return null;
        }
        return entry.getEnclosures().get(0).getUrl();
    }

    private String resolveEnclosureType(SyndEntry entry) {
        if (entry.getEnclosures() == null || entry.getEnclosures().isEmpty()) {
            return null;
        }
        return entry.getEnclosures().get(0).getType();
    }

    private String resolveThumbnail(SyndEntry entry, String rawContent) {
        var thumbnailFromContent = extractImageFromHtml(rawContent, resolveBaseUri(entry));
        if (StringUtils.hasText(thumbnailFromContent)) {
            return thumbnailFromContent;
        }

        return findThumbnailFromEnclosures(entry).orElse(null);
    }

    private Optional<String> findThumbnailFromEnclosures(SyndEntry entry) {
        if (entry.getEnclosures() == null || entry.getEnclosures().isEmpty()) {
            return Optional.empty();
        }

        return entry.getEnclosures().stream()
                .map(this::resolveThumbnailFromEnclosure)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private String resolveBaseUri(SyndEntry entry) {
        return StringUtils.hasText(entry.getLink()) ? entry.getLink() : null;
    }

    private String resolveThumbnailFromEnclosure(SyndEnclosure enclosure) {
        if (enclosure == null) {
            return null;
        }
        var url = enclosure.getUrl();
        if (!StringUtils.hasText(url)) {
            return null;
        }

        if (isImageType(enclosure.getType()) || isImageUrl(url)) {
            return url;
        }

        return null;
    }

    private boolean isImageType(String type) {
        return StringUtils.hasText(type) && type.toLowerCase().startsWith("image");
    }

    private boolean isImageUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        var lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")
                || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".webp")
                || lowerUrl.endsWith(".gif");
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
