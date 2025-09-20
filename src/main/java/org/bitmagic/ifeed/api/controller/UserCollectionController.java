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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/collections")
@RequiredArgsConstructor
public class UserCollectionController {

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
    public ResponseEntity<List<CollectionItemResponse>> listCollections(@AuthenticationPrincipal UserPrincipal principal) {
        var user = resolveUser(principal);
        var collections = userCollectionService.listCollections(user);
        return ResponseEntity.ok(collections);
    }

    private User resolveUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authService.findUserById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
