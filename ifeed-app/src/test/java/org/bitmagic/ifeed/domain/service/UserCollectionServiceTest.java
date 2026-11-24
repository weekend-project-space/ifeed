package org.bitmagic.ifeed.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bitmagic.ifeed.api.request.CollectionRequest;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCollectionServiceTest {

    @Mock
    private UserCollectionRepository repository;
    @Mock
    private ArticleRepository articleRepository;

    private UserCollectionFolderService folders;
    private UserCollectionService service;
    private Article article;

    @BeforeEach
    void setUp() {
        folders = new UserCollectionFolderService(repository);
        service = new UserCollectionService(repository, articleRepository, folders);
        article = new Article();
        article.setId(42L);
        article.setUid(UUID.randomUUID());
    }

    @Test
    void distinguishesMissingAndExplicitNullFolder() throws Exception {
        var mapper = new ObjectMapper();
        when(articleRepository.findOne(any(Specification.class))).thenReturn(Optional.of(article));

        service.addToCollection(7, article.getUid(), mapper.readValue("{}", CollectionRequest.class));
        service.addToCollection(7, article.getUid(), mapper.readValue("{\"folderId\":null}", CollectionRequest.class));

        verify(repository).save(7L, 42L, null, false);
        verify(repository).save(7L, 42L, null, true);
        assertThrows(Exception.class, () -> mapper.readValue("{\"folderId\":\"\"}", CollectionRequest.class));
        assertFalse(mapper.readValue("{\"folderSpecified\":true}", CollectionRequest.class).isFolderSpecified());
    }

    @Test
    void foreignOrMissingFolderCannotReceiveCollection() {
        var request = new CollectionRequest();
        var folderUid = UUID.randomUUID();
        request.setFolderId(folderUid.toString());
        when(articleRepository.findOne(any(Specification.class))).thenReturn(Optional.of(article));
        when(repository.findFolder(7L, folderUid, true)).thenReturn(Optional.empty());

        var error = assertThrows(ApiException.class, () -> service.addToCollection(7, article.getUid(), request));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verify(repository, never()).save(anyLong(), anyLong(), any(), anyBoolean());
    }

    @Test
    void removingAbsentArticleIsIdempotent() {
        when(articleRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        service.removeFromCollection(7, article.getUid());

        verifyNoInteractions(repository);
    }

    @Test
    void normalizesNamesAndMapsDuplicatesToConflict() {
        folders.create(7, "  Folder  ");
        verify(repository).createFolder(7L, "Folder");
        when(repository.createFolder(7L, "Duplicate")).thenThrow(new DuplicateKeyException("duplicate"));

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApiException.class, () -> folders.create(7, "Duplicate")).getStatus());
        assertThrows(ApiException.class, () -> folders.create(7, "   "));
        assertThrows(ApiException.class, () -> folders.create(7, "a".repeat(51)));
    }

    @Test
    void invalidFolderFilterIsRejected() {
        assertThrows(ApiException.class, () -> service.listCollections(7, "", PageRequest.of(0, 20)));
        assertThrows(ApiException.class, () -> service.listCollections(7, "null", PageRequest.of(0, 20)));
        verifyNoInteractions(repository);
    }
}
