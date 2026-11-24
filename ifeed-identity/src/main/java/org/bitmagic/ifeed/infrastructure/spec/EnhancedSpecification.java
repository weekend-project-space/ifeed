package org.bitmagic.ifeed.infrastructure.spec;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/8
 **/


public record EnhancedSpecification<T>(Specification<T> delegate, List<Order> orders, boolean distinct, Integer offset,
                                       Integer limit) implements Specification<T> {
    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate predicate = delegate.toPredicate(root, query, cb);
        if (distinct) query.distinct(true);
        if (!orders.isEmpty()) query.orderBy(orders.stream().map(o -> o.toOrder(root, cb)).toList());
        return predicate;
    }

    @FunctionalInterface
    interface Order<T> {
        jakarta.persistence.criteria.Order toOrder(Root<T> root, CriteriaBuilder cb);
    }
}
