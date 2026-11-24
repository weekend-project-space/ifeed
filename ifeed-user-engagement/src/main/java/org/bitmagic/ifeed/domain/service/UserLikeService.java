package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.LikeItemResponse;
import org.bitmagic.ifeed.api.response.LikeStateResponse;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.BehaviorPageQuery;
import org.bitmagic.ifeed.domain.repository.UserLikeRepository;
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
public class UserLikeService {

    private final UserLikeRepository likeRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public LikeStateResponse add(Integer userId, UUID articleUid) {
        var article = articleRepository.findOne((root, query, builder) -> builder.equal(root.get("uid"), articleUid))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found"));
        return new LikeStateResponse(articleUid, true, likeRepository.save(userId.longValue(), article.getId()));
    }

    @Transactional
    public void remove(Integer userId, UUID articleUid) {
        articleRepository.findOne((root, query, builder) -> builder.equal(root.get("uid"), articleUid))
                .ifPresent(article -> likeRepository.delete(userId.longValue(), article.getId()));
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Integer userId, Long articleId) {
        return likeRepository.exists(userId.longValue(), articleId);
    }

    @Transactional(readOnly = true)
    public Page<LikeItemResponse> list(Integer userId, Pageable pageable) {
        var query = BehaviorPageQuery.of(pageable, "likedAt", Map.of("likedAt", "liked_at"));
        return likeRepository.list(userId.longValue(), query);
    }
}
