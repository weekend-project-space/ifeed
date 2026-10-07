package org.bitmagic.ifeed.infrastructure.text.search.pg;

import java.time.LocalDateTime; /**
 * 搜索统计信息
 */
public record SearchStats(long total, double avgLength, long indexSize, LocalDateTime lastUpdated) {}
