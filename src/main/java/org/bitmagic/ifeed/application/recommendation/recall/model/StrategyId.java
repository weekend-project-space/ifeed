package org.bitmagic.ifeed.application.recommendation.recall.model;

import lombok.Getter;

/**
 * 召回策略通道标识。用于区分不同的召回路径（如用户到物品、物品到物品等）。
 */
@Getter
public enum StrategyId {
    // ==================== 用户系列策略 ====================
    /**
     * User to User: 用户协同过滤
     */
    U2U(0.6, "用户协同过滤"),

    /**
     * User to Item: 用户兴趣推荐
     */
    U2I(0.7, "用户兴趣推荐"),

    /**
     * User to Item to Item: 用户关联推荐
     */
    U2I2I(0.8, "用户关联推荐"),

    /**
     * User to Action to Item: 用户行为推荐
     */
    U2A2I(1.0, "用户行为推荐"),

    // ==================== 物品系列策略 ====================
    /**
     * Item to Item: 物品协同过滤
     */
    I2I(0.5, "物品协同过滤"),

    /**
     * Random Item to Item: 随机物品关联
     */
    RANDOM_I2I(0.3, "随机物品关联"),

    // ==================== 内容策略 ====================
    /**
     * Latest: 最新内容
     */
    LATEST(0.2, "最新内容"),

    /**
     * Hot: 热门内容
     */
    HOT(0.1, "热门内容"),

    // ==================== 混合策略 ====================
    /**
     * Mix: 混合策略
     */
    MIX(0.9, "混合策略");

    // ==================== 枚举属性 ====================

    private final double defaultWeight;
    private final String description;

    StrategyId(double defaultWeight, String description) {
        this.defaultWeight = defaultWeight;
        this.description = description;
    }

    // ==================== Getters ====================

}
