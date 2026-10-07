package org.bitmagic.ifeed.api.response;

public record OpmlImportSkippedResponse(
        String feedUrl,
        String reason
) {
}
