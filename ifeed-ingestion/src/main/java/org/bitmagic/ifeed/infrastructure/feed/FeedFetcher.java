package org.bitmagic.ifeed.infrastructure.feed;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;

import java.io.IOException;

/**
 * @author yangrd
 * @date 2025/11/10
 **/
public interface FeedFetcher {

    SyndFeed fetch(String feedUrl) throws IOException, InterruptedException, FeedException;
}
