package org.bitmagic.ifeed.api.controller;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.ReadHistoryRequest;
import org.bitmagic.ifeed.api.response.MessageResponse;
import org.bitmagic.ifeed.api.response.ReadHistoryItemResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.UserHistoryService;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/history")
@RequiredArgsConstructor
public class UserHistoryController {

    private final UserHistoryService userHistoryService;

    @PostMapping
    public ResponseEntity<MessageResponse> recordHistory(@AuthenticationPrincipal UserPrincipal principal,
                                                         @RequestBody ReadHistoryRequest request) {
        if (request.articleId() == null || request.articleId().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "article_id is required");
        }
        var articleId = IdentifierUtils.parseUuid(request.articleId(), "article id");
        userHistoryService.recordHistory(principal.getId(), articleId, request.readAt());
        return ResponseEntity.ok(new MessageResponse("History recorded."));
    }

    @GetMapping
    public ResponseEntity<Page<ReadHistoryItemResponse>> listHistory(@AuthenticationPrincipal UserPrincipal principal,
                                                                     Pageable pageable) {
        var history = userHistoryService.listHistory(principal.getId(), pageable);
        return ResponseEntity.ok(history);
    }


}
