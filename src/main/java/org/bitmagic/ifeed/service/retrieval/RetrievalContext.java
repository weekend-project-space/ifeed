package org.bitmagic.ifeed.service.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
@Data
@AllArgsConstructor
@Builder
public class RetrievalContext {

    @Nullable
    String query;
    @Nullable
    float[] embedding;
    @Nullable
    Integer userId;
    boolean includeGlobal;
    int topK;
    @Nullable
    List<Integer> feedIds;
}
