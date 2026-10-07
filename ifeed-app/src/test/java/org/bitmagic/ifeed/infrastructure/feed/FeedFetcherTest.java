package org.bitmagic.ifeed.infrastructure.feed;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.util.ContentCleaner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class FeedFetcherTest {

    @Autowired
    private FeedFetcher feedFetcher;

    @Test
    void fetch() throws FeedException, IOException, InterruptedException {
        SyndFeed feed = feedFetcher.fetch("https://www.jiqizhixin.com/rss");
        feed.getEntries().forEach(syndEntry -> {
           log.info("{}", ContentCleaner.clean(syndEntry.getContents().stream().map(SyndContent::getValue).collect(Collectors.joining())).mdContent());
        });
        assertNotNull(feed);
    }
}