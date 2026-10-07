package org.bitmagic.ifeed.api.util;

import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;

public final class BehaviorPageables {

    private BehaviorPageables() {
    }

    public static Pageable parse(MultiValueMap<String, String> parameters, String defaultSort) {
        try {
            int page = Integer.parseInt(parameters.getFirst("page") == null ? "0" : parameters.getFirst("page"));
            int size = Integer.parseInt(parameters.getFirst("size") == null ? "20" : parameters.getFirst("size"));
            if (page < 0 || size < 1 || size > 100) {
                throw new IllegalArgumentException("Invalid page or size");
            }
            var orders = new ArrayList<Sort.Order>();
            for (String value : parameters.getOrDefault("sort", List.of(defaultSort + ",desc"))) {
                var parts = value.split(",", -1);
                if (parts.length > 2 || parts[0].isBlank()) {
                    throw new IllegalArgumentException("Invalid sort");
                }
                var direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1]);
                orders.add(new Sort.Order(direction, parts[0]));
            }
            return PageRequest.of(page, size, Sort.by(orders));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid pagination or sort parameters", exception);
        }
    }
}
