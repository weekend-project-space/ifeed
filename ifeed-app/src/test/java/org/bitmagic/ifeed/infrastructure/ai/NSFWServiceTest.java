package org.bitmagic.ifeed.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NSFWServiceTest {

    private final NSFWService service = new NSFWService(null);

    @Test
    void recognizesConfiguredUrls() {
        List.of(
                "https://javdb.com/",
                "https://www.pornhub.com/",
                "https://www.t66y.com/",
                "https://91porn.com/",
                "https://www.141jav.com/",
                "https://www.141ppv.com/",
                "https://xsijishe.com/",
                "https://1fuli.info/",
                "https://fuliba2023.net/",
                "https://xxxclub.to/",
                "https://t.me/sexytoday",
                "https://t.me/xiaojiejiepindao",
                "https://rsshub.rssforever.com/javdb/"
        ).forEach(url -> assertTrue(service.isNSFW(List.of(url)), url));
    }

    @Test
    void recognizesWwwHostsAndNestedPaths() {
        assertTrue(service.isNSFW(List.of("https://www.javdb.com/post/1")));
        assertTrue(service.isNSFW(List.of("HTTPS://T.ME/SEXYTODAY/123")));
        assertTrue(service.isNSFW(List.of("https://rsshub.rssforever.com/javdb/actor/1")));
    }

    @Test
    void ignoresUnrelatedAndMalformedUrls() {
        assertFalse(service.isNSFW((List<String>) null));
        assertFalse(service.isNSFW(List.of()));
        assertFalse(service.isNSFW(List.of("https://example.com/", "not a url")));
        assertFalse(service.isNSFW(List.of("https://javdb.com.example.org/")));
        assertFalse(service.isNSFW(List.of("https://t.me/unrelated/sexytoday")));
        assertFalse(service.isNSFW(List.of("https://rsshub.rssforever.com/github/")));
    }
}
