package org.bitmagic.ifeed.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final ArticleRepository articleRepository;

    public Page<Article> listArticles(Integer page, Integer size, String sort) {
        var pageable = buildPageable(page, size, sort);
        return articleRepository.findAll(pageable);
    }

    public Article getArticle(UUID articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    public Page<Article> searchArticles(String query, Integer page, Integer size) {
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Query must not be blank");
        }
        var pageable = buildPageable(page, size, "publishedAt,desc");
        return articleRepository
                .findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCase(query, query, pageable);
    }

    private Pageable buildPageable(Integer page, Integer size, String sort) {
        int pageNumber = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Sort sortObject = parseSort(sort);
        return PageRequest.of(pageNumber, pageSize, sortObject);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "publishedAt");
        }

        var normalized = sort.trim();
        if (!normalized.contains(",")) {
            return switch (normalized.toLowerCase()) {
                case "latest" -> Sort.by(Sort.Direction.DESC, "publishedAt");
                case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt");
                case "title" -> Sort.by(Sort.Direction.ASC, "title");
                default -> Sort.by(Sort.Direction.DESC, "publishedAt");
            };
        }

        var parts = normalized.split(",");
        var property = parts[0].trim();
        if (property.isEmpty()) {
            property = "publishedAt";
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ignored) {
                direction = Sort.Direction.DESC;
            }
        }

        return Sort.by(direction, property);
    }
}
