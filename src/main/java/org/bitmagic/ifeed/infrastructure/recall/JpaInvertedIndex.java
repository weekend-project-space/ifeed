package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.spi.InvertedIndex;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserPreferenceService;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 基于 JPA 的倒排索引实现，支持按类目或作者获取最新文章集合。
 */
@Component
@RequiredArgsConstructor
public class JpaInvertedIndex implements InvertedIndex {

    private final ArticleRepository articleRepository;

    @Override
    public List<ScoredId> query(List<UserPreferenceService.AttributePreference> attributes, int k) {
        if (attributes == null || attributes.isEmpty() || k <= 0) {
            return List.of();
        }

        Map<Long, ScoredId> results = new LinkedHashMap<>();
        for (UserPreferenceService.AttributePreference attribute : attributes) {
            String key = attribute.attributeKey();
            if (key == null || key.isBlank()) {
                continue;
            }

            String normalizedKey = key.trim().toLowerCase(Locale.ROOT);
            String value = attribute.attributeValue();
            if (value == null || value.isBlank()) {
                continue;
            }

            List<Long> ids = switch (normalizedKey) {
                case "category" -> articleRepository.findTopIdsByCategory(value, PageRequest.of(0, Math.min(k, 50)));
                case "author" -> articleRepository.findTopIdsByAuthor(value, PageRequest.of(0, Math.min(k, 50)));
                default -> List.of();
            };

            if (ids.isEmpty()) {
                continue;
            }

            Map<String, Object> metadata = Map.of(normalizedKey, value);
            for (Long id : ids) {
                results.merge(id, new ScoredId(id, attribute.score(), metadata),
                        (left, right) -> left.score() >= right.score() ? left : right);
                if (results.size() >= k) {
                    return List.copyOf(results.values());
                }
            }
        }

        return List.copyOf(results.values());
    }
}
