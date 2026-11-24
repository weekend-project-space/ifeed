package org.bitmagic.ifeed.infrastructure;

import lombok.Getter;

import java.time.Instant;

/**
 * 分钟级指数衰减新鲜度计算器
 * Freshness = e^(-λ * Δt_minutes)
 * 适用于：博客、周刊、知识库、RSS 聚合
 */
public class FreshnessCalculator {

    @Getter
    private final double lambda;           // 每小时衰减率
    private final double halfLifeHours;  // 半衰期

    /**
     * 构造器：指定半衰期（分钟、小时、天）
     */
    public FreshnessCalculator(double halfLife, TimeUnit timeUnit) {
        this.halfLifeHours = toHours(halfLife, timeUnit);
        if (this.halfLifeHours <= 0) {
            throw new IllegalArgumentException("halfLife must be > 0");
        }
        this.lambda = Math.log(2) / this.halfLifeHours;  // λ = ln(2) / T_half
    }

    /**
     * 直接指定 lambda（每小时衰减率）
     */
    public FreshnessCalculator(double lambdaPerHour) {
        this.lambda = lambdaPerHour;
        this.halfLifeHours = Math.log(2) / lambda;
    }

    /**
     * 计算新鲜度（分钟级）
     */
    public double calculate(Instant publishedAt) {
        return calculate(publishedAt, Instant.now());
    }



    public double calculate(Instant publishedAt, Instant currentTime) {
        return FreshnessCalculatorUtils.calculate(lambda, publishedAt, currentTime);
    }

    // --- 单位转换 ---

    private double toHours(double value, TimeUnit unit) {
        return switch (unit) {
            case MINUTES -> value/60;
            case HOURS   -> value ;
            case DAYS    -> value *24;
            default -> throw new IllegalArgumentException("Unsupported unit: " + unit);
        };
    }

    @Override
    public String toString() {
        return String.format("FreshnessCalculator[λ=%.6f/hour, halfLife=%.1f hours]",
                lambda, halfLifeHours);
    }

    /** 支持的时间单位（分钟及以上） */
    public enum TimeUnit {
        MINUTES, HOURS, DAYS
    }
}