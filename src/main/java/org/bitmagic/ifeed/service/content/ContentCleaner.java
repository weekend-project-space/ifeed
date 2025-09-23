package org.bitmagic.ifeed.service.content;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ContentCleaner {

    public Content clean(String html) {
        if (!StringUtils.hasText(html)) {
            return new Content("", "");
        }

        Document document = Jsoup.parse(html);
        document.select("script, style, noscript").remove();
        var sanitized = Jsoup.clean(document.html(), Safelist.relaxed());
        var converter = FlexmarkHtmlConverter.builder().build();

        return new Content(converter.convert(sanitized),
                Jsoup.parse(sanitized).text().replaceAll("\\s+", " ").trim());
    }

    public record Content(String mdContent, String textContent) {
    }
}
