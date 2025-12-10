package org.bitmagic.ifeed.infrastructure.feed.parse;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.util.XmlEncodingDetector;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RomeFeedParser implements FeedParser {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ofPattern("E, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    );

    private static final Pattern DATE_FIX_PATTERN = Pattern.compile(
            "<(pubDate|updated|dc:date)>([^<]*?(?:\\d{1,2} [A-Za-z]{3} \\d{4})(?!.*\\d{2}:\\d{2}:\\d{2}).*?)</\\1>",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public SyndFeed parse(byte[] bytes, String feedUrl) throws IOException, FeedException {
        try (InputStream input = preprocessFeedXml(bytes)) {
            return buildFeed(input);
        } catch (Exception e) {
            log.warn("Standard parsing failed for {}: {}. Trying Jsoup fallback.",
                    feedUrl, e.getMessage());
            var fallback = buildFeedWithJsoupFallback(feedUrl, bytes);
            if (fallback != null) {
                log.debug("Jsoup fallback succeeded for {}", feedUrl);
                return fallback;
            }
            throw new FeedException("Failed to parse feed: " + feedUrl, e);
        }
    }

    // ----------- XML 预处理（修复不规范日期） ----------------

    /**
     * 修复 Rome 无法解析的 pubDate，例如 "Wed, 5 Aug 2020"
     */
    private InputStream preprocessFeedXml(byte[] bytes) {
        String xml = XmlEncodingDetector.toString(bytes);
        Matcher matcher = DATE_FIX_PATTERN.matcher(xml);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group(1);
            String rawDate = matcher.group(2).trim();

            // 修复单日位数
            rawDate = rawDate.replaceAll(", ([0-9]) ", ", 0$1 ");

            // 若缺时间则补 "00:00:00 GMT"
            if (!rawDate.matches(".*\\d{2}:\\d{2}:\\d{2}.*")) {
                rawDate += " 00:00:00 GMT";
            }

            String replacement = String.format("<%s>%s</%s>", tag, rawDate, tag);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ----------- Rome 原生解析 ----------------

    private SyndFeed buildFeed(InputStream inputStream) throws IOException, FeedException {
        try (XmlReader reader = new XmlReader(inputStream)) {
            log.debug("Detected feed encoding: {}", reader.getEncoding());
            return new SyndFeedInput().build(reader);
        }
    }

    // ----------- Jsoup fallback 解析 ----------------

    private SyndFeed buildFeedWithJsoupFallback(String feedUrl, byte[] bytes) {
        String raw = XmlEncodingDetector.toString(bytes);
        if (!StringUtils.hasText(raw)) return null;

        Document document = Jsoup.parse(raw);
        Element channel = document.selectFirst("channel");
        if (channel != null) return buildFeedFromRssChannel(channel);

        Element atomFeed = document.selectFirst("feed");
        if (atomFeed != null) return buildFeedFromAtom(atomFeed);

        log.warn("Jsoup fallback could not identify feed structure for {}", feedUrl);
        return null;
    }

    // ----------- RSS 解析 ----------------

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
        String title = textOf(item, "title");
        String link = textOf(item, "link");
        if (!StringUtils.hasText(title) && !StringUtils.hasText(link)) return null;

        var entry = new SyndEntryImpl();
        entry.setTitle(title);
        entry.setLink(link);
        entry.setUri(Optional.ofNullable(textOf(item, "guid")).orElse(link));
        entry.setAuthor(textOf(item, "author"));
        entry.setPublishedDate(parseDate(firstNonEmpty(item, "pubDate", "dc\\:date", "updated")));

        // 内容
        String desc = htmlOf(item, "description");
        if (!StringUtils.hasText(desc)) desc = htmlOf(item, "content\\:encoded");
        if (StringUtils.hasText(desc)) {
            SyndContentImpl content = new SyndContentImpl();
            content.setType("text/html");
            content.setValue(desc);
            entry.setDescription(content);
        }

        // 附件
        String enclosureUrl = attrOf(item, "enclosure", "url");
        if (StringUtils.hasText(enclosureUrl)) {
            SyndEnclosureImpl enclosure = new SyndEnclosureImpl();
            enclosure.setUrl(enclosureUrl);
            enclosure.setType(attrOf(item, "enclosure", "type"));
            try {
                enclosure.setLength(Long.parseLong(attrOf(item, "enclosure", "length")));
            } catch (Exception ignored) {
            }
            entry.setEnclosures(List.of(enclosure));
        }

        return entry;
    }

    // ----------- Atom 解析 ----------------

    private SyndFeed buildFeedFromAtom(Element feedEl) {
        var feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");
        feed.setTitle(textOf(feedEl, "title"));
        feed.setLink(resolveAtomLink(feedEl));
        feed.setDescription(textOf(feedEl, "subtitle"));
        feed.setPublishedDate(parseDate(firstNonEmpty(feedEl, "updated", "published")));

        var entries = feedEl.select("entry").stream()
                .map(this::toAtomEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        feed.setEntries(entries);
        return feed;
    }

    private SyndEntry toAtomEntry(Element e) {
        String title = textOf(e, "title");
        String link = resolveAtomLink(e);
        if (!StringUtils.hasText(title) && !StringUtils.hasText(link)) return null;

        var entry = new SyndEntryImpl();
        entry.setTitle(title);
        entry.setLink(link);
        entry.setUri(Optional.ofNullable(textOf(e, "id")).orElse(link));
        entry.setAuthor(textOf(e, "author > name"));
        entry.setPublishedDate(parseDate(firstNonEmpty(e, "updated", "published")));

        Element contentEl = e.selectFirst("content, summary");
        if (contentEl != null && StringUtils.hasText(contentEl.html())) {
            SyndContentImpl content = new SyndContentImpl();
            content.setType(Optional.ofNullable(contentEl.attr("type")).orElse("text/html"));
            content.setValue(contentEl.html());
            entry.setDescription(content);
        }

        return entry;
    }

    // ----------- 公共辅助函数 ----------------

    private String resolveAtomLink(Element parent) {
        Element link = parent.selectFirst("> link[rel=alternate][href], link[href]");
        return (link != null && StringUtils.hasText(link.attr("href")))
                ? link.attr("href").trim() : null;
    }

    private String firstNonEmpty(Element parent, String... queries) {
        for (String q : queries) {
            String val = textOf(parent, q);
            if (StringUtils.hasText(val)) return val;
        }
        return null;
    }

    private String textOf(Element parent, String cssQuery) {
        if (parent == null) return null;
        Element el = parent.selectFirst(cssQuery);
        return (el != null && StringUtils.hasText(el.text())) ? el.text().replaceAll("^<!\\[CDATA\\[(.*)\\]\\]>$", "$1")
                .replaceAll("<!\\[CDATA\\[|\\]\\]>$", "")
                .trim() : null;
    }

    private String htmlOf(Element parent, String cssQuery) {
        if (parent == null) return null;
        Element el = parent.selectFirst(cssQuery);
        return (el != null && StringUtils.hasText(el.html())) ? el.html().replaceAll("^<!\\[CDATA\\[(.*)\\]\\]>$", "$1")
                .replaceAll("<!\\[CDATA\\[|\\]\\]>$", "")
                .trim() : null;
    }

    private String attrOf(Element parent, String tagName, String attrName) {
        Element el = parent.selectFirst(tagName + "[" + attrName + "]");
        if (el == null) return null;
        String val = el.attr(attrName);
        return StringUtils.hasText(val) ? val.trim() : null;
    }

    private String contentOf(Element parent, String... queries) {
        for (String q : queries) {
            Element el = parent.selectFirst(q);
            if (el == null) continue;
            String text = el.text();
            if (StringUtils.hasText(text)) return text.trim();
            String html = el.html();
            if (StringUtils.hasText(html)) return html.trim();
        }
        return null;
    }

    private Date parseDate(String value) {
        if (!StringUtils.hasText(value)) return null;
        String trimmed = value.trim();

        // 1️⃣ 尝试各种标准格式（RFC1123、ISO 等）
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return Date.from(ZonedDateTime.parse(trimmed, fmt).toInstant());
            } catch (DateTimeParseException ignored) {
            }
        }

        // 2️⃣ 常见的无时区格式，如 "2023-06-21 08:00:46"
        try {
            LocalDateTime dt = LocalDateTime.parse(trimmed,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
        }

        // 3️⃣ ISO 格式（例如 "2023-06-21T08:00:46"）
        try {
            LocalDateTime dt = LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
        }

        // 4️⃣ 纯日期（例如 "2023-06-21"）
        try {
            LocalDate d = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
            return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
        }

        // 5️⃣ 时间戳
        try {
            return new Date(Long.parseLong(trimmed));
        } catch (NumberFormatException ignored) {
        }

        // 6️⃣ 无法识别，返回 null
        log.debug("Unrecognized date format: {}", value);
        return null;
    }

}
