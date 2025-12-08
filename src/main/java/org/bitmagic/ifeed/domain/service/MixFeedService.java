package org.bitmagic.ifeed.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.MixFeed;
import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.MixFeedFilterConfig;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.MixFeedRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.domain.spec.MixFeedSpecs;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.infrastructure.util.JSON;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MixFeedService {

    private final MixFeedRepository mixFeedRepository;
    private final ArticleRepository articleRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public MixFeed create(User user, String name, String description, String icon, boolean isPublic,
                          MixFeedFilterConfig filterConfig) {
        if (!StringUtils.hasText(name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MixFeed name is required");
        }

        String filterConfigJson = serializeFilterConfig(filterConfig);

        MixFeed mixFeed = MixFeed.builder()
                .user(user)
                .name(name.trim())
                .description(description)
                .icon(icon)
                .isPublic(isPublic)
                .filterConfig(filterConfigJson)
                .subscriberCount(0)
                .build();

        return mixFeedRepository.save(mixFeed);
    }

    @Transactional(readOnly = true)
    public MixFeed getById(UUID uid) {
        return mixFeedRepository.findByUid(uid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MixFeed not found"));
    }

    @Transactional(readOnly = true)
    public MixFeed getByIdAndUser(UUID uid, Integer userId) {
        return mixFeedRepository.findByUidAndUserId(uid, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MixFeed not found or access denied"));
    }

    @Transactional(readOnly = true)
    public List<MixFeed> getMyMixFeeds(Integer userId) {
        return mixFeedRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Page<MixFeed> getPublicMixFeeds(Pageable pageable) {
        return mixFeedRepository.findByIsPublicTrue(pageable);
    }

    @Transactional(readOnly = true)
    public List<MixFeed> searchMixFeeds(String query) {
        if (!StringUtils.hasText(query)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "query must not be empty");
        }
        var normalized = query.trim();
        return mixFeedRepository.searchByQuery(normalized, PageRequest.of(0, 20));
    }

    @Transactional
    public void update(UUID uid, Integer userId, String name, String description, String icon, Boolean isPublic,
                       MixFeedFilterConfig filterConfig) {
        MixFeed mixFeed = getByIdAndUser(uid, userId);

        if (StringUtils.hasText(name)) {
            mixFeed.setName(name.trim());
        }
        if (description != null) {
            mixFeed.setDescription(description);
        }
        if (icon != null) {
            mixFeed.setIcon(icon);
        }
        if (isPublic != null) {
            mixFeed.setIsPublic(isPublic);
        }
        if (filterConfig != null) {
            mixFeed.setFilterConfig(serializeFilterConfig(filterConfig));
        }

        mixFeed.setUpdatedAt(Instant.now());
        mixFeedRepository.save(mixFeed);
    }

    @Transactional
    public void delete(UUID uid, Integer userId) {
        MixFeed mixFeed = getByIdAndUser(uid, userId);

        // Check if there are active subscriptions
        long subscriberCount = userSubscriptionRepository.countBySourceTypeAndSourceIdAndActiveTrue(
                SourceType.MIX_FEED, mixFeed.getId());

        if (subscriberCount > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot delete MixFeed with active subscriptions. Please unsubscribe all users first.");
        }

        mixFeedRepository.delete(mixFeed);
    }

    @Transactional(readOnly = true)
    public MixFeedDetail getDetail(UUID uid, Integer userId) {
        MixFeed mixFeed = getById(uid);

        // Check access: must be public or owned by user
        if (!mixFeed.getIsPublic() && !mixFeed.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied to private MixFeed");
        }

        long articleCount = countArticles(mixFeed);
        long subscriberCount = userSubscriptionRepository.countBySourceTypeAndSourceIdAndActiveTrue(
                SourceType.MIX_FEED, mixFeed.getId());

        return new MixFeedDetail(mixFeed, articleCount, subscriberCount);
    }

    @Transactional(readOnly = true)
    public boolean isSubscribed(Integer userId, MixFeed mixFeed) {
        if (userId == null || mixFeed == null) {
            return false;
        }
        return userSubscriptionRepository.existsByUserIdAndSourceTypeAndSourceIdAndActiveTrue(
                userId, SourceType.MIX_FEED, mixFeed.getId());
    }

    /**
     * Get filtered articles for a MixFeed based on user's own Feed subscriptions
     */
    @Transactional(readOnly = true)
    public Page<ArticleSummaryView> getFilteredArticles(UUID mixFeedUid, Integer userId, Pageable pageable) {
        MixFeed mixFeed = getById(mixFeedUid);

        // Check access
        if (!mixFeed.getIsPublic() && !mixFeed.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied to private MixFeed");
        }

        MixFeedFilterConfig config = mixFeed.config();

        // Extract filter parameters
        Set<UUID> sourceFeedIds = null;
        if (config.getSourceFeeds() != null && !config.getSourceFeeds().isEmpty()) {
            sourceFeedIds = config.getSourceFeeds().keySet().stream()
                    .map(UUID::fromString)
                    .collect(java.util.stream.Collectors.toSet());
        }
        List<String> includeKeywords = config.getKeywords().getInclude();
        List<String> excludeKeywords = config.getKeywords().getExclude();
        Instant fromDate = config.getDateRange() != null ? config.getDateRange().getFrom() : null;
        Instant toDate = config.getDateRange() != null ? config.getDateRange().getTo() : null;

        // Build Specification
        Specification<Article> spec = MixFeedSpecs.mixFeedArticles(sourceFeedIds, fromDate, toDate, includeKeywords, excludeKeywords);

        // Execute query and map results
        return articleRepository.findAll(spec, pageable).map(this::toSummaryView);
    }


    private ArticleSummaryView toSummaryView(org.bitmagic.ifeed.domain.model.Article article) {
        return new ArticleSummaryView(
                article.getUid(),
                article.getId(),
                article.getTitle(),
                article.getLink(),
                article.getSummary(),
                article.getFeed() != null ? article.getFeed().getTitle() : null,
                article.getPublishedAt(),
                article.getTags(),
                article.getThumbnail(),
                article.getEnclosure());
    }

    /**
     * Count articles matching MixFeed filter
     */
    @Transactional(readOnly = true)
    public long countArticles(MixFeed mixFeed) {
        // TODO: Implement count using similar filter logic
        // For now, return 0 as placeholder
        return 0;
    }

    private String serializeFilterConfig(MixFeedFilterConfig config) {
        if (config == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize filter config", e);
        }
    }

    private MixFeedFilterConfig deserializeFilterConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return new MixFeedFilterConfig();
        }
        return JSON.fromJson(json, MixFeedFilterConfig.class);
    }

    public record MixFeedDetail(MixFeed mixFeed, long articleCount, long subscriberCount) {
    }
}
