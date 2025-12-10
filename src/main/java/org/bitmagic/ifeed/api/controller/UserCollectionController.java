package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.CollectionItemResponse;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.AuthService;
import org.bitmagic.ifeed.domain.service.UserCollectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/collections")
@RequiredArgsConstructor
public class UserCollectionController {

    private final UserCollectionService userCollectionService;

    @PostMapping("/{articleId}")
    public ResponseEntity<MessageResponse> addToCollection(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable String articleId) {
        userCollectionService.addToCollection(principal.getId(), IdentifierUtils.parseUuid(articleId, "article id"));
        return ResponseEntity.ok(new MessageResponse("Article collected."));
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<MessageResponse> removeFromCollection(@AuthenticationPrincipal UserPrincipal principal,
                                                                @PathVariable String articleId) {
        userCollectionService.removeFromCollection(principal.getId(), IdentifierUtils.parseUuid(articleId, "article id"));
        return ResponseEntity.ok(new MessageResponse("Article uncollected."));
    }

    @GetMapping
    public ResponseEntity<Page<CollectionItemResponse>> listCollections(@AuthenticationPrincipal UserPrincipal principal,
                                                                        Pageable pageable) {
        var collections = userCollectionService.listCollections(principal.getId(), pageable);
        return ResponseEntity.ok(collections);
    }


}
