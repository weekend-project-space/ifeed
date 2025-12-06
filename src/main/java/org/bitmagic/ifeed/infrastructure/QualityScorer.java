package org.bitmagic.ifeed.infrastructure;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yangrd
 * @date 2025/12/5
 *
 * 文章质量评分器 - 极简版
 * <p>
 * 使用方法：
 * ArticleQualityScorer scorer = new ArticleQualityScorer();
 * double score = scorer.score(content, pubDate);
 */
public class QualityScorer {
    /**
     * 主评分方法
     *
     * @param content 文章内容（必需）
     * @param pubDate 发布日期（可选，传null则不考虑时效性）
     * @return 质量分数 0.0-1.0
     */
    public double score(String content, LocalDateTime pubDate) {
        if (content == null || content.trim().isEmpty()) {
            return 0.0;
        }

        // 三个维度：内容(60%) + 时效性(40%)
        double contentScore = scoreContent(content);
        double timeScore = scoreFreshness(pubDate);

        return 0.6 * contentScore + 0.4 * timeScore;
    }

    /**
     * 带更多参数的评分方法
     */
    public double score(String content, LocalDateTime pubDate,
                        int subscriberCount, boolean isVerified) {
        if (content == null || content.trim().isEmpty()) {
            return 0.0;
        }

        double contentScore = scoreContent(content);
        double timeScore = scoreFreshness(pubDate);
        double authorityScore = scoreAuthority(subscriberCount, isVerified);

        return 0.5 * contentScore + 0.3 * timeScore + 0.2 * authorityScore;
    }

    /**
     * 内容质量评分
     */
    private double scoreContent(String content) {
        int length = content.length();

        // 1. 长度分 (40%)
        double lengthScore = calculateLengthScore(length);

        // 2. 结构分 (30%) - 是否有标题、段落
        double structureScore = calculateStructureScore(content);

        // 3. 富媒体分 (30%) - 代码、链接、图片
        double richContentScore = calculateRichContentScore(content);

        return lengthScore * 0.4 + structureScore * 0.3 + richContentScore * 0.3;
    }

    /**
     * 长度评分
     */
    private double calculateLengthScore(int length) {
        if (length < 300) return 0.2;           // 太短
        if (length < 800) return 0.5;           // 偏短
        if (length <= 4000) return 1.0;         // 最佳
        if (length <= 6000) return 0.8;         // 偏长
        if (length <= 10000) return 0.6;        // 较长
        return 0.4;                             // 过长
    }

    /**
     * 结构评分
     */
    private double calculateStructureScore(String content) {
        double score = 0.3; // 基础分

        // 有标题标记
        if (content.matches("(?s).*#{1,6}\\s+.+.*")) {
            score += 0.3;
        }

        // 段落数量
        int paragraphs = content.split("\n\n+").length;
        if (paragraphs >= 3 && paragraphs <= 30) {
            score += 0.4;
        } else if (paragraphs > 30) {
            score += 0.2;
        }

        return Math.min(1.0, score);
    }

    /**
     * 富媒体内容评分
     */
    private double calculateRichContentScore(String content) {
        double score = 0.2; // 基础分

        // 代码块 (最高0.4分)
        int codeBlocks = countMatches(content, "```");
        score += Math.min(0.4, codeBlocks * 0.15);

        // 链接 (最高0.3分)
        int links = countMatches(content, "https?://");
        score += Math.min(0.3, links * 0.05);

        // 图片 (最高0.1分)
        int images = countMatches(content, "!\\[.*?\\]\\(.*?\\)");
        score += Math.min(0.1, images * 0.05);

        return Math.min(1.0, score);
    }

    /**
     * 时效性评分
     */
    private double scoreFreshness(LocalDateTime pubDate) {
        if (pubDate == null) {
            return 0.5; // 默认中等分
        }

        long daysOld = ChronoUnit.DAYS.between(
                pubDate.toLocalDate(),
                LocalDateTime.now().toLocalDate()
        );

        // 指数衰减
        if (daysOld < 0) daysOld = 0; // 未来日期按今天算

        double score = Math.exp(-0.03 * daysOld);
        return Math.max(0.1, Math.min(1.0, score));
    }

    /**
     * 来源权威性评分
     */
    private double scoreAuthority(int subscriberCount, boolean isVerified) {
        double score = 0.5;

        // 订阅数加分
        if (subscriberCount >= 50000) {
            score = 0.95;
        } else if (subscriberCount >= 10000) {
            score = 0.85;
        } else if (subscriberCount >= 1000) {
            score = 0.75;
        } else if (subscriberCount >= 100) {
            score = 0.65;
        }

        // 认证加成
        if (isVerified) {
            score = Math.min(1.0, score + 0.15);
        }

        return score;
    }

    /**
     * 获取评级
     */
    public String getGrade(double score) {
        if (score >= 0.85) return "A";
        if (score >= 0.70) return "B";
        if (score >= 0.55) return "C";
        return "D";
    }

    /**
     * 工具方法：统计正则匹配次数
     */
    private int countMatches(String text, String regex) {
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}
