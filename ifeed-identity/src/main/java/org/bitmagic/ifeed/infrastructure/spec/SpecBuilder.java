package org.bitmagic.ifeed.infrastructure.spec;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author yangrd
 * @date 2025/11/8
 **/

public class SpecBuilder<T> {

    private final List<Specification<T>> specs = new ArrayList<>();
    private final List<EnhancedSpecification.Order> orders = new ArrayList<>();
    private boolean distinct = false;
    private Integer offset;
    private Integer limit;

    SpecBuilder() {
    }

    // === 条件添加 ===

    SpecBuilder<T> add(Specification<T> spec) {
        if (spec != null) specs.add(spec);
        return this;
    }

    public SpecBuilder<T> and(Specification<T> spec) {
        return add(spec);
    }

    public SpecBuilder<T> or(Specification<T> spec) {
        if (spec == null || specs.isEmpty()) return this;
        Specification<T> left = Specification.allOf(specs);
        specs.clear();
        specs.add(Specification.anyOf(left, spec));
        return this;
    }

    public SpecBuilder<T> when(boolean condition, Consumer<SpecBuilder<T>> config) {
        if (condition) {
            SpecBuilder<T> sub = new SpecBuilder<>();
            config.accept(sub);
            add(sub.buildSpec());
        }
        return this;
    }

    // === 基础条件 ===

    public SpecBuilder<T> eq(String field, Object value) {
        return add(value == null ? null : (root, q, cb) -> cb.equal(root.get(field), value));
    }

    public SpecBuilder<T> in(String field, Collection<?> values) {
        return add((values == null || values.isEmpty()) ? null :
                (root, q, cb) -> {
                    CriteriaBuilder.In<Object> in = cb.in(root.get(field));
                    values.forEach(in::value);
                    return in;
                });
    }

    public SpecBuilder<T> isTrue(String field) {
        return add((root, q, cb) -> cb.isTrue(root.get(field)));
    }

    public SpecBuilder<T> isFalse(String field) {
        return add((root, q, cb) -> cb.isFalse(root.get(field)));
    }

    public SpecBuilder<T> isNull(String field) {
        return add((root, q, cb) -> cb.isNull(root.get(field)));
    }

    public SpecBuilder<T> notNull(String field) {
        return add((root, q, cb) -> cb.isNotNull(root.get(field)));
    }

    public SpecBuilder<T> like(String field, String pattern) {
        return add((pattern == null || pattern.isBlank()) ? null :
                (root, q, cb) -> cb.like(cb.lower(root.get(field)), pattern.toLowerCase()));
    }

    public SpecBuilder<T> contains(String field, String value) {
        return like(field, "%" + value + "%");
    }

    public SpecBuilder<T> startsWith(String field, String prefix) {
        return like(field, prefix + "%");
    }

    public SpecBuilder<T> endsWith(String field, String suffix) {
        return like(field, "%" + suffix);
    }

    public <V extends Comparable<? super V>> SpecBuilder<T> gt(String field, V value) {
        return add(value == null ? null : (root, q, cb) -> {
            return cb.greaterThan(root.get(field), value);
        });
    }

    public <V extends Comparable<? super V>> SpecBuilder<T> gte(String field, V value) {
        return add(value == null ? null : (root, q, cb) -> {
            return cb.greaterThanOrEqualTo(root.get(field), value);
        });
    }

    public <V extends Comparable<? super V>> SpecBuilder<T> lt(String field, V value) {
        return add(value == null ? null : (root, q, cb) -> {
            return cb.lessThan(root.get(field), value);
        });
    }

    public <V extends Comparable<? super V>> SpecBuilder<T> lte(String field, V value) {
        return add(value == null ? null : (root, q, cb) -> {
            return cb.lessThanOrEqualTo(root.get(field), value);
        });
    }
    // === Join 支持 ===

    public JoinSpecBuilder<T> join(String field) {
        return new JoinSpecBuilder<>(this, field);
    }

    public JoinSpecBuilder<T> leftJoin(String field) {
        return new JoinSpecBuilder<>(this, field, JoinType.LEFT);
    }

    public JoinSpecBuilder<T> innerJoin(String field) {
        return new JoinSpecBuilder<>(this, field, JoinType.INNER);
    }

    // === 排序 ===
    public SpecBuilder<T> orderBy(String field, OrderDirection dir) {
        this.orders.add((root, cb) -> dir.apply(cb, root.get(field)));
        return this;
    }

    public SpecBuilder<T> asc(String field) {
        return orderBy(field, OrderDirection.ASC);
    }

    public SpecBuilder<T> desc(String field) {
        return orderBy(field, OrderDirection.DESC);
    }

    // === 去重、分页 ===

    public SpecBuilder<T> distinct() {
        this.distinct = true;
        return this;
    }

    public SpecBuilder<T> page(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
        return this;
    }

    public SpecBuilder<T> first(int limit) {
        return page(0, limit);
    }

    // === 构建 ===

    public Specification<T> build() {
        Specification<T> spec = specs.isEmpty() ? (root, q, cb) -> cb.conjunction()
                : Specification.allOf(specs);

        return new EnhancedSpecification<>(spec, orders, distinct, offset, limit);
    }

    Specification<T> buildSpec() {
        return specs.isEmpty() ? (root, q, cb) -> cb.conjunction() : Specification.allOf(specs);
    }
}