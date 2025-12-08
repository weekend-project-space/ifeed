package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.CategoriesResponse;
import org.bitmagic.ifeed.api.response.DiscoveryFeedResponse;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.DiscoveryService;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    /**
     * Get all available categories
     * GET /api/discovery/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<CategoriesResponse> getCategories() {
        var categories = discoveryService.getAllCategories();
        return ResponseEntity.ok(new CategoriesResponse(categories));
    }

    /**
     * Browse feeds with pagination, filtering, and sorting
     * GET /api/discovery/feeds?category=tech&page=0&size=20&sort=popular
     */
    @GetMapping("/feeds")
    public ResponseEntity<Page<DiscoveryFeedResponse>> browseFeeds(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "popular") String sort) {
        ensureAuthenticated(principal);

        // Validate and limit size
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        var feeds = discoveryService.browseFeeds(
                principal.getId(),
                category,
                page,
                size,
                sort);

        return ResponseEntity.ok(feeds);
    }

    /**
     * Search feeds by keyword
     * GET /api/discovery/feeds/search?q=techcrunch&category=tech
     */
    @GetMapping("/feeds/search")
    public ResponseEntity<Page<DiscoveryFeedResponse>> searchFeeds(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ensureAuthenticated(principal);

        // Validate query
        if (q == null || q.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Search query cannot be empty");
        }

        // Validate and limit size
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        var result = discoveryService.searchFeeds(
                principal.getId(),
                q.trim(),
                category,
                page,
                size);

        return ResponseEntity.ok(result);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
