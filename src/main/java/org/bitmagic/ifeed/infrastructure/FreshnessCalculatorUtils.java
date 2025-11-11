package org.bitmagic.ifeed.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * @author yangrd
 * @date 2025/11/11
 **/
public final class FreshnessCalculatorUtils {

    private FreshnessCalculatorUtils() {}

    public static double calculate(double lambdaPerHours, Instant publishedAt, Instant currentTime) {
        Objects.requireNonNull(publishedAt);
        Objects.requireNonNull(currentTime);

        long deltaHours = Duration.between(publishedAt, currentTime).toHours();
        if (deltaHours < 0) deltaHours = 0;

        double score = Math.exp(-lambdaPerHours * deltaHours);
        return Double.isFinite(score) ? score : 0.0;
    }
}
