package org.bitmagic.ifeed.application.feed.info;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.utils.Strings;
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
            var normalizedTitle = truncate(fetchedTitle.trim(), 255);
            if (!normalizedTitle.equals(feed.getTitle())) {
                feed.setTitle(normalizedTitle);
                feed.setDescription(truncate(syndFeed.getDescription(), 255));
                feed.setSiteUrl(Strings.isBlank(syndFeed.getLink()) ? "" : truncate(syndFeed.getLink(), 255));
            }
            if (syndFeed.getDescription() != null && !syndFeed.getDescription().equals(feed.getDescription())) {
                feed.setDescription(truncate(syndFeed.getDescription(), 255));
            }
            return;
        }

        if (!StringUtils.hasText(feed.getTitle())) {
            feed.setTitle(truncate(resolveFallbackTitle(feed), 255));
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

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
