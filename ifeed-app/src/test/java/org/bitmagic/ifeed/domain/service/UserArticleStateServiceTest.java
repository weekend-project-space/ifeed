package org.bitmagic.ifeed.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bitmagic.ifeed.domain.record.ArticleBehaviorState;
import org.bitmagic.ifeed.domain.repository.UserArticleStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserArticleStateServiceTest {
    @Mock private UserArticleStateRepository repository;
    private UserArticleStateService service;
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    public record Summary(UUID id, String title) {}

    @BeforeEach
    void setUp() { service = new UserArticleStateService(repository); }

    @Test
    void batchesUniqueIdsAndPreservesOrderPageMetadataAndPublicFolder() {
        var folder = UUID.randomUUID();
        when(repository.findStates(7, List.of(first, second))).thenReturn(Map.of(
                first, new ArticleBehaviorState(true, false, folder),
                second, new ArticleBehaviorState(false, true, null)));
        var source = new PageImpl<>(List.of(new Summary(first, "First"), new Summary(second, "Second"),
                new Summary(first, "Again")), PageRequest.of(2, 3), 20);

        var result = service.attach(7, source, Summary::id);

        assertEquals(20, result.getTotalElements());
        assertEquals(source.getPageable(), result.getPageable());
        assertEquals(List.of("First", "Second", "Again"), result.stream().map(row -> row.article().title()).toList());
        assertTrue(result.getContent().getFirst().collected());
        assertFalse(result.getContent().getFirst().liked());
        assertEquals(folder, result.getContent().getFirst().folderId());
        assertTrue(result.getContent().get(1).liked());
        verify(repository, times(1)).findStates(7, List.of(first, second));
    }

    @Test
    void anonymousAndEmptyListsNeverQueryPersonalData() {
        var summary = new Summary(first, "Article");
        for (var userId : new Integer[]{null, 0, -1}) {
            var result = service.attach(userId, List.of(summary), Summary::id).getFirst();
            assertFalse(result.liked());
            assertFalse(result.collected());
            assertNull(result.folderId());
        }
        assertTrue(service.attach(7, List.<Summary>of(), Summary::id).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void stateBelongsToTheCurrentUserAndMissingRowsDefaultToFalse() {
        when(repository.findStates(7, List.of(first))).thenReturn(Map.of(first, new ArticleBehaviorState(true, true, null)));
        when(repository.findStates(8, List.of(first))).thenReturn(Map.of());
        assertTrue(service.attach(7, List.of(new Summary(first, "Article")), Summary::id).getFirst().liked());
        var other = service.attach(8, List.of(new Summary(first, "Article")), Summary::id).getFirst();
        assertFalse(other.liked());
        assertFalse(other.collected());
        assertNull(other.folderId());
    }

    @Test
    void serializationKeepsTheExistingFlatArticleShape() throws Exception {
        var response = service.attach(0, List.of(new Summary(first, "Article")), Summary::id).getFirst();
        var mapper = new ObjectMapper();
        var json = mapper.readTree(mapper.writeValueAsString(response));
        assertEquals(first.toString(), json.get("id").asText());
        assertEquals("Article", json.get("title").asText());
        assertFalse(json.get("liked").asBoolean());
        assertFalse(json.get("collected").asBoolean());
        assertTrue(json.has("folderId"));
        assertTrue(json.get("folderId").isNull());
        assertFalse(json.has("article"));
    }
}
