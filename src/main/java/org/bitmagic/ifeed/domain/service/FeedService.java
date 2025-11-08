package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedRepository feedRepository;
    private final ArticleRepository articleRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Transactional(readOnly = true)
    public FeedDetail getFeedDetail(UUID feedUid) {
        var feed = feedRepository.findByUid(feedUid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feed not found"));
        return buildDetail(feed);
    }

    @Transactional(readOnly = true)
    public FeedDetail getFeedDetailByUrl(String feedUrl) {
        if (!StringUtils.hasText(feedUrl)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Feed url must not be blank");
        }
        var normalized = feedUrl.trim();
        var feed = feedRepository.findByUrl(normalized)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feed not found"));
        return buildDetail(feed);
    }

    @Transactional(readOnly = true)
    public List<Feed> searchFeeds(String query) {
        if (!StringUtils.hasText(query)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "query must not be empty");
        }
        var normalized = query.trim();
        return feedRepository.searchByQuery(normalized, PageRequest.of(0, 20));
    }

    private FeedDetail buildDetail(Feed feed) {
        var articleCount = articleRepository.countByFeed(feed);
        var subscriberCount = userSubscriptionRepository.countByFeedAndActiveTrue(feed);
        var latestPublishedAt = articleRepository.findTopByFeedOrderByPublishedAtDesc(feed)
                .map(article -> article.getPublishedAt())
                .orElse(feed.getLastUpdated());
        return new FeedDetail(feed, articleCount, subscriberCount, latestPublishedAt);
    }

    @Transactional(readOnly = true)
    public boolean isSubscribed(Integer userId, Feed feed) {
        if (userId == null || feed == null) {
            return false;
        }
        return userSubscriptionRepository.existsByUser_IdAndFeedAndActiveTrue(userId, feed);
    }

    public record FeedDetail(Feed feed, long articleCount, long subscriberCount, Instant latestPublishedAt) {
    }
}
