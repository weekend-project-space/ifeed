package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserReadFeedRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserReadFeedService {

    private final FeedRepository feedRepository;
    private final UserReadFeedRepository readFeedRepository;

    // Record that user read a feed at current time. Upsert latest timestamp per feed.
    @Transactional
    public void recordFeedRead(Integer userId, UUID feedUid) {
        recordFeedRead(userId, feedUid, Instant.now());
    }

    @Transactional
    public void recordFeedRead(Integer userId, UUID feedUid, Instant readAt) {
        var feed = feedRepository.findByUid(feedUid)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feed not found"));
        readFeedRepository.upsert(userId.longValue(), feed.getId(), readAt);
    }
}
