package org.bitmagic.ifeed.api.response;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

/**
 * A slimmed-down page response with an extra {@code meta} field.
 *
 * <p>We intentionally return only the essential paging fields (page/size/total)
 * and allow additional metadata such as {@code snapshotId} for radar snapshot
 * paging consistency.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        int totalPages,
        long totalElements,
        Map<String, Object> meta) {

    public static <T> PageResponse<T> from(Page<T> page, Map<String, Object> meta) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                meta
        );
    }
}
