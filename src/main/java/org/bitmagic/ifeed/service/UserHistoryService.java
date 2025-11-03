package org.bitmagic.ifeed.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ReadHistoryItemResponse;
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
public class UserHistoryService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public void recordHistory(User user, UUID articleId, Instant readAt) {
        var article = articleRepository.findOne((root, query, cb) -> cb.equal(root.get("uid"), articleId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found"));

        var document = getOrCreateDocument(user);
        ensureReadHistoryInitialized(document);

        var articleIdValue = article.getUid().toString();
        var timestamp = readAt != null ? readAt : Instant.now();
//      添加文章阅读记录
        var existing = document.getReadHistory().stream()
                .filter(item -> articleIdValue.equals(item.getArticleId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setTimestamp(timestamp);
        } else {
            document.getReadHistory().add(UserBehaviorDocument.ArticleRef.builder()
                    .articleId(articleIdValue)
                    .timestamp(timestamp)
                    .build());
        }

        //      添加feed阅读记录
        var feedIdValue = article.getFeed().getUid().toString();
        var feedExisting = document.getReadFeedHistory().stream()
                .filter(item -> feedIdValue.equals(item.getFeedId()))
                .findFirst()
                .orElse(null);

        if (feedExisting != null) {
            feedExisting.setTimestamp(timestamp);
        } else {
            document.getReadFeedHistory().add(UserBehaviorDocument.FeedRef.builder()
                    .feedId(feedIdValue)
                    .timestamp(timestamp)
                    .build());
        }

        userBehaviorRepository.save(document);
    }

    @Transactional(readOnly = true)
    public Page<ReadHistoryItemResponse> listHistory(User user, Pageable pageable) {
        var document = userBehaviorRepository.findById(user.getId().toString()).orElse(null);
        if (document == null) {
            return Page.empty(pageable);
        }
        ensureReadHistoryInitialized(document);
        if (document.getReadHistory().isEmpty()) {
            return Page.empty(pageable);
        }

        var sorted = sortHistory(document, pageable);
        var total = sorted.size();
        var fromIndex = Math.min((int) pageable.getOffset(), total);
        var toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        var pageRefs = sorted.subList(fromIndex, toIndex);

        var articleIds = pageRefs.stream()
                .map(UserBehaviorDocument.ArticleRef::getArticleId)
                .map(UUID::fromString)
                .toList();

        Map<UUID, Article> articles = articleRepository.findByUidIn(articleIds).stream()
                .collect(Collectors.toMap(Article::getUid, Function.identity()));

        var content = pageRefs.stream()
                .map(item -> {
                    var id = UUID.fromString(item.getArticleId());
                    var article = articles.get(id);
                    var title = article != null ? article.getTitle() : null;
                    return new ReadHistoryItemResponse(item.getArticleId(), title, item.getTimestamp());
                })
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    private List<UserBehaviorDocument.ArticleRef> sortHistory(UserBehaviorDocument document, Pageable pageable) {
        var comparator = Comparator.comparing(UserBehaviorDocument.ArticleRef::getTimestamp);
        var order = pageable.getSort().getOrderFor("readAt");
        if (order == null) {
            order = pageable.getSort().getOrderFor("timestamp");
        }
        if (order == null || order.isDescending()) {
            comparator = comparator.reversed();
        }
        return new ArrayList<>(document.getReadHistory()).stream()
                .sorted(comparator)
                .toList();
    }

    private UserBehaviorDocument getOrCreateDocument(User user) {
        return userBehaviorRepository.findById(user.getId().toString())
                .orElseGet(() -> UserBehaviorDocument.builder()
                        .id(user.getId().toString())
                        .build());
    }

    private void ensureReadHistoryInitialized(UserBehaviorDocument document) {
        if (document.getReadHistory() == null) {
            document.setReadHistory(new ArrayList<>());
        }
    }
}
