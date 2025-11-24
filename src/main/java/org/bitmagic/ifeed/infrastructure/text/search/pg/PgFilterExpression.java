package org.bitmagic.ifeed.infrastructure.text.search.pg;

import org.bitmagic.ifeed.infrastructure.text.search.FilterExpression;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PostgreSQL 过滤表达式实现
 */
public record PgFilterExpression(String expression, Map<String, Object> parameters) implements FilterExpression {

    @Override
    public String toFilterString() {
        return expression;
    }

    @Override
    public void addParameters(MapSqlParameterSource params) {
        parameters.forEach(params::addValue);
    }

    public static PgFilterExpression eq(String field, Object value) {
        return new PgFilterExpression(
                String.format("metadata->>'%s' = :filter_%s", field, field),
                Map.of("filter_" + field, value.toString())
        );
    }

    public static PgFilterExpression contains(String field, Object value) {
        return new PgFilterExpression(
                String.format("metadata @> :filter_%s::jsonb", field),
                Map.of("filter_" + field, String.format("{\"%s\":\"%s\"}", field, value))
        );
    }

    public static PgFilterExpression gt(String field, Number value) {
        return new PgFilterExpression(
                String.format("(metadata->>'%s')::numeric > :filter_%s", field, field),
                Map.of("filter_" + field, value)
        );
    }

    public static PgFilterExpression dateRange(LocalDateTime start, LocalDateTime end) {
        return new PgFilterExpression(
                "created_at BETWEEN :startDate AND :endDate",
                Map.of("startDate", start, "endDate", end)
        );
    }

    public static PgFilterExpression in(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values collection cannot be null or empty");
        }
        return new PgFilterExpression(
                String.format("metadata->>'%s' IN (:filter_%s)", field, field),
                Map.of("filter_" + field, values.stream()
                        .map(Object::toString)
                        .collect(Collectors.toList()))
        );
    }

    public static PgFilterExpression and(PgFilterExpression... expressions) {
        String combined = Arrays.stream(expressions)
                .map(FilterExpression::toFilterString)
                .collect(Collectors.joining(" AND ", "(", ")"));

        Map<String, Object> allParams = new HashMap<>();
        for (PgFilterExpression expr : expressions) {
            allParams.putAll(expr.parameters);
        }
        return new PgFilterExpression(combined, allParams);
    }

    public static PgFilterExpression or(PgFilterExpression... expressions) {
        String combined = Arrays.stream(expressions)
                .map(FilterExpression::toFilterString)
                .collect(Collectors.joining(" OR ", "(", ")"));

        Map<String, Object> allParams = new HashMap<>();
        for (PgFilterExpression expr : expressions) {
            allParams.putAll(expr.parameters);
        }
        return new PgFilterExpression(combined, allParams);
    }
}
