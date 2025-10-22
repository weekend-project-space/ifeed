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
    private int recentBehaviorLimit = 200;
}
