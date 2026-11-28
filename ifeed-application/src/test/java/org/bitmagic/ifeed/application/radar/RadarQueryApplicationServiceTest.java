package org.bitmagic.ifeed.application.radar;

import org.bitmagic.ifeed.api.response.PageResponse;
import org.bitmagic.ifeed.api.response.radar.RadarItemResponse;
import org.bitmagic.ifeed.api.response.radar.RadarTopicResponse;
import org.bitmagic.ifeed.domain.service.radar.RadarQueryService;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RadarQueryApplicationServiceTest {
    private final RadarQueryService radar = mock(RadarQueryService.class);
    private final RadarQueryApplicationService queries = new RadarQueryApplicationService(radar);

    @Test
    void sharesTheWindowRuleBeforeCallingTheExistingQuery() {
        for (int hours : List.of(5, 73)) {
            assertThatThrownBy(() -> queries.digest(null, hours, PageRequest.of(0, 10)))
                    .isInstanceOfSatisfying(ApiException.class,
                            error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        verifyNoInteractions(radar);
    }

    @Test
    void delegatesToTheExistingQueryWithoutChangingSnapshotOrPaging() {
        var page = PageRequest.of(1, 5);
        var topicId = UUID.randomUUID();
        var digest = new PageResponse<RadarTopicResponse>(List.of(), 1, 5, 0, 0, Map.of("snapshotId", "snapshot"));
        var detail = new PageResponse<RadarItemResponse>(List.of(), 1, 5, 0, 0, Map.of("snapshotId", "snapshot"));
        when(radar.digest("snapshot", 24, page)).thenReturn(digest);
        when(radar.topicDetail("snapshot", topicId, false, page)).thenReturn(detail);
        assertThat(queries.digest("snapshot", 24, page)).isSameAs(digest);
        assertThat(queries.topicDetail("snapshot", topicId, false, page)).isSameAs(detail);
        verify(radar).digest("snapshot", 24, page);
        verify(radar).topicDetail("snapshot", topicId, false, page);
    }
}
