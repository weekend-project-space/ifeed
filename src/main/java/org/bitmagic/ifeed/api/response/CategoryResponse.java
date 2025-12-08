package org.bitmagic.ifeed.api.response;

public record CategoryResponse(
        String id,
        String name,
        String icon,
        Long feedCount,
        String description) {
}
