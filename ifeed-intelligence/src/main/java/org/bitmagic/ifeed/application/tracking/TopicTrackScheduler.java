package org.bitmagic.ifeed.application.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicTrackScheduler {

    private final TopicTrackingService topicTrackingService;

    @Scheduled(initialDelayString = "${app.topic-tracking.initial-delay:PT2M}",
            fixedDelayString = "${app.topic-tracking.fixed-delay:PT10M}")
    public void scanTopics() {
        log.info("start to scan topics");
        var topicIds = topicTrackingService.findEnabledTopicIds();
        int completed = 0;
        for (Long topicId : topicIds) {
            try {
                topicTrackingService.triggerScanAfterCommit(topicId);
                completed++;
            } catch (RuntimeException exception) {
                log.warn("Topic tracking scan failed for topic id {}", topicId, exception);
            }
        }
        log.info("Topic tracking scan finished: topics={}", completed);
    }
}
