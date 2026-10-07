package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.LikeItemResponse;
import org.bitmagic.ifeed.api.response.LikeStateResponse;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.api.util.BehaviorPageables;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.UserLikeService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MultiValueMap;

@RestController
@RequestMapping("/api/user/likes")
@RequiredArgsConstructor
public class UserLikeController {

    private final UserLikeService service;

    @PostMapping("/{articleId}")
    public LikeStateResponse add(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String articleId) {
        return service.add(principal.getId(), IdentifierUtils.parseUuid(articleId, "article id"));
    }

    @DeleteMapping("/{articleId}")
    public MessageResponse remove(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String articleId) {
        service.remove(principal.getId(), IdentifierUtils.parseUuid(articleId, "article id"));
        return new MessageResponse("Article unliked.");
    }

    @GetMapping
    public Page<LikeItemResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestParam MultiValueMap<String, String> parameters) {
        return service.list(principal.getId(), BehaviorPageables.parse(parameters, "likedAt"));
    }
}
