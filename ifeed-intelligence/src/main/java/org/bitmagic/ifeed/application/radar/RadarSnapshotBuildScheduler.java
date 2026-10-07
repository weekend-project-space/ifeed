package org.bitmagic.ifeed.application.radar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RadarSnapshotBuildScheduler {

    private final RadarSnapshotBuildService buildService;

    @Scheduled(
            initialDelayString = "${app.radar.initial-delay:PT1M}",
            fixedDelayString = "${app.radar.fixed-delay:PT1H}"
    )
    public void build() {
        buildService.build();
    }
}
