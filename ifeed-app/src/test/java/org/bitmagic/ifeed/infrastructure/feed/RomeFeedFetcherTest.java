package org.bitmagic.ifeed.infrastructure.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@Slf4j
@SpringBootTest
class RomeFeedFetcherTest {

    @Autowired
    FeedFetcher feedFetcher;

    @Test
    void fetch() throws FeedException, IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        SyndFeed syndFeed = feedFetcher.fetch("https://rss.jrj.com.cn/stock/727.xml");
        log.info("{}", mapper.writeValueAsString(syndFeed));
    }

}