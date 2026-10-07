package org.bitmagic.ifeed.infrastructure.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;

/**
 * @author yangrd
 * @date 2025/11/8
 **/

@FunctionalInterface
public interface OrderDirection {
    Order apply(CriteriaBuilder cb, Path<?> path);

    OrderDirection ASC = CriteriaBuilder::asc;
    OrderDirection DESC = CriteriaBuilder::desc;
}