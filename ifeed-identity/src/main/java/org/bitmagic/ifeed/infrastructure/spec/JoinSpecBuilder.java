package org.bitmagic.ifeed.infrastructure.spec;

import jakarta.persistence.criteria.*;

import java.util.Collection;

/**
 * @author yangrd
 * @date 2025/11/8
 **/


public record JoinSpecBuilder<T>(SpecBuilder<T> parent, String field, JoinType joinType) {

    public JoinSpecBuilder(SpecBuilder<T> parent, String field) {
        this(parent, field, JoinType.LEFT);
    }

    private Join<T, ?> getJoin(Root<T> root) {
        return root.join(field, joinType);
    }

    public SpecBuilder<T> eq(String attr, Object value) {
        parent.add(value == null ? null :
                (root, q, cb) -> cb.equal(getJoin(root).get(attr), value));
        return parent;
    }

    public SpecBuilder<T> in(String attr, Collection<?> values) {
        parent.add((values == null || values.isEmpty()) ? null :
                (root, q, cb) -> {
                    CriteriaBuilder.In<Object> in = cb.in(getJoin(root).get(attr));
                    values.forEach(in::value);
                    return in;
                });
        return parent;
    }

    public SpecBuilder<T> like(String attr, String pattern) {
        parent.add((pattern == null || pattern.isBlank()) ? null :
                (root, q, cb) -> cb.like(cb.lower(getJoin(root).get(attr)), "%" + pattern.toLowerCase() + "%"));
        return parent;
    }

    public SpecBuilder<T> contains(String attr, String value) {
        return like(attr, "%" + value + "%");
    }
}