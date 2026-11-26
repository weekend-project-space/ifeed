package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.ArticleWithStateResponse;
import org.bitmagic.ifeed.domain.record.ArticleBehaviorState;
import org.bitmagic.ifeed.domain.repository.UserArticleStateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserArticleStateService {
    private final UserArticleStateRepository repository;

    public <T> Page<ArticleWithStateResponse<T>> attach(Integer userId, Page<T> articles, Function<T, UUID> articleUid) {
        return new PageImpl<>(attach(userId, articles.getContent(), articleUid),
                articles.getPageable(), articles.getTotalElements());
    }

    public <T> List<ArticleWithStateResponse<T>> attach(Integer userId, List<T> articles, Function<T, UUID> articleUid) {
        if (articles.isEmpty()) return List.of();
        var ids = articles.stream().map(articleUid).distinct().toList();
        Map<UUID, ArticleBehaviorState> states = userId == null || userId <= 0
                ? Map.of() : repository.findStates(userId, ids);
        return articles.stream().map(article -> {
            var state = states.getOrDefault(articleUid.apply(article), ArticleBehaviorState.EMPTY);
            return new ArticleWithStateResponse<>(article, state.collected(), state.liked(), state.folderId());
        }).toList();
    }
}
