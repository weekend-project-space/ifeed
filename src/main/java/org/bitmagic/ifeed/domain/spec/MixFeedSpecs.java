package org.bitmagic.ifeed.domain.spec;

import jakarta.persistence.criteria.Predicate;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.MixFeed;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * @author yangrd
 * @date 2025/11/28
 **/
public interface MixFeedSpecs {

    static @NotNull Specification<Article> mixFeedArticles(Set<UUID> finalSourceFeedIds, Instant fromDate, Instant toDate, List<String> includeKeywords, List<String> excludeKeywords) {
        Specification<Article> spec = (root, query,
                                       cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            // Filter by source feeds
            if (finalSourceFeedIds != null && !finalSourceFeedIds.isEmpty()) {
                predicates.add(root.get("feed").get("uid").in(finalSourceFeedIds));
            }

            // Filter by date range
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publishedAt"), toDate));
            }

            // Filter by include keywords (OR logic)
            if (includeKeywords != null && !includeKeywords.isEmpty()) {
                List<Predicate> keywordPredicates = new java.util.ArrayList<>();
                for (String keyword : includeKeywords) {
                    String pattern = "%" + keyword.toLowerCase() + "%";
                    keywordPredicates.add(cb.like(cb.lower(root.get("title")), pattern));
                    keywordPredicates.add(cb.like(cb.lower(root.get("content")), pattern));
                }
                predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            }

            // Filter by exclude keywords (AND NOT logic)
            if (excludeKeywords != null && !excludeKeywords.isEmpty()) {
                for (String keyword : excludeKeywords) {
                    String pattern = "%" + keyword.toLowerCase() + "%";
                    predicates.add(cb.notLike(cb.lower(root.get("title")), pattern));
                    predicates.add(cb.notLike(cb.lower(root.get("content")), pattern));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return spec;
    }

    static Specification<MixFeed> toSpec(Integer userId) {
        return Spec.<MixFeed>on()
                .and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user").get("id"), userId)).build();
    }
}
