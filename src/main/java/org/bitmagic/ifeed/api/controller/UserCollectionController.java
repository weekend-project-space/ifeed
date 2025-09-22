package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.CollectionItemResponse;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.AuthService;
import org.bitmagic.ifeed.service.UserCollectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/collections")
@RequiredArgsConstructor
public class UserCollectionController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final UserCollectionService userCollectionService;
    private final AuthService authService;

    @PostMapping("/{articleId}")
    public ResponseEntity<MessageResponse> addToCollection(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable String articleId) {
        var user = resolveUser(principal);
        userCollectionService.addToCollection(user, IdentifierUtils.parseUuid(articleId, "article id"));
        return ResponseEntity.ok(new MessageResponse("Article collected."));
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<MessageResponse> removeFromCollection(@AuthenticationPrincipal UserPrincipal principal,
                                                                @PathVariable String articleId) {
        var user = resolveUser(principal);
        userCollectionService.removeFromCollection(user, IdentifierUtils.parseUuid(articleId, "article id"));
        return ResponseEntity.ok(new MessageResponse("Article uncollected."));
    }

    @GetMapping
    public ResponseEntity<Page<CollectionItemResponse>> listCollections(@AuthenticationPrincipal UserPrincipal principal,
                                                                        @RequestParam(required = false) Integer page,
                                                                        @RequestParam(required = false) Integer size,
                                                                        @RequestParam(required = false) String sort) {
        var user = resolveUser(principal);
        var pageable = buildPageable(page, size, sort);
        var collections = userCollectionService.listCollections(user, pageable);
        return ResponseEntity.ok(collections);
    }

    private Pageable buildPageable(Integer page, Integer size, String sort) {
        int pageNumber = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Sort sortSpec;
        if (sort == null || sort.isBlank()) {
            sortSpec = Sort.by(Sort.Direction.DESC, "collectedAt");
        } else {
            var parts = sort.split(",");
            var property = parts[0].trim();
            if (property.isEmpty()) {
                property = "collectedAt";
            }
            Sort.Direction direction = Sort.Direction.DESC;
            if (parts.length > 1) {
                try {
                    direction = Sort.Direction.fromString(parts[1].trim());
                } catch (IllegalArgumentException ignored) {
                    direction = Sort.Direction.DESC;
                }
            }
            sortSpec = Sort.by(direction, property);
        }

        return PageRequest.of(pageNumber, pageSize, sortSpec);
    }

    private User resolveUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authService.findUserById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
