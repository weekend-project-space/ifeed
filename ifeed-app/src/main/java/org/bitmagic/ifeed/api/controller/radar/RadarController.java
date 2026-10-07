package org.bitmagic.ifeed.api.controller.radar;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.PageResponse;
import org.bitmagic.ifeed.api.response.radar.RadarItemResponse;
import org.bitmagic.ifeed.api.response.radar.RadarTopicResponse;
import org.bitmagic.ifeed.domain.service.radar.RadarQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/user/radar")
@RequiredArgsConstructor
public class RadarController {

    private final RadarQueryService radarQueryService;

    @GetMapping("/digest")
    public ResponseEntity<PageResponse<RadarTopicResponse>> digest(
            @RequestParam(required = false) String snapshotId,
            @RequestParam(required = false) Integer windowHours,
            Pageable pageable
    ) {
        if (windowHours != null && (windowHours < 6 || windowHours > 72)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WINDOW_HOURS");
        }
        return ResponseEntity.ok(radarQueryService.digest(snapshotId, windowHours, pageable));
    }

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<PageResponse<RadarItemResponse>> topicDetail(
            @RequestParam String snapshotId,
            @PathVariable String topicId,
            @RequestParam(required = false, defaultValue = "false") boolean includeContent,
            Pageable pageable
    ) {
        return ResponseEntity.ok(radarQueryService.topicDetail(snapshotId, UUID.fromString(topicId), includeContent, pageable));
    }
}
