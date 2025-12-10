package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.CategoryResponse;
import org.bitmagic.ifeed.api.response.DiscoveryFeedResponse;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.value.FeedCategory;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private final FeedRepository feedRepository;
    private final ArticleRepository articleRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    /**
     * Get all available categories with feed counts
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        // Get feed counts by category
        Map<String, Long> categoryCounts = getCategoryCounts();

        // Build category responses
        List<CategoryResponse> categories = new ArrayList<>();
        for (FeedCategory category : FeedCategory.values()) {
            Long count = categoryCounts.getOrDefault(category.getCode(), 0L);
            categories.add(new CategoryResponse(
                    category.getCode(),
                    category.getName(),
                    category.getIcon(),
                    count,
                    category.getDescription()));
        }

        return categories;
    }

    /**
     * Browse feeds with pagination, filtering, and sorting
     */
    @Transactional(readOnly = true)
    public Page<DiscoveryFeedResponse> browseFeeds(
            Integer userId,
            String category,
            int page,
            int size,
            String sort) {
        // Build pageable with sorting
        Pageable pageable = buildPageable(page, size, sort);

        // Fetch feeds
        Page<Feed> feedPage;
        if (category != null && !category.equals("all") && FeedCategory.isValidCode(category)) {
            feedPage = feedRepository.findByCategory(category, pageable);
        } else {
            feedPage = feedRepository.findAll(pageable);
        }

        // Get subscription status for all feeds
        Set<Integer> subscribedFeedIds = getSubscribedFeedIds(userId);

        // Get subscriber counts
        List<Integer> feedIds = feedPage.getContent().stream()
                .map(Feed::getId)
                .toList();
        Map<Integer, Long> subscriberCounts = getSubscriberCounts(feedIds);

        // Get article counts
        Map<Integer, Long> articleCounts = getArticleCounts(feedIds);

        // Convert to response
        return feedPage.map(feed -> toDiscoveryFeedResponse(
                feed,
                subscribedFeedIds.contains(feed.getId()),
                subscriberCounts.getOrDefault(feed.getId(), 0L),
                articleCounts.getOrDefault(feed.getId(), 0L)));
    }

    /**
     * Search feeds by keyword
     */
    @Transactional(readOnly = true)
    public Page<DiscoveryFeedResponse> searchFeeds(
            Integer userId,
            String query,
            String category,
            int page,
            int size) {
        // Build pageable
        Pageable pageable = PageRequest.of(page, size);

        // Search feeds
        Page<Feed> feedPage;
        if (category != null && !category.equals("all") && FeedCategory.isValidCode(category)) {
            // Search with category filter - need to convert List to Page
            List<Feed> feeds = feedRepository.searchByQueryAndCategory(query, category, pageable);
            feedPage = new PageImpl<>(feeds, pageable, feeds.size());
        } else {
            // Search all categories - need to convert List to Page
            List<Feed> feeds = feedRepository.searchByQuery(query, pageable);
            feedPage = new PageImpl<>(feeds, pageable, feeds.size());
        }

        // Get subscription status
        Set<Integer> subscribedFeedIds = getSubscribedFeedIds(userId);

        // Get counts
        List<Integer> feedIds = feedPage.getContent().stream().map(Feed::getId).toList();
        Map<Integer, Long> subscriberCounts = getSubscriberCounts(feedIds);
        Map<Integer, Long> articleCounts = getArticleCounts(feedIds);

        // Convert to response
        return feedPage.map(feed -> toDiscoveryFeedResponse(
                feed,
                subscribedFeedIds.contains(feed.getId()),
                subscriberCounts.getOrDefault(feed.getId(), 0L),
                articleCounts.getOrDefault(feed.getId(), 0L)));
    }

    // Helper methods

    private Map<String, Long> getCategoryCounts() {
        List<Object[]> results = feedRepository.countByCategory();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            String category = (String) result[0];
            Long count = (Long) result[1];
            counts.put(category, count);
        }

        // Add total count for "all" category
        long totalCount = counts.values().stream().mapToLong(Long::longValue).sum();
        counts.put("all", totalCount);

        return counts;
    }

    private Pageable buildPageable(int page, int size, String sortBy) {
        // Limit size
        size = Math.min(size, 100);

        Sort sort = switch (sortBy) {
            case "recent" -> Sort.by(Sort.Direction.DESC, "id");
            case "active" -> Sort.by(Sort.Direction.DESC, "lastUpdated");
            default -> // popular: featured first, then by subscriber count (we'll sort in memory for
                       // now)
                Sort.by(Sort.Direction.DESC, "featured");
        };

        return PageRequest.of(page, size, sort);
    }

    private Set<Integer> getSubscribedFeedIds(Integer userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return userSubscriptionRepository.findByUserIdAndSourceTypeAndActiveTrue(userId, SourceType.FEED)
                .stream()
                .map(sub -> sub.getSourceId())
                .collect(Collectors.toSet());
    }

    private Map<Integer, Long> getSubscriberCounts(List<Integer> feedIds) {
        if (feedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> results = userSubscriptionRepository.countBySourceTypeAndSourceIdIn(
                SourceType.FEED, feedIds);
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            Integer feedId = (Integer) result[0];
            Long count = (Long) result[1];
            counts.put(feedId, count);
        }
        return counts;
    }

    private Map<Integer, Long> getArticleCounts(List<Integer> feedIds) {
        if (feedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> results = articleRepository.countByFeedIdIn(feedIds);
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            Integer feedId = (Integer) result[0];
            Long count = (Long) result[1];
            counts.put(feedId, count);
        }
        return counts;
    }

    private DiscoveryFeedResponse toDiscoveryFeedResponse(
            Feed feed,
            boolean subscribed,
            long subscriberCount,
            long articleCount) {
        String host = extractHost(feed.getSiteUrl());
        String favicon = host != null ? "https://favicon.im/" + host : null;

        FeedCategory category = FeedCategory.fromCode(feed.getCategory());

        return new DiscoveryFeedResponse(
                feed.getUid().toString(),
                feed.getTitle(),
                feed.getDescription(),
                feed.getUrl(),
                feed.getSiteUrl(),
                favicon,
                feed.getCategory(),
                category.getName(),
                subscriberCount,
                articleCount,
                feed.getLastUpdated(),
                feed.getUpdateFrequency() != null ? feed.getUpdateFrequency() : "UNKNOWN",
                subscribed,
                feed.getFeatured() != null ? feed.getFeatured() : false);
    }

    private String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri.getHost();
            }
            String path = uri.getPath();
            if (path != null && !path.isBlank()) {
                return path;
            }
        } catch (URISyntaxException ignored) {
            return url;
        }
        return url;
    }
}
