package org.bitmagic.ifeed.application.feed.info;

import com.rometools.rome.feed.synd.SyndFeed;
import org.bitmagic.ifeed.domain.model.Feed;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class DefaultFeedInfoService implements FeedInfoService {

    @Override
    public void update(Feed feed, SyndFeed syndFeed) {
        var fetchedTitle = syndFeed != null ? syndFeed.getTitle() : null;
        if (StringUtils.hasText(fetchedTitle)) {
            var normalizedTitle = fetchedTitle.trim();
            if (!normalizedTitle.equals(feed.getTitle())) {
                feed.setTitle(normalizedTitle);
                feed.setDescription(syndFeed.getDescription());
                feed.setSiteUrl(syndFeed.getLink());
            }
            if (syndFeed.getDescription() != null && !syndFeed.getDescription().equals(feed.getDescription())) {
                feed.setDescription(syndFeed.getDescription());
            }
            return;
        }

        if (!StringUtils.hasText(feed.getTitle())) {
            feed.setTitle(resolveFallbackTitle(feed));
        }
    }

    private String resolveFallbackTitle(Feed feed) {
        var siteTitle = extractHost(feed.getSiteUrl());
        if (StringUtils.hasText(siteTitle)) {
            return siteTitle;
        }

        var feedTitle = extractHost(feed.getUrl());
        if (StringUtils.hasText(feedTitle)) {
            return feedTitle;
        }

        return "未命名订阅";
    }

    private String extractHost(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        var trimmed = url.trim();
        try {
            var uri = new URI(trimmed);
            if (StringUtils.hasText(uri.getHost())) {
                return uri.getHost();
            }
            var path = uri.getPath();
            if (StringUtils.hasText(path)) {
                return path;
            }
        } catch (URISyntaxException ignored) {
            // ignore and fallback below
        }

        return trimmed;
    }
}
