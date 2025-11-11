package org.bitmagic.ifeed.application.recommendation.recall.model;

/**
 * 召回策略通道标识。用于区分不同的召回路径（如用户到物品、物品到物品等）。
 */
public enum StrategyId {
    U2U,
    U2I,
    I2I,
    U2I2I,
    U2A2I,
    LATEST,
    HOT,
    RANDOM_I2I
}
