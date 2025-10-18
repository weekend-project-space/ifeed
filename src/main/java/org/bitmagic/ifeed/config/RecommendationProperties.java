package org.bitmagic.ifeed.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationProperties {

    /**
     * Number of recent user interactions (reads/collections) to consider when building profiles.
     */
    private int recentBehaviorLimit = 20;

    /**
     * Maximum ItemCF candidates returned.
     */
    private int collaborativeCandidateLimit = 40;

    /**
     * Maximum content-based candidates returned.
     */
    private int contentCandidateLimit = 40;

    /**
     * Maximum popularity candidates returned.
     */
    private int popularityCandidateLimit = 40;

    /**
     * Time window (days) for ItemCF metric vectors.
     */
    private int collaborativeMetricWindowDays = 30;

    /**
     * Time window (days) for popularity scoring.
     */
    private int popularityWindowDays = 14;

    /**
     * Weight applied to collected counts in popularity score.
     */
    private double popularityWeightCollected = 0.5;

    /**
     * Weight applied to read counts in popularity score.
     */
    private double popularityWeightRead = 0.3;

    /**
     * Weight applied to recency factor in popularity score.
     */
    private double popularityWeightRecency = 0.2;

    /**
     * Half-life (days) for recency exponential decay.
     */
    private double popularityRecencyHalfLifeDays = 7.0;

    /**
     * Maximum number of recommendations returned to the client.
     */
    private int finalCandidateLimit = 30;

    /**
     * Final score coefficient for collaborative similarity.
     */
    private double weightCollaborative = 0.5;

    /**
     * Final score coefficient for content similarity.
     */
    private double weightContent = 0.3;

    /**
     * Final score coefficient for popularity.
     */
    private double weightPopularity = 0.2;

    /**
     * Maximum number of items per feed in the final recommendation list.
     */
    private int feedMaxPerFeed = 3;

    /**
     * Cache TTL (seconds) for recommendation results.
     */
    private long cacheTtlSeconds = 900;

    /**
     * Number of similar articles stored per anchor in the offline matrix.
     */
    private int similarityTopK = 50;
}
