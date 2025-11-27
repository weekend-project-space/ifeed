package org.bitmagic.ifeed.domain.service;

import com.rometools.opml.feed.opml.Opml;
import com.rometools.opml.feed.opml.Outline;
import com.rometools.rome.io.WireFeedInput;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.OpmlImportConfirmRequest;
import org.bitmagic.ifeed.api.response.*;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.model.UserSubscriptionId;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.domain.spec.FeedSpecs;
import org.bitmagic.ifeed.domain.spec.UserSubscriptionSpecs;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OpmlImportService {

    private static final String FAVICON_TEMPLATE = "https://favicon.im/%s";
    private static final int USER_SUBSCRIPTION_QUOTA = 500;

    private final FeedRepository feedRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    /* ======================  预览  ====================== */
    public OpmlPreviewResponse generatePreview(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OPML file is required");
        }

        List<Outline> outlines = parseOPMLWithRome(file);
        if (outlines.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No subscriptions found in OPML file");
        }

        // 1. 去重 + 收集所有 feedUrl
        Set<String> seen = new HashSet<>();
        List<Outline> unique = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();

        for (Outline o : outlines) {
            String feedUrl = o.getXmlUrl();
            if (!StringUtils.hasText(feedUrl)) {
                warnings.add("Skipped outline without feedUrl" +
                        (StringUtils.hasText(o.getTitle()) ? ": " + o.getTitle() : ""));
                continue;
            }
            feedUrl = feedUrl.trim();
            if (!seen.add(feedUrl)) {
                warnings.add("Duplicate feed skipped: " + feedUrl);
                continue;
            }
            unique.add(o);
        }

        if (unique.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No valid subscriptions found in OPML file");
        }

        // 2. 批量查询已存在的 Feed
        Set<String> feedUrls = unique.stream().map(o -> o.getXmlUrl().trim()).collect(java.util.stream.Collectors.toSet());
        Map<String, Feed> feedMap = feedRepository.findAll(FeedSpecs.urlIn(feedUrls)).stream()
                .collect(java.util.stream.Collectors.toMap(Feed::getUrl, f -> f));

        // 3. 批量查询用户已订阅的 FeedId
        Set<Integer> existingFeedIds = feedMap.values().stream()
                .map(Feed::getId).collect(java.util.stream.Collectors.toSet());
        Set<Integer> subscribedFeedIds = userSubscriptionRepository.findAll(
                        UserSubscriptionSpecs.userAndFeedIdsActive(user.getId(), existingFeedIds))
                .stream().map(s -> s.getFeed().getId()).collect(java.util.stream.Collectors.toSet());

        long usedQuota = userSubscriptionRepository.countByUserAndActiveTrue(user);
        int remainingQuota = (int) Math.max(0, USER_SUBSCRIPTION_QUOTA - usedQuota);

        List<OpmlPreviewFeedResponse> feeds = unique.stream()
                .map(o -> toPreviewFeed(o, feedMap.get(o.getXmlUrl().trim()), subscribedFeedIds.contains(
                        feedMap.containsKey(o.getXmlUrl().trim()) ? feedMap.get(o.getXmlUrl().trim()).getId() : null)))
                .toList();

        return new OpmlPreviewResponse(feeds,
                warnings.isEmpty() ? List.of() : List.copyOf(warnings),
                remainingQuota);
    }

    /* ======================  确认导入  ====================== */
    @Transactional
    public OpmlImportConfirmResponse confirm(User user, OpmlImportConfirmRequest request) {
        if (request == null || request.feeds() == null || request.feeds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Feed list cannot be empty");
        }

        // 1. 收集所有待导入的 feedUrl（去重 + 过滤）
        Set<String> selectedUrls = request.feeds().stream()
                .filter(item -> item != null && Boolean.TRUE.equals(item.selected()))
                .map(item -> normalize(item.feedUrl()))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (selectedUrls.isEmpty()) {
            return new OpmlImportConfirmResponse(0, List.of(), "No valid feeds to import.");
        }

        // 2. 批量查已存在的 Feed
        Map<String, Feed> feedMap = feedRepository.findAll(FeedSpecs.urlIn(selectedUrls)).stream()
                .collect(Collectors.toMap(Feed::getUrl, f -> f));

        // 3. 批量查用户已订阅的 FeedId
        Set<Integer> existingFeedIds = feedMap.values().stream()
                .map(Feed::getId)
                .collect(Collectors.toSet());

        Set<Integer> subscribedFeedIds = userSubscriptionRepository.findAll(
                        UserSubscriptionSpecs.userAndFeedIdsActive(user.getId(), existingFeedIds))
                .stream()
                .map(sub -> sub.getFeed().getId())
                .collect(Collectors.toSet());

        // 4. 批量创建缺失的 Feed
        List<Feed> newFeeds = new ArrayList<>();
        Map<String, OpmlImportConfirmRequest.OpmlImportConfirmRequestItem> itemMap = request.feeds().stream()
                .filter(item -> item != null && Boolean.TRUE.equals(item.selected()))
                .collect(Collectors.toMap(
                        item -> normalize(item.feedUrl()),
                        item -> item,
                        (a, b) -> a // 去重
                ));

        for (String url : selectedUrls) {
            if (feedMap.containsKey(url)) continue;

            OpmlImportConfirmRequest.OpmlImportConfirmRequestItem item = itemMap.get(url);
            String siteUrl = fallbackSiteUrl(item.siteUrl(), url);
            String title = resolveFeedTitle(item.title(), siteUrl, url);

            Feed feed = Feed.builder()
                    .url(url)
                    .siteUrl(siteUrl)
                    .title(title)
                    .build();

            newFeeds.add(feed);
            feedMap.put(url, feed); // 预占位
        }

        if (!newFeeds.isEmpty()) {
            feedRepository.saveAllAndFlush(newFeeds); // 刷新 ID
        }

        // 5. 批量创建订阅（跳过已订阅）
        List<UserSubscription> newSubs = new ArrayList<>();
        List<OpmlImportSkippedResponse> skipped = new ArrayList<>();

        long usedQuota = userSubscriptionRepository.countByUserAndActiveTrue(user);
        int remainingQuota = (int) Math.max(0, USER_SUBSCRIPTION_QUOTA - usedQuota);
        int imported = 0;

        for (String url : selectedUrls) {
            Feed feed = feedMap.get(url);
            if (feed.getId() == null) {
                skipped.add(new OpmlImportSkippedResponse(url, SkipReason.FAILED.name()));
                continue;
            }

            if (subscribedFeedIds.contains(feed.getId())) {
                skipped.add(new OpmlImportSkippedResponse(url, SkipReason.ALREADY_SUBSCRIBED.name()));
                continue;
            }

            if (remainingQuota <= 0) {
                skipped.add(new OpmlImportSkippedResponse(url, SkipReason.QUOTA_EXCEEDED.name()));
                continue;
            }

            UserSubscription sub = UserSubscription.builder()
                    .id(new UserSubscriptionId(user.getId(), feed.getId()))
                    .user(user)
                    .feed(feed)
                    .active(true)
                    .build();

            newSubs.add(sub);
            imported++;
            remainingQuota--;
        }

        if (!newSubs.isEmpty()) {
            userSubscriptionRepository.saveAll(newSubs);
        }

        return new OpmlImportConfirmResponse(imported, skipped, "OPML import completed.");
    }
    /* ======================  ROME-OPML 解析  ====================== */
    private List<Outline> parseOPMLWithRome(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            WireFeedInput input = new WireFeedInput();
            Opml opml = (Opml) input.build(new InputStreamReader(is));
            if (opml == null || opml.getOutlines() == null) {
                return List.of();
            }
            return flattenOutlines(opml.getOutlines());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid OPML file: " + e.getMessage(), e);
        }
    }

    /**
     * 递归扁平化所有 outline（支持任意层级嵌套）
     */
    private List<Outline> flattenOutlines(List<Outline> outlines) {
        return outlines.stream()
                .flatMap(out -> {
                    java.util.List<Outline> result = new ArrayList<>();
                    if (StringUtils.hasText(out.getXmlUrl())) {
                        result.add(out);
                    }
                    if (out.getChildren() != null && !out.getChildren().isEmpty()) {
                        result.addAll(flattenOutlines(out.getChildren()));
                    }
                    return result.stream();
                })
                .toList();
    }

    /* ======================  辅助方法  ====================== */
    private OpmlPreviewFeedResponse toPreviewFeed(Outline o, Feed feed, boolean alreadySubscribed) {
        String feedUrl = o.getXmlUrl().trim();
        String siteUrl = fallbackSiteUrl(o.getHtmlUrl(), feedUrl);
        String title = resolveFeedTitle(getTitle(o), siteUrl, feedUrl);
        String avatar = resolveAvatar(siteUrl, feedUrl);

        if (feed != null) {
            if (!StringUtils.hasText(siteUrl)) siteUrl = feed.getSiteUrl();
            if (!StringUtils.hasText(title)) title = feed.getTitle();
        }

        return new OpmlPreviewFeedResponse(feedUrl, title, siteUrl, avatar, alreadySubscribed, List.of());
    }

    private String getTitle(Outline o) {
        return Stream.of(o.getTitle(), o.getText())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private String fallbackSiteUrl(String siteUrl, String feedUrl) {
        return StringUtils.hasText(siteUrl) ? siteUrl.trim() : feedUrl;
    }

    private String resolveFeedTitle(String requested, String siteUrl, String feedUrl) {
        if (StringUtils.hasText(requested)) return requested.trim();
        String host = extractHost(siteUrl);
        if (StringUtils.hasText(host)) return host;
        host = extractHost(feedUrl);
        return StringUtils.hasText(host) ? host : "未命名订阅";
    }

    private String resolveAvatar(String siteUrl, String feedUrl) {
        String host = extractHost(siteUrl);
        if (!StringUtils.hasText(host)) host = extractHost(feedUrl);
        return StringUtils.hasText(host) ? FAVICON_TEMPLATE.formatted(host) : null;
    }

    private String extractHost(String url) {
        if (!StringUtils.hasText(url)) return null;
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (StringUtils.hasText(host)) return host;
            String path = uri.getPath();
            if (StringUtils.hasText(path)) return path;
        } catch (Exception ignored) {
        }
        return url;
    }

    private String normalize(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private enum SkipReason {
        ALREADY_SUBSCRIBED, INVALID_FEED, DUPLICATE, FAILED, QUOTA_EXCEEDED
    }
}