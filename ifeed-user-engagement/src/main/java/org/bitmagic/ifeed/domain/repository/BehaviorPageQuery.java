package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Map;

public record BehaviorPageQuery(Pageable pageable, String orderBy) {

    public static BehaviorPageQuery of(Pageable pageable, String defaultField, Map<String, String> columns) {
        if (pageable.isUnpaged() || pageable.getPageSize() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, defaultField);
        var clauses = new ArrayList<String>();
        for (Sort.Order order : sort) {
            var column = columns.get(order.getProperty());
            if (column == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported sort field: " + order.getProperty());
            }
            clauses.add(column + " " + order.getDirection().name());
        }
        clauses.add("id ASC");
        return new BehaviorPageQuery(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort),
                String.join(", ", clauses));
    }
}
