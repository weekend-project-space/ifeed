package org.bitmagic.ifeed.domain.service;

import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserHistoryServiceTest {

    @Mock
    private UserReadHistoryRepository historyRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private UserReadFeedService feedService;

    private UserHistoryService service;
    private Article article;

    @BeforeEach
    void setUp() {
        service = new UserHistoryService(historyRepository, articleRepository, feedService);
        var feed = new Feed();
        feed.setUid(UUID.randomUUID());
        article = new Article();
        article.setId(3_000_000_001L);
        article.setUid(UUID.randomUUID());
        article.setFeed(feed);
    }

    @Test
    void recordsInternalArticleIdAndPreservesFeedReadTime() {
        when(articleRepository.findOne(any(Specification.class))).thenReturn(Optional.of(article));
        var readAt = Instant.parse("2026-09-21T08:00:00Z");

        service.recordHistory(7, article.getUid(), readAt);

        verify(historyRepository).upsert(7L, article.getId(), readAt);
        verify(feedService).recordFeedRead(7, article.getFeed().getUid(), readAt);
    }

    @Test
    void usesServerTimeWhenReadTimeIsAbsent() {
        when(articleRepository.findOne(any(Specification.class))).thenReturn(Optional.of(article));
        var before = Instant.now();

        service.recordHistory(7, article.getUid(), null);

        var timestamp = ArgumentCaptor.forClass(Instant.class);
        verify(historyRepository).upsert(eq(7L), eq(article.getId()), timestamp.capture());
        assertFalse(timestamp.getValue().isBefore(before));
        assertFalse(timestamp.getValue().isAfter(Instant.now()));
        verify(feedService).recordFeedRead(7, article.getFeed().getUid(), timestamp.getValue());
    }

    @Test
    void missingArticleDoesNotWriteHistoryOrFeedState() {
        when(articleRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        var error = assertThrows(ApiException.class, () -> service.recordHistory(7, article.getUid(), null));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verifyNoInteractions(historyRepository, feedService);
    }

    @Test
    void supportsLegacyTimestampSortAndDefaultsToNewestFirst() {
        service.listHistory(7, PageRequest.of(2, 20, Sort.by("timestamp")));
        service.listHistory(7, PageRequest.of(0, 20));

        verify(historyRepository).findByUserId(7L, PageRequest.of(2, 20, Sort.by("readAt")));
        verify(historyRepository).findByUserId(7L,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "readAt")));
    }

    @Test
    void rejectsUnsupportedSortAndOversizedPage() {
        assertThrows(ApiException.class, () -> service.listHistory(7, PageRequest.of(0, 20, Sort.by("userId"))));
        assertThrows(ApiException.class, () -> service.listHistory(7, PageRequest.of(0, 101)));
        verifyNoInteractions(historyRepository);
    }

    @Test
    void deletionIsScopedToUserAndPreservesMissingHistoryResponse() {
        when(articleRepository.findOne(any(Specification.class))).thenReturn(Optional.of(article));
        when(historyRepository.delete(7L, article.getId())).thenReturn(1, 0);

        service.removeFromHistory(7, article.getUid());
        var error = assertThrows(ApiException.class, () -> service.removeFromHistory(7, article.getUid()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verify(historyRepository, times(2)).delete(7L, article.getId());
        verifyNoInteractions(feedService);
    }
}
