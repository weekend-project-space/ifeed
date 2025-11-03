package org.bitmagic.ifeed.service.recall;

import java.time.Instant;
import java.util.UUID;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
public record DocScore(UUID id, double score, Instant pubDate, String title) {
}
