package org.bitmagic.ifeed.application.radar;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.PageResponse;
import org.bitmagic.ifeed.api.response.radar.RadarItemResponse;
import org.bitmagic.ifeed.api.response.radar.RadarTopicResponse;
import org.bitmagic.ifeed.domain.service.radar.RadarQueryService;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RadarQueryApplicationService {
    private final RadarQueryService radar;

    public PageResponse<RadarTopicResponse> digest(String snapshotId, Integer windowHours, Pageable pageable) {
        if (windowHours != null && (windowHours < 6 || windowHours > 72)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WINDOW_HOURS");
        }
        return radar.digest(snapshotId, windowHours, pageable);
    }

    public PageResponse<RadarItemResponse> topicDetail(String snapshotId, UUID topicId, boolean includeContent, Pageable pageable) {
        return radar.topicDetail(snapshotId, topicId, includeContent, pageable);
    }
}
