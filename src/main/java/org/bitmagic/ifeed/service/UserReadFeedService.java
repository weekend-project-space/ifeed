package org.bitmagic.ifeed.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.entity.Feed;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserReadFeedService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final FeedRepository feedRepository;

    // Record that user read a feed at current time. Upsert latest timestamp per feed.
    @Transactional
    public void recordFeedRead(User user, UUID feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feed not found"));

        var document = userBehaviorRepository.findById(user.getId().toString())
                .orElseGet(() -> UserBehaviorDocument.builder()
                        .id(user.getId().toString())
                        .build());

        if (document.getReadFeedHistory() == null) {
            document.setReadFeedHistory(new java.util.ArrayList<>());
        }

        var feedIdValue = feed.getId().toString();
        var now = Instant.now();

        var existing = document.getReadFeedHistory().stream()
                .filter(ref -> feedIdValue.equals(ref.getFeedId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setTimestamp(now);
        } else {
            document.getReadFeedHistory().add(UserBehaviorDocument.FeedRef.builder()
                    .feedId(feedIdValue)
                    .timestamp(now)
                    .build());
        }

        userBehaviorRepository.save(document);
    }
}

