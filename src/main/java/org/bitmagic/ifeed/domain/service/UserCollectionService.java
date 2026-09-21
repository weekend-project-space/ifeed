package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.CollectionRequest;
import org.bitmagic.ifeed.api.response.CollectionItemResponse;
import org.bitmagic.ifeed.api.response.CollectionStateResponse;
import org.bitmagic.ifeed.api.util.IdentifierUtils;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.BehaviorPageQuery;
import org.bitmagic.ifeed.domain.repository.UserCollectionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCollectionService {

    private final UserCollectionRepository collectionRepository;
    private final ArticleRepository articleRepository;
    private final UserCollectionFolderService folderService;

    @Transactional
    public CollectionStateResponse addToCollection(Integer userId, UUID articleId, CollectionRequest request) {
        var article = articleRepository.findOne((root, query, builder) -> builder.equal(root.get("uid"), articleId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found"));
        boolean folderSpecified = request != null && request.isFolderSpecified();
        Long folderId = request != null && request.getFolderId() != null
                ? folderService.requireFolder(userId.longValue(), request.getFolderId(), true).id() : null;
        return collectionRepository.save(userId.longValue(), article.getId(), folderId, folderSpecified);
    }

    @Transactional
    public void removeFromCollection(Integer userId, UUID articleId) {
        articleRepository.findOne((root, query, builder) -> builder.equal(root.get("uid"), articleId))
                .ifPresent(article -> collectionRepository.delete(userId.longValue(), article.getId()));
    }

    @Transactional(readOnly = true)
    public boolean isCollected(Integer userId, Long articleId) {
        return collectionRepository.exists(userId.longValue(), articleId);
    }

    @Transactional(readOnly = true)
    public Page<CollectionItemResponse> listCollections(Integer userId, String folder, Pageable pageable) {
        var query = BehaviorPageQuery.of(pageable, "collectedAt",
                Map.of("collectedAt", "collected_at", "timestamp", "collected_at"));
        Long folderId = null;
        if (folder != null && !folder.equals("default")) {
            var folderUid = IdentifierUtils.parseUuid(folder, "folder id");
            folderId = folderService.requireFolder(userId.longValue(), folderUid, false).id();
        }
        return collectionRepository.list(userId.longValue(), folderId, folder != null, query);
    }
}
