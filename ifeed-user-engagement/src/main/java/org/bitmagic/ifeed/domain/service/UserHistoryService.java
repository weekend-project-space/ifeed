package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ReadHistoryItemResponse;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserReadHistoryRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserHistoryService {

    private final UserReadHistoryRepository historyRepository;
    private final ArticleRepository articleRepository;
    private final UserReadFeedService userReadFeedService;

    @Transactional
    public void recordHistory(Integer userId, UUID articleId, Instant readAt) {
        var article = articleRepository.findOne((root, query, builder) -> builder.equal(root.get("uid"), articleId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found"));
        var timestamp = readAt != null ? readAt : Instant.now();
        historyRepository.upsert(userId.longValue(), article.getId(), timestamp);
        userReadFeedService.recordFeedRead(userId, article.getFeed().getUid(), timestamp);
    }

    @Transactional(readOnly = true)
    public Page<ReadHistoryItemResponse> listHistory(Integer userId, Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getPageSize() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "History page size must be between 1 and 100");
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!order.getProperty().equals("readAt") && !order.getProperty().equals("timestamp")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported history sort field");
            }
        }
        var order = pageable.getSort().getOrderFor("readAt");
        if (order == null) {
            order = pageable.getSort().getOrderFor("timestamp");
        }
        var direction = order != null ? order.getDirection() : Sort.Direction.DESC;
        var normalized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(direction, "readAt"));
        return historyRepository.findByUserId(userId.longValue(), normalized);
    }

    @Transactional
    public void removeFromHistory(Integer userId, UUID articleId) {
        var article = articleRepository.findOne((root, query, builder) -> builder.equal(root.get("uid"), articleId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not read"));
        if (historyRepository.delete(userId.longValue(), article.getId()) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Article not read");
        }
    }
}
