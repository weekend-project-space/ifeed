package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.CollectionRequest;
import org.bitmagic.ifeed.api.response.CollectionItemResponse;
import org.bitmagic.ifeed.api.response.CollectionStateResponse;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.api.util.BehaviorPageables;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.UserCollectionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MultiValueMap;

@RestController
@RequestMapping("/api/user/collections")
@RequiredArgsConstructor
public class UserCollectionController {

    private final UserCollectionService userCollectionService;

    @PostMapping("/{articleId}")
    public ResponseEntity<CollectionStateResponse> addToCollection(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @PathVariable String articleId,
                                                                   @RequestBody(required = false) CollectionRequest request) {
        return ResponseEntity.ok(userCollectionService.addToCollection(principal.getId(),
                IdentifierUtils.parseUuid(articleId, "article id"), request));
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<MessageResponse> removeFromCollection(@AuthenticationPrincipal UserPrincipal principal,
                                                                @PathVariable String articleId) {
        userCollectionService.removeFromCollection(principal.getId(), IdentifierUtils.parseUuid(articleId, "article id"));
        return ResponseEntity.ok(new MessageResponse("Article uncollected."));
    }

    @GetMapping
    public ResponseEntity<Page<CollectionItemResponse>> listCollections(@AuthenticationPrincipal UserPrincipal principal,
                                                                        @RequestParam(required = false) String folderId,
                                                                        @RequestParam MultiValueMap<String, String> parameters) {
        var collections = userCollectionService.listCollections(principal.getId(), folderId,
                BehaviorPageables.parse(parameters, "collectedAt"));
        return ResponseEntity.ok(collections);
    }


}
