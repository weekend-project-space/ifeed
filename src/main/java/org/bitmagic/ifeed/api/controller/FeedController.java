package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.FeedDetailResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.FeedService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/{feedId}")
    public ResponseEntity<FeedDetailResponse> getFeed(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable String feedId) {
        ensureAuthenticated(principal);
        var detail = feedService.getFeedDetail(IdentifierUtils.parseUuid(feedId, "feed id"));
        var subscribed = feedService.isSubscribed(principal.getId(), detail.feed());
        return ResponseEntity.ok(toResponse(detail, subscribed));
    }

    @GetMapping("/lookup")
    public ResponseEntity<FeedDetailResponse> lookupFeed(@AuthenticationPrincipal UserPrincipal principal,
                                                         @RequestParam String feedUrl) {
        ensureAuthenticated(principal);
        var detail = feedService.getFeedDetailByUrl(feedUrl);
        var subscribed = feedService.isSubscribed(principal.getId(), detail.feed());
        return ResponseEntity.ok(toResponse(detail, subscribed));
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private FeedDetailResponse toResponse(FeedService.FeedDetail detail, boolean subscribed) {
        var feed = detail.feed();
        return new FeedDetailResponse(
                feed.getId().toString(),
                feed.getTitle(),
                feed.getUrl(),
                feed.getSiteUrl(),
                "https://favicon.im/%s".formatted(extractHost(feed.getSiteUrl())),
                feed.getLastFetched(),
                feed.getLastUpdated(),
                detail.latestPublishedAt(),
                detail.articleCount(),
                detail.subscriberCount(),
                subscribed
        );
    }

    private String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            var uri = new URI(url.trim());
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri.getHost();
            }
            var path = uri.getPath();
            if (path != null && !path.isBlank()) {
                return path;
            }
        } catch (URISyntaxException ignored) {
            return url;
        }
        return url;
    }
}
