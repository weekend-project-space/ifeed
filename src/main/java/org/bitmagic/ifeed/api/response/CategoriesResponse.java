package org.bitmagic.ifeed.api.response;

import java.util.List;

public record CategoriesResponse(
        List<CategoryResponse> categories) {
}
