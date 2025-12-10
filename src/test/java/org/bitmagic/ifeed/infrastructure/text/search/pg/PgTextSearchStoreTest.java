package org.bitmagic.ifeed.infrastructure.text.search.pg;

import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.text.search.ScoredDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class PgTextSearchStoreTest {

    @Autowired
    PgTextSearchStore pgTextSearchStore;

    @Test
    void searchWithFilter() {
        List<ScoredDocument> scoredDocuments = pgTextSearchStore.searchWithFilter("阮一峰的网络日志", 30, null, true, 0.1);
        scoredDocuments.forEach(doc -> {
            log.info("doc={}: score: {}", doc.document().metadata().get("title"), doc.score());
        });
    }
}