package org.bitmagic.ifeed.infrastructure.text.search.pg;

import org.bitmagic.ifeed.infrastructure.text.search.tracking.TopicArticleSearchRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TopicArticleSearchRequestTest {

    @Test
    void normalizesRulesAndTakesAnImmutableSnapshot() {
        var feeds = new HashSet<>(Set.of(1));
        var keywords = new ArrayList<>(Arrays.asList(null, " ", " AI ", "AI", "医疗"));
        Instant now = Instant.now();

        var request = new TopicArticleSearchRequest(feeds, keywords, null, now, now, null, 200);
        feeds.clear();
        keywords.clear();

        assertEquals(Set.of(1), request.feedIds());
        assertEquals(List.of("AI", "医疗"), request.includeKeywords());
        assertTrue(request.blockedKeywords().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> request.feedIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> request.includeKeywords().clear());
    }

    @Test
    void rejectsInvalidWindowCursorAndPageSize() {
        Instant now = Instant.now();

        assertThrows(NullPointerException.class, () -> request(null, now, null, 200));
        assertThrows(IllegalArgumentException.class, () -> request(now, now.minusSeconds(1), null, 200));
        assertThrows(IllegalArgumentException.class, () -> request(now, now, -1L, 200));
        assertThrows(IllegalArgumentException.class, () -> request(now, now, null, 0));
    }

    private TopicArticleSearchRequest request(Instant from, Instant to, Long cursor, int limit) {
        return new TopicArticleSearchRequest(Set.of(1), List.of("AI"), List.of(), from, to, cursor, limit);
    }
}
