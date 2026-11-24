package org.bitmagic.ifeed.application.search;

import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.retrieval.RetrievalContext;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class SearchRetrievalServiceTest {

    @Autowired
    SearchRetrievalService searchRetrievalService;

    @Test
    void execute() {
        searchRetrievalService.execute(RetrievalContext.builder().query("苹果").includeGlobal(true).dateRange(RetrievalContext.DateRange.builder().from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()).to(Instant.now()).build()).topK(100).build()).forEach(item -> {
            log.info("{} {} {} {}", item.docId(), item.score(), item.source(), JSON.toJson(item.meta()));
        });
    }
}