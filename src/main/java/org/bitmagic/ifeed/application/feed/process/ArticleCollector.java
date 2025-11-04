package org.bitmagic.ifeed.application.feed.process;

import com.rometools.rome.feed.synd.SyndEntry;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.config.properties.RssFetcherProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.Feed;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ArticleCollector {

    private final EntryProcessor entryProcessor;
    private final RssFetcherProperties properties;

    public List<Article> collect(Feed feed, List<SyndEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        var limit = Math.max(1, properties.getMaxItems());
        return entries.stream()
                .limit(limit)
                .map(entry -> entryProcessor.process(feed, entry))
                .flatMap(Optional::stream)
                .toList();
    }
}
