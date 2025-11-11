package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ItemFreshnessProvider;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 JPA 的新鲜度数据提供器，从文章表中批量查询发布时间。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaItemFreshnessProvider implements ItemFreshnessProvider {

    private final ArticleRepository articleRepository;

    @Override
    public Map<Long, Instant> publishedAt(Collection<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }

        try {
            List<Object[]> rows = articleRepository.findPublishedAtByIdIn(itemIds);
            Map<Long, Instant> result = new HashMap<>(rows.size());
            for (Object[] row : rows) {
                if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                    continue;
                }

                Long id = convertId(row[0]);
                Instant publishedAt = convertTimestamp(row[1]);
                if (id != null && publishedAt != null) {
                    result.put(id, publishedAt);
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("Failed to load publish times for items {}: {}", itemIds, ex.getMessage());
            return Map.of();
        }
    }


    private Long convertId(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Instant convertTimestamp(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        try {
            return Instant.parse(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
