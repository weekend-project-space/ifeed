package org.bitmagic.ifeed.infrastructure.feed.fetch;

import java.io.IOException;

public interface FeedFetcher {

    byte[] fetch(String feedUrl) throws IOException, InterruptedException;
}
