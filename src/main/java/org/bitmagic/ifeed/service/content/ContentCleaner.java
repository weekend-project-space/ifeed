package org.bitmagic.ifeed.service.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ContentCleaner {

    public String clean(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }

        Document document = Jsoup.parse(html);
        document.select("script, style, noscript").remove();
        var sanitized = Jsoup.clean(document.html(), Safelist.relaxed());
        return Jsoup.parse(sanitized).text().replaceAll("\\s+", " ").trim();
    }
}
