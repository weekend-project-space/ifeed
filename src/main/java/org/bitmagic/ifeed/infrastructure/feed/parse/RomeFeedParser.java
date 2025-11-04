package org.bitmagic.ifeed.infrastructure.feed.parse;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RomeFeedParser implements FeedParser {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME
    );

    @Override
    public SyndFeed parse(byte[] bytes, String feedUrl) throws IOException, FeedException {
        try {
            return buildFeed(bytes);
        } catch (Exception e) {
            log.warn("Standard parsing failed for {}. Trying Jsoup fallback.", feedUrl, e);
            var fallback = buildFeedWithJsoupFallback(feedUrl, bytes);
            if (fallback != null) {
                log.info("Jsoup fallback succeeded for {}", feedUrl);
                return fallback;
            }
            throw new FeedException("Failed to parse feed: " + feedUrl, e);
        }
    }

    private SyndFeed buildFeed(byte[] bytes) throws IOException, FeedException {
        try (var reader = new XmlReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8.name(), true)) {
            return new SyndFeedInput().build(reader);
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
