package org.bitmagic.ifeed.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OpmlImportConfirmRequest(
        @NotEmpty(message = "feeds must not be empty")
        @Valid
        List<OpmlImportConfirmRequestItem> feeds
) {

    public record OpmlImportConfirmRequestItem(
            String feedUrl,
            String title,
            String siteUrl,
            String avatar,
            Boolean selected
    ) {
    }
}
