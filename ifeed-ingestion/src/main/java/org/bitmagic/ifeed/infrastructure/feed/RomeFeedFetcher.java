package org.bitmagic.ifeed.infrastructure.feed;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.infrastructure.feed.fetch.HttpFetcher;
import org.bitmagic.ifeed.infrastructure.feed.parse.FeedParser;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author yangrd
 * @date 2025/11/10
 **/
@Service
@RequiredArgsConstructor
public class RomeFeedFetcher implements FeedFetcher {

    private final HttpFetcher httpFetcher;

    private final FeedParser feedParser;

    @Override
    public SyndFeed fetch(String feedUrl) throws IOException, InterruptedException, FeedException {
        return feedParser.parse(httpFetcher.fetch(feedUrl), feedUrl);
    }
}
