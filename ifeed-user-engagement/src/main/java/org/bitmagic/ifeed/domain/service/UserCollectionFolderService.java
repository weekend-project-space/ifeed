package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.CollectionFolderResponse;
import org.bitmagic.ifeed.domain.record.CollectionFolder;
import org.bitmagic.ifeed.domain.repository.BehaviorPageQuery;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCollectionFolderService {

    private final UserCollectionRepository collectionRepository;

    @Transactional
    public CollectionFolderResponse create(Integer userId, String name) {
        try {
            return collectionRepository.createFolder(userId.longValue(), normalizeName(name));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Collection folder name already exists", exception);
        }
    }

    @Transactional
    public CollectionFolderResponse rename(Integer userId, UUID folderUid, String name) {
        var normalized = normalizeName(name);
        var folder = requireFolder(userId.longValue(), folderUid, true);
        try {
            return collectionRepository.renameFolder(userId.longValue(), folder.id(), normalized);
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Collection folder name already exists", exception);
        }
    }

    @Transactional
    public void delete(Integer userId, UUID folderUid) {
        var folder = requireFolder(userId.longValue(), folderUid, true);
        collectionRepository.deleteFolder(userId.longValue(), folder.id());
    }

    @Transactional(readOnly = true)
    public Page<CollectionFolderResponse> list(Integer userId, Pageable pageable) {
        var query = BehaviorPageQuery.of(pageable, "createdAt",
                Map.of("name", "name", "createdAt", "created_at", "updatedAt", "updated_at"));
        return collectionRepository.listFolders(userId.longValue(), query);
    }

    public CollectionFolder requireFolder(Long userId, UUID folderUid, boolean lock) {
        return collectionRepository.findFolder(userId, folderUid, lock)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Collection folder not found"));
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Collection folder name is required");
        }
        var normalized = name.strip();
        if (normalized.isEmpty() || normalized.codePointCount(0, normalized.length()) > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Collection folder name must contain 1 to 50 characters");
        }
        return normalized;
    }
}
