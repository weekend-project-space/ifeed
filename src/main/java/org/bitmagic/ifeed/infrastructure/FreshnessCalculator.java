package org.bitmagic.ifeed.infrastructure;

import java.time.Instant;
import java.time.Duration;
import java.util.Objects;

/**
 * 指数衰减新鲜度计算器
 * Freshness = e^(-λ * Δt)
 */
public class FreshnessCalculator {

    private final double lambda;     // 衰减系数
    private final double halfLife;   // 半衰期（可选，用于计算 lambda）

    /**
     * 通过半衰期构造（推荐）
     *
     * @param halfLife 半衰期，单位由 timeUnit 指定
     * @param timeUnit 时间单位（毫秒、秒、小时、天）
     */
    public FreshnessCalculator(double halfLife, TimeUnit timeUnit) {
        this.halfLife = convertToMillis(halfLife, timeUnit);
        this.lambda = Math.log(2) / this.halfLife; // λ = ln(2) / T_half
    }

    /**
     * 直接指定 lambda（每毫秒衰减率）
     *
     * @param lambda 衰减系数（越大衰减越快）
     */
    public FreshnessCalculator(double lambda) {
        this.lambda = lambda;
        this.halfLife = Math.log(2) / lambda;
    }

    /**
     * 计算文章新鲜度
     *
     * @param publishedAt 文章发布时间（Instant）
     * @return 新鲜度分数 [0, 1]，越接近 1 越新鲜
     */
    public double calculate(Instant publishedAt) {
        return calculate(publishedAt, Instant.now());
    }

    /**
     * 计算文章新鲜度（支持自定义当前时间，方便测试）
     */
    public double calculate(Instant publishedAt, Instant currentTime) {
        Objects.requireNonNull(publishedAt, "publishedAt cannot be null");
        Objects.requireNonNull(currentTime, "currentTime cannot be null");

        if (publishedAt.isAfter(currentTime)) {
            return 1.0; // 未来文章视为最新
        }

        long deltaMillis = Duration.between(publishedAt, currentTime).toMillis();
        return Math.exp(-lambda * deltaMillis);
    }

    // --- 辅助方法 ---

    private double convertToMillis(double value, TimeUnit unit) {
        return switch (unit) {
            case MILLISECONDS -> value;
            case SECONDS -> value * 1000;
            case MINUTES -> value * 60 * 1000;
            case HOURS -> value * 3600 * 1000;
            case DAYS -> value * 86400 * 1000;
        };
    }

    public double getLambda() {
        return lambda;
    }

    public double getHalfLifeMillis() {
        return halfLife;
    }

    @Override
    public String toString() {
        return String.format("FreshnessCalculator[λ=%.6f, halfLife=%.2f ms]", lambda, halfLife);
    }

    public enum TimeUnit {
        MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS
    }
}
