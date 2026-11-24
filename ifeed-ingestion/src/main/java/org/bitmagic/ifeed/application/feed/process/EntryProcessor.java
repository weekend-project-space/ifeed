package org.bitmagic.ifeed.application.feed.process;

import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.Feed;
import com.rometools.rome.feed.synd.SyndEntry;

import java.util.Optional;

public interface EntryProcessor {

    Optional<Article> process(Feed feed, SyndEntry entry);
}
