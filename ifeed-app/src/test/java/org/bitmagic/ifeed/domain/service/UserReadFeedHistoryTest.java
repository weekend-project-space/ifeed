package org.bitmagic.ifeed.domain.service;

import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserReadFeedRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReadFeedHistoryTest {

    @Mock
    private UserReadFeedRepository readFeedRepository;
    @Mock
    private FeedRepository feedRepository;

    @Test
    void resolvesFeedAndWritesReadStateToPostgres() {
        var feedUid = UUID.randomUUID();
        var readAt = Instant.parse("2026-09-21T08:00:00Z");
        var feed = new Feed();
        feed.setId(11);
        feed.setUid(feedUid);
        when(feedRepository.findByUid(feedUid)).thenReturn(Optional.of(feed));
        var service = new UserReadFeedService(feedRepository, readFeedRepository);

        service.recordFeedRead(7, feedUid, readAt.minusSeconds(60));

        verify(readFeedRepository).upsert(7L, 11, readAt.minusSeconds(60));
    }
}
