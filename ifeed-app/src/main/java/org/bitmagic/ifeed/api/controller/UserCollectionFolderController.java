package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.CollectionFolderRequest;
import org.bitmagic.ifeed.api.response.CollectionFolderResponse;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.api.util.BehaviorPageables;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.UserCollectionFolderService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MultiValueMap;

@RestController
@RequestMapping("/api/user/collection-folders")
@RequiredArgsConstructor
public class UserCollectionFolderController {

    private final UserCollectionFolderService service;

    @PostMapping
    public ResponseEntity<CollectionFolderResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                           @RequestBody CollectionFolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(principal.getId(), request.name()));
    }

    @PutMapping("/{folderId}")
    public CollectionFolderResponse rename(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable String folderId, @RequestBody CollectionFolderRequest request) {
        return service.rename(principal.getId(), IdentifierUtils.parseUuid(folderId, "folder id"), request.name());
    }

    @DeleteMapping("/{folderId}")
    public MessageResponse delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String folderId) {
        service.delete(principal.getId(), IdentifierUtils.parseUuid(folderId, "folder id"));
        return new MessageResponse("Collection folder deleted.");
    }

    @GetMapping
    public Page<CollectionFolderResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                              @RequestParam MultiValueMap<String, String> parameters) {
        return service.list(principal.getId(), BehaviorPageables.parse(parameters, "createdAt"));
    }
}
