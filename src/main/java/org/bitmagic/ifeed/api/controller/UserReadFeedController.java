package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.UserReadFeedService;
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

    @PostMapping("/{feedId}")
    public ResponseEntity<MessageResponse> recordReadFeed(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable String feedId) {
        userReadFeedService.recordFeedRead(principal.getId(), IdentifierUtils.parseUuid(feedId, "feed id"));
        return ResponseEntity.ok(new MessageResponse("Feed read."));
    }

}

