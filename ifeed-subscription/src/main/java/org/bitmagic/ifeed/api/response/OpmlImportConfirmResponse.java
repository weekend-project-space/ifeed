package org.bitmagic.ifeed.api.response;

import java.util.List;

public record OpmlImportConfirmResponse(
        int importedCount,
        List<OpmlImportSkippedResponse> skipped,
        String message
) {
}
