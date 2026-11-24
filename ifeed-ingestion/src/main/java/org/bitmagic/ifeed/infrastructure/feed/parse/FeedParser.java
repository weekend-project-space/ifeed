package org.bitmagic.ifeed.infrastructure.feed.parse;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;

import java.io.IOException;

public interface FeedParser {

    SyndFeed parse(byte[] bytes, String feedUrl) throws FeedException, IOException;
}
