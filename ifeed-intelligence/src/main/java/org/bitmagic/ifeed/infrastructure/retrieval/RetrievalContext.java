package org.bitmagic.ifeed.infrastructure.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bitmagic.ifeed.domain.model.value.MixFeedFilterConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
@Data
@AllArgsConstructor
@Builder
public class RetrievalContext {

    @NotNull
    String query;
    @Nullable
    Integer userId;
    boolean includeGlobal;
    int topK;
    double threshold = 0.3;
    //    扩展， 全站 (includeGlobal)，我的(userId)，订阅源(sourceFeeds)
    @Nullable
    Collection<Integer> sourceFeeds;
    @Nullable
    DateRange dateRange;

    @Builder
    public record DateRange(Instant from, Instant to) {
        public static DateRange of(Instant from, Instant to) {
            return new DateRange(from, to);
        }
    }
}
