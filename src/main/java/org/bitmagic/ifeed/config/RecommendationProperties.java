package org.bitmagic.ifeed.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.embedding.user.options")
public class RecommendationProperties {

    /**
     * Number of recent user interactions (reads/collections) to consider when building profiles.
     */
    private int recentBehaviorLimit = 200;
    private String defaultProfile = "";
    private double readWeight = 1.0;
    private double collectWeight = 2.0;
    private double skipWeight = -0.6;
    private Duration decayHalfLife = Duration.ofHours(24);
    private int personaTagLimit = 5;
    private int personaCategoryLimit = 3;
    private float llmWeight = 0.7f;   // 与行为向量的混合比例
}
