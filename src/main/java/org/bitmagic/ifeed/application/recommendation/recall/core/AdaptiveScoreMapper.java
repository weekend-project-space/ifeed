package org.bitmagic.ifeed.application.recommendation.recall.core;


/**
 * 自适应分段映射器
 * 将 [0, 1] 区间的分数映射到 [0.5, 1] 区间
 * 适用于推荐排序场景
 */
public class AdaptiveScoreMapper {

    /**
     * 映射策略枚举
     */
    public enum Strategy {
        /**
         * 排序优先模式：拉大头部差距
         * 适合需要精准排序的场景
         */
        RANKING,

        /**
         * 平衡模式：线性映射
         * 适合一般场景
         */
        BALANCED,

        /**
         * 探索模式：保护低分
         * 适合需要鼓励内容探索的场景
         */
        EXPLORATION
    }

    private Strategy strategy;

    /**
     * 构造函数
     *
     * @param strategy 映射策略
     */
    public AdaptiveScoreMapper(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * 默认构造函数，使用排序优先策略
     */
    public AdaptiveScoreMapper() {
        this(Strategy.RANKING);
    }

    /**
     * 映射单个分数
     *
     * @param x 原始分数，范围 [0, 1]
     * @return 映射后分数，范围 [0.5, 1]
     */
    public double map(double x) {
        // 边界检查
        if (x < 0.0) {
            x = 0.0;
        } else if (x > 1.0) {
            x = 1.0;
        }

        switch (strategy) {
            case RANKING:
                return mapRanking(x);
            case EXPLORATION:
                return mapExploration(x);
            case BALANCED:
            default:
                return mapBalanced(x);
        }
    }

    /**
     * 排序优先模式映射
     * 分段策略：
     * [0, 0.3]   -> [0.5, 0.6]   低分压缩
     * [0.3, 0.7] -> [0.6, 0.8]   中分正常
     * [0.7, 1]   -> [0.8, 1]     高分放大
     */
    private double mapRanking(double x) {
        if (x < 0.3) {
            // [0, 0.3] -> [0.5, 0.6]
            return 0.5 + 0.1 * (x / 0.3);
        } else if (x < 0.7) {
            // [0.3, 0.7] -> [0.6, 0.8]
            return 0.6 + 0.2 * ((x - 0.3) / 0.4);
        } else {
            // [0.7, 1] -> [0.8, 1]
            return 0.8 + 0.2 * ((x - 0.7) / 0.3);
        }
    }

    /**
     * 平衡模式映射（线性）
     * [0, 1] -> [0.5, 1]
     */
    private double mapBalanced(double x) {
        return 0.5 + 0.5 * x;
    }

    /**
     * 探索模式映射
     * 保护低分，鼓励探索
     * [0, 0.5] -> [0.5, 0.75]  低分区间占50%输出范围
     * [0.5, 1] -> [0.75, 1]    高分区间占50%输出范围
     */
    private double mapExploration(double x) {
        if (x < 0.5) {
            // [0, 0.5] -> [0.5, 0.75]
            return 0.5 + 0.25 * (x / 0.5);
        } else {
            // [0.5, 1] -> [0.75, 1]
            return 0.75 + 0.25 * ((x - 0.5) / 0.5);
        }
    }


    /**
     * 获取当前策略
     */
    public Strategy getStrategy() {
        return strategy;
    }

    /**
     * 设置映射策略
     */
    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * 根据字符串设置策略
     *
     * @param strategyName "ranking", "balanced", 或 "exploration"
     */
    public void setStrategy(String strategyName) {
        switch (strategyName.toLowerCase()) {
            case "ranking":
                this.strategy = Strategy.RANKING;
                break;
            case "balanced":
                this.strategy = Strategy.BALANCED;
                break;
            case "exploration":
                this.strategy = Strategy.EXPLORATION;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown strategy: " + strategyName +
                                ". Valid options: ranking, balanced, exploration"
                );
        }
    }
}