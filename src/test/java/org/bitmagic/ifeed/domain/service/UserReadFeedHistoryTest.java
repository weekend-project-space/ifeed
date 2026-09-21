package org.bitmagic.ifeed.domain.service;

import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReadFeedHistoryTest {

    @Mock
    private UserBehaviorRepository userBehaviorRepository;
    @Mock
    private FeedRepository feedRepository;

    @Test
    void preservesNewerFeedReadTimeAndOtherBehaviorFields() {
        var feedUid = UUID.randomUUID();
        var readAt = Instant.parse("2026-09-21T08:00:00Z");
        var reference = UserBehaviorDocument.FeedRef.builder().feedId(feedUid.toString()).timestamp(readAt).build();
        var collection = UserBehaviorDocument.ArticleRef.builder().articleId(UUID.randomUUID().toString())
                .timestamp(readAt).build();
        var document = UserBehaviorDocument.builder().id("7").collections(List.of(collection))
                .readFeedHistory(new ArrayList<>(List.of(reference))).build();
        when(userBehaviorRepository.findById("7")).thenReturn(Optional.of(document));
        var service = new UserReadFeedService(userBehaviorRepository, feedRepository);

        service.recordFeedRead(7, feedUid, readAt.minusSeconds(60));

        assertEquals(readAt, reference.getTimestamp());
        assertEquals(List.of(collection), document.getCollections());
        verify(userBehaviorRepository).save(document);
    }
}
