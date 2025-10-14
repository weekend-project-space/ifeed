package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.service.AuthService;
import org.bitmagic.ifeed.service.UserReadFeedService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/readfeed")
@RequiredArgsConstructor
public class UserReadFeedController {

    private final UserReadFeedService userReadFeedService;
    private final AuthService authService;

    @PostMapping("/{feedId}")
    public ResponseEntity<MessageResponse> recordReadFeed(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable String feedId) {
        var user = resolveUser(principal);
        userReadFeedService.recordFeedRead(user, IdentifierUtils.parseUuid(feedId, "feed id"));
        return ResponseEntity.ok(new MessageResponse("Feed read."));
    }

    private User resolveUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authService.findUserById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}

