package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.SearchResultResponse;
import org.bitmagic.ifeed.api.response.ArticleWithStateResponse;
import org.bitmagic.ifeed.application.search.ArticleSearchQueryService;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final ArticleSearchQueryService search;

    @GetMapping
    public ResponseEntity<Page<ArticleWithStateResponse<SearchResultResponse>>> search(
            @AuthenticationPrincipal UserPrincipal principal, @RequestParam String query,
            @RequestParam(required = false, defaultValue = "semantic") String type,
            @RequestParam(required = false, defaultValue = "owner") String source,
            @RequestParam(required = false) String feedId, @RequestParam(required = false) String tags,
            @RequestParam(required = false) String category,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(search.search(principal == null ? null : principal.getId(), query, type, source,
                feedId, tags, category, pageable));
    }
}
