package org.bitmagic.ifeed.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.topic-tracking")
@Validated
public class TopicTrackingProperties {
    @Min(1)
    @Max(3650)
    private int historyDays = 180;
    @Min(1)
    @Max(1000)
    private int candidatePageSize = 200;
    @Min(1)
    @Max(128)
    private int embeddingBatchSize = 16;
    @Min(1)
    @Max(10000)
    private int embeddingTextMaxLength = 2000;
    @Min(1)
    @Max(30)
    private int summaryContentArticleLimit = 30;
    @Min(1)
    @Max(10000)
    private int summaryTextMaxLength = 2000;
    @Min(4096)
    @Max(1000000)
    private int summaryInputMaxBytes = 12000;
    @Min(1)
    @Max(32768)
    private int summaryMaxTokens = 1024;
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("1.0")
    private double clusterSimilarityThreshold = 0.85;
    private boolean jevEnabled = true;
    @DecimalMin(value = "0.0")
    @DecimalMax("1.0")
    private double jevRelevanceThreshold = 0.5;
    @Min(256)
    @Max(10000)
    private int jevTextMaxLength = 2000;
    @Min(1)
    @Max(3)
    private int maxCategories = 3;
    @Min(1)
    @Max(30)
    private int maxSourcesPerCategory = 10;
    private String fixedDelay = "PT10M";
    private String initialDelay = "PT2M";
}
