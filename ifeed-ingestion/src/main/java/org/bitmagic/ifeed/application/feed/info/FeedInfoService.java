package org.bitmagic.ifeed.application.feed.info;

import com.rometools.rome.feed.synd.SyndFeed;
import org.bitmagic.ifeed.domain.model.Feed;

public interface FeedInfoService {

    void update(Feed feed, SyndFeed syndFeed);
}
