package org.bitmagic.ifeed.api.util;

import org.bitmagic.ifeed.domain.repository.BehaviorPageQuery;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BehaviorPageablesTest {

    @Test
    void defaultsAndMultipleSortsAreSupported() {
        var parameters = new LinkedMultiValueMap<String, String>();
        var defaults = BehaviorPageables.parse(parameters, "createdAt");
        assertEquals(0, defaults.getPageNumber());
        assertEquals(20, defaults.getPageSize());
        assertTrue(defaults.getSort().getOrderFor("createdAt").isDescending());
        parameters.add("sort", "name");
        parameters.add("sort", "createdAt,desc");
        var query = BehaviorPageQuery.of(BehaviorPageables.parse(parameters, "createdAt"), "createdAt",
                Map.of("name", "name", "createdAt", "created_at"));
        assertEquals("name ASC, created_at DESC, id ASC", query.orderBy());
    }

    @Test
    void rejectsInvalidPaginationAndSortDirection() {
        for (var entry : Map.of("page", "-1", "size", "101", "sort", "readAt,invalid").entrySet()) {
            var parameters = new LinkedMultiValueMap<String, String>();
            parameters.add(entry.getKey(), entry.getValue());
            assertThrows(ApiException.class, () -> BehaviorPageables.parse(parameters, "readAt"));
        }
        var parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("size", "0");
        assertThrows(ApiException.class, () -> BehaviorPageables.parse(parameters, "readAt"));
    }
}
