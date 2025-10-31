package org.bitmagic.ifeed.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.CollectionItemResponse;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCollectionService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public void addToCollection(User user, UUID articleId) {
        var article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found"));

        var document = getOrCreateDocument(user);
        ensureCollectionsInitialized(document);

        var articleIdValue = article.getId().toString();
        var exists = document.getCollections().stream()
                .anyMatch(item -> articleIdValue.equals(item.getArticleId()));

        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "Article already collected");
        }

        document.getCollections().add(UserBehaviorDocument.ArticleRef.builder()
                .articleId(articleIdValue)
                .timestamp(Instant.now())
                .build());

        userBehaviorRepository.save(document);
    }

    @Transactional
    public void removeFromCollection(User user, UUID articleId) {
        var document = userBehaviorRepository.findById(user.getId().toString())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not collected"));

        ensureCollectionsInitialized(document);

        var removed = document.getCollections().removeIf(item -> articleId.toString().equals(item.getArticleId()));
        if (!removed) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Article not collected");
        }

        userBehaviorRepository.save(document);
    }

    @Transactional(readOnly = true)
    public boolean isCollected(UUID userId, UUID articleId) {
        var document = userBehaviorRepository.findById(userId.toString()).orElse(null);
        if (document == null) {
            return false;
        }

        ensureCollectionsInitialized(document);
        var articleIdValue = articleId.toString();
        return document.getCollections().stream()
                .anyMatch(item -> articleIdValue.equals(item.getArticleId()));
    }

    @Transactional(readOnly = true)
    public Page<CollectionItemResponse> listCollections(User user, Pageable pageable) {
        var document = userBehaviorRepository.findById(user.getId().toString()).orElse(null);
        if (document == null) {
            return Page.empty(pageable);
        }

        ensureCollectionsInitialized(document);
        if (document.getCollections().isEmpty()) {
            return Page.empty(pageable);
        }

        var sorted = sortCollections(document, pageable);
        var total = sorted.size();
        var fromIndex = Math.min((int) pageable.getOffset(), total);
        var toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        var pageRefs = sorted.subList(fromIndex, toIndex);

        var articleIds = pageRefs.stream()
                .map(UserBehaviorDocument.ArticleRef::getArticleId)
                .map(UUID::fromString)
                .toList();

        Map<UUID, Article> articles = articleRepository.findByIdIn(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));

        var content = pageRefs.stream()
                .map(item -> {
                    var id = UUID.fromString(item.getArticleId());
                    var article = articles.get(id);
                    var title = article != null ? article.getTitle() : null;
                    return new CollectionItemResponse(item.getArticleId(), title, item.getTimestamp());
                })
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    private List<UserBehaviorDocument.ArticleRef> sortCollections(UserBehaviorDocument document, Pageable pageable) {
        var comparator = Comparator.comparing(UserBehaviorDocument.ArticleRef::getTimestamp);
        var order = pageable.getSort().getOrderFor("collectedAt");
        if (order == null) {
            order = pageable.getSort().getOrderFor("timestamp");
        }
        if (order == null || order.isDescending()) {
            comparator = comparator.reversed();
        }
        return new ArrayList<>(document.getCollections()).stream()
                .sorted(comparator)
                .toList();
    }

    private UserBehaviorDocument getOrCreateDocument(User user) {
        return userBehaviorRepository.findById(user.getId().toString())
                .orElseGet(() -> UserBehaviorDocument.builder()
                        .id(user.getId().toString())
                        .build());
    }

    private void ensureCollectionsInitialized(UserBehaviorDocument document) {
        if (document.getCollections() == null) {
            document.setCollections(new ArrayList<>());
        }
    }
}
