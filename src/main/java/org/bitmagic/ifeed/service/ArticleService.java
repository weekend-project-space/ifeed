package org.bitmagic.ifeed.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ArticleRepository articleRepository;

    public List<Article> listArticles(Integer limit, Integer offset, String sort) {
        var pageable = buildPageable(limit, offset, sort);
        return articleRepository.findAll(pageable).getContent();
    }

    public Article getArticle(UUID articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    public List<Article> searchArticles(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Query must not be blank");
        }

        if (limit <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid limit");
        }

        var size = Math.min(limit, MAX_LIMIT);
        var pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return articleRepository
                .findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCase(query, query, pageable)
                .getContent();
    }

    private Pageable buildPageable(Integer limit, Integer offset, String sort) {
        var size = limit == null ? DEFAULT_LIMIT : limit;
        if (size <= 0 || size > MAX_LIMIT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid limit");
        }

        var offsetValue = offset == null ? 0 : offset;
        if (offsetValue < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid offset");
        }

        var sortObject = switch (sort == null ? "latest" : sort.toLowerCase()) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt");
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            default -> Sort.by(Sort.Direction.DESC, "publishedAt");
        };

        var page = offsetValue / size;
        return PageRequest.of(page, size, sortObject);
    }
}
