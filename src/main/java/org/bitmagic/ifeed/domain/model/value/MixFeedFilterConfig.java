package org.bitmagic.ifeed.domain.model.value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixFeedFilterConfig {

    private Map<String, String> sourceFeeds; // Feed UUID 列表 和名字，空表示所有订阅
    private KeywordFilter keywords;
    private DateRange dateRange;
    private String sortBy;
    private String sortOrder;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordFilter {
        private List<String> include; // 包含任一关键字（OR）
        private List<String> exclude; // 排除所有关键字（AND NOT）
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        private Instant from;
        private Instant to;
    }
}
