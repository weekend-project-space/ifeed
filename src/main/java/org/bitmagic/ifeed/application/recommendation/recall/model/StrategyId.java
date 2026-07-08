package org.bitmagic.ifeed.application.recommendation.recall.model;

import lombok.Getter;

/**
 * 召回策略通道标识。用于区分不同的召回路径（如用户到物品、物品到物品等）。
 */
@Getter
public enum StrategyId {

    // ==================== 用户系列策略 ====================

    /**
     * User to Action to Item: 用户强行为信号
     */
    U2A2I(1.2, "用户行为推荐"),

    /**
     * User to Item: 用户兴趣建模
     */
    U2I(1.0, "用户兴趣推荐"),

    /**
     * User to Item to Item: 用户关联内容
     */
    U2I2I(0.9, "用户关联推荐"),

    /**
     * User to User: 用户协同过滤
     */
    U2U(0.8, "用户协同过滤"),

    // ==================== 物品系列策略 ====================

    /**
     * Item to Item: 物品协同过滤
     */
    I2I(0.9, "物品协同过滤"),

    /**
     * Random Item to Item: 探索 / 冷启动
     */
    RANDOM_I2I(0.3, "随机物品关联"),

    // ==================== 内容策略 ====================

    /**
     * Latest: 新鲜度兜底
     */
    LATEST(0.2, "最新内容"),

    /**
     * Hot: 热度兜底
     */
    HOT(0.25, "热门内容"),

    // ==================== 混合策略 ====================

    /**
     * Mix: 融合结果（不参与权重计算）
     */
    MIX(0.0, "混合策略");

    // ==================== 枚举属性 ====================

    private final double defaultWeight;
    private final String description;

    StrategyId(double defaultWeight, String description) {
        this.defaultWeight = defaultWeight;
        this.description = description;
    }


}
