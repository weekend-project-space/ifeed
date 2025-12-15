package org.bitmagic.ifeed.infrastructure.score;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 纯内容质量评分器
 * <p>
 * 只基于文章内容本身评估质量，不依赖：
 * - 发布时间
 * - 作者权威性
 * - 用户互动数据
 * <p>
 * 评分维度：
 * 1. 内容深度 (30%) - 长度、信息密度
 * 2. 可读性 (25%) - 结构、段落、句子
 * 3. 富媒体 (20%) - 代码、图片、链接
 * 4. 专业性 (15%) - 术语、引用、数据
 * 5. 原创性 (10%) - 词汇多样性、表达独特性
 */
public class QualityScorer {

    // 预编译正则
    private static final Pattern CODE_BLOCK = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern INLINE_CODE = Pattern.compile("`[^`]+`");
    private static final Pattern LINK = Pattern.compile("https?://[^\\s)]+");
    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\([^)]+\\)");
    private static final Pattern HEADER = Pattern.compile("^#{1,6}\\s+.+$", Pattern.MULTILINE);
    private static final Pattern LIST = Pattern.compile("^[*+-]\\s+|^\\d+\\.\\s+", Pattern.MULTILINE);
    private static final Pattern QUOTE = Pattern.compile("^>\\s+", Pattern.MULTILINE);
    private static final Pattern TABLE = Pattern.compile("\\|.+\\|\\n\\|[-:| ]+\\|");
    private static final Pattern NUMBER = Pattern.compile("\\d+\\.\\d+|\\d+%|\\$\\d+");
    private static final Pattern CITATION = Pattern.compile("\\[\\d+\\]|\\[\\^\\d+\\]");

    /**
     * 主评分方法
     */
    public ScoreResult score(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ScoreResult(0.0, new HashMap<>(), null);
        }

        // 提取内容特征
        ContentFeatures features = extractFeatures(content);

        // 各维度评分
        Map<String, Double> scores = new HashMap<>();
        scores.put("depth", scoreDepth(features));           // 30%
        scores.put("readability", scoreReadability(features)); // 25%
        scores.put("richness", scoreRichness(features));      // 20%
        scores.put("professionalism", scoreProfessionalism(features)); // 15%
        scores.put("originality", scoreOriginality(features));  // 10%

        // 加权计算
        double total = scores.get("depth") * 0.30
                + scores.get("readability") * 0.25
                + scores.get("richness") * 0.20
                + scores.get("professionalism") * 0.15
                + scores.get("originality") * 0.10;

        return new ScoreResult(total, scores, features);
    }

    /**
     * 提取内容特征（一次遍历）
     */
    private ContentFeatures extractFeatures(String content) {
        ContentFeatures f = new ContentFeatures();

        // 基础统计
        f.rawLength = content.length();
        f.lines = content.split("\n").length;
        f.paragraphs = content.split("\n\\s*\n+").length;

        // 去除代码块后的纯文本（避免代码影响统计）
        String textOnly = CODE_BLOCK.matcher(content).replaceAll("");
        f.textLength = textOnly.length();

        // 句子分析
        String[] sentences = textOnly.split("[.!?。！？]+");
        List<Integer> sentLengths = new ArrayList<>();
        for (String s : sentences) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                int words = trimmed.split("\\s+").length;
                sentLengths.add(words);
            }
        }
        f.sentenceCount = sentLengths.size();
        f.avgSentenceLength = sentLengths.isEmpty() ? 0 :
                sentLengths.stream().mapToInt(Integer::intValue).average().orElse(0);
        f.sentenceLengthStd = calculateStd(sentLengths);

        // 词汇分析
        String[] words = textOnly.toLowerCase()
                .replaceAll("[^a-z0-9\\s\\u4e00-\\u9fa5]", " ")
                .split("\\s+");
        f.wordCount = words.length;
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        uniqueWords.remove("");
        f.uniqueWordCount = uniqueWords.size();
        f.vocabularyRichness = f.wordCount > 0 ? (double) f.uniqueWordCount / f.wordCount : 0;

        // 词频分析（检测重复）
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String w : words) {
            if (w.length() > 3) { // 只统计有意义的词
                wordFreq.put(w, wordFreq.getOrDefault(w, 0) + 1);
            }
        }
        f.topWordRepetition = wordFreq.values().stream()
                .mapToInt(Integer::intValue)
                .max().orElse(0);

        // 富媒体统计
        f.codeBlockCount = countMatches(CODE_BLOCK, content);
        f.inlineCodeCount = countMatches(INLINE_CODE, content);
        f.imageCount = countMatches(IMAGE, content);
        f.linkCount = countMatches(LINK, content);
        f.headerCount = countMatches(HEADER, content);
        f.listItemCount = countMatches(LIST, content);
        f.quoteCount = countMatches(QUOTE, content);
        f.tableCount = countMatches(TABLE, content);

        // 专业性指标
        f.numberCount = countMatches(NUMBER, content);
        f.citationCount = countMatches(CITATION, content);

        // 代码质量分析（如果有代码块）
        if (f.codeBlockCount > 0) {
            f.avgCodeBlockLength = extractCodeBlocks(content).stream()
                    .mapToInt(String::length)
                    .average().orElse(0);
        }

        // 结构复杂度
        f.structureComplexity = calculateStructureComplexity(f);

        return f;
    }

    /**
     * 1. 内容深度评分 (30%)
     * 考虑：长度、信息密度、结构复杂度
     */
    private double scoreDepth(ContentFeatures f) {
        double score = 0.0;

        // 1.1 长度得分 (40%) - 使用正态分布
        double lengthScore = calculateLengthScore(f.textLength);
        score += lengthScore * 0.40;

        // 1.2 信息密度 (35%) - 词汇量/长度
        double density = f.wordCount > 0 ? (double) f.textLength / f.wordCount : 0;
        double densityScore = 0.0;
        if (density >= 5 && density <= 7) { // 最佳密度（中英文混合）
            densityScore = 1.0;
        } else if (density >= 4 && density <= 8) {
            densityScore = 0.8;
        } else if (density >= 3 && density <= 10) {
            densityScore = 0.6;
        } else {
            densityScore = 0.4;
        }
        score += densityScore * 0.35;

        // 1.3 结构复杂度 (25%)
        double complexityScore = Math.min(1.0, f.structureComplexity / 10.0);
        score += complexityScore * 0.25;

        return score;
    }

    /**
     * 2. 可读性评分 (25%)
     * 考虑：段落结构、句子长度、排版
     */
    private double scoreReadability(ContentFeatures f) {
        double score = 0.0;

        // 2.1 段落结构 (40%)
        double paragraphScore = 0.0;
        if (f.paragraphs >= 5 && f.paragraphs <= 30) {
            paragraphScore = 1.0;
        } else if (f.paragraphs >= 3 && f.paragraphs <= 50) {
            paragraphScore = 0.8;
        } else if (f.paragraphs >= 2) {
            paragraphScore = 0.6;
        } else {
            paragraphScore = 0.3; // 单段长文
        }
        score += paragraphScore * 0.40;

        // 2.2 句子长度 (35%) - 15-25词最佳
        double sentenceScore = 0.0;
        if (f.avgSentenceLength >= 15 && f.avgSentenceLength <= 25) {
            sentenceScore = 1.0;
        } else if (f.avgSentenceLength >= 10 && f.avgSentenceLength <= 35) {
            sentenceScore = 0.8;
        } else if (f.avgSentenceLength >= 5 && f.avgSentenceLength <= 45) {
            sentenceScore = 0.6;
        } else {
            sentenceScore = 0.4;
        }
        score += sentenceScore * 0.35;

        // 2.3 句子长度一致性 (25%) - 标准差越小越好
        double consistencyScore = 1.0 - Math.min(1.0, f.sentenceLengthStd / 20.0);
        score += consistencyScore * 0.25;

        return score;
    }

    /**
     * 3. 富媒体评分 (20%)
     * 考虑：代码、图片、链接、表格等
     */
    private double scoreRichness(ContentFeatures f) {
        double score = 0.0;

        // 3.1 标题结构 (25%)
        if (f.headerCount >= 4) score += 0.25;
        else if (f.headerCount >= 2) score += 0.20;
        else if (f.headerCount >= 1) score += 0.10;

        // 3.2 代码内容 (30%)
        if (f.codeBlockCount >= 3) {
            score += 0.30;
        } else if (f.codeBlockCount >= 1) {
            score += 0.20;
            // 代码质量加分
            if (f.avgCodeBlockLength >= 100 && f.avgCodeBlockLength <= 500) {
                score += 0.05;
            }
        } else if (f.inlineCodeCount >= 5) {
            score += 0.10;
        }

        // 3.3 视觉内容 (25%)
        if (f.imageCount >= 3) score += 0.15;
        else if (f.imageCount >= 1) score += 0.10;

        if (f.tableCount >= 1) score += 0.10;

        // 3.4 引用和链接 (20%)
        if (f.linkCount >= 5) score += 0.15;
        else if (f.linkCount >= 2) score += 0.10;
        else if (f.linkCount >= 1) score += 0.05;

        if (f.quoteCount >= 2) score += 0.05;

        return Math.min(1.0, score);
    }

    /**
     * 4. 专业性评分 (15%)
     * 考虑：数据引用、术语使用、逻辑性
     */
    private double scoreProfessionalism(ContentFeatures f) {
        double score = 0.4; // 基础分

        // 4.1 数据支撑 (35%)
        if (f.numberCount >= 10) score += 0.25;
        else if (f.numberCount >= 5) score += 0.20;
        else if (f.numberCount >= 2) score += 0.15;
        else if (f.numberCount >= 1) score += 0.10;

        // 4.2 引用文献 (30%)
        if (f.citationCount >= 5) score += 0.30;
        else if (f.citationCount >= 2) score += 0.20;
        else if (f.citationCount >= 1) score += 0.10;

        // 4.3 列表使用（逻辑清晰）(20%)
        if (f.listItemCount >= 5) score += 0.20;
        else if (f.listItemCount >= 2) score += 0.10;

        // 4.4 词汇专业度 (15%) - 长词占比
        double avgWordLength = f.wordCount > 0 ? (double) f.textLength / f.wordCount : 0;
        if (avgWordLength >= 6) score += 0.15;
        else if (avgWordLength >= 5) score += 0.10;
        else if (avgWordLength >= 4.5) score += 0.05;

        return Math.min(1.0, score);
    }

    /**
     * 5. 原创性评分 (10%)
     * 考虑：词汇多样性、表达独特性
     */
    private double scoreOriginality(ContentFeatures f) {
        double score = 0.0;

        // 5.1 词汇丰富度 (60%)
        double vocabScore = f.vocabularyRichness;
        // 调整：太短的文章不可靠
        if (f.wordCount < 100) {
            vocabScore *= 0.5;
        } else if (f.wordCount < 300) {
            vocabScore *= 0.8;
        }
        score += vocabScore * 0.60;

        // 5.2 重复度惩罚 (40%)
        double repetitionScore = 1.0;
        if (f.topWordRepetition > 20) {
            repetitionScore = 0.4; // 严重重复
        } else if (f.topWordRepetition > 10) {
            repetitionScore = 0.6;
        } else if (f.topWordRepetition > 5) {
            repetitionScore = 0.8;
        }
        score += repetitionScore * 0.40;

        return score;
    }

    // ========== 辅助方法 ==========

    /**
     * 长度评分 - 正态分布曲线
     */
    private double calculateLengthScore(int length) {
        // 最佳长度：1500-3000字
        double optimal = 2200;
        double sigma = 2000;

        double score = Math.exp(-Math.pow(length - optimal, 2) / (2 * sigma * sigma));

        // 过短惩罚更严重
        if (length < 500) {
            score *= 0.5;
        } else if (length < 1000) {
            score *= 0.8;
        }

        return Math.max(0.2, score);
    }

    /**
     * 计算结构复杂度
     */
    private double calculateStructureComplexity(ContentFeatures f) {
        double complexity = 0;
        complexity += f.headerCount * 1.5;
        complexity += f.paragraphs * 0.5;
        complexity += f.listItemCount * 0.3;
        complexity += f.codeBlockCount * 2.0;
        complexity += f.tableCount * 2.0;
        complexity += f.quoteCount * 1.0;
        return complexity;
    }

    /**
     * 计算标准差
     */
    private double calculateStd(List<Integer> values) {
        if (values.isEmpty()) return 0;
        double avg = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average().orElse(0);
        return Math.sqrt(variance);
    }

    /**
     * 提取所有代码块
     */
    private List<String> extractCodeBlocks(String content) {
        List<String> blocks = new ArrayList<>();
        Matcher m = CODE_BLOCK.matcher(content);
        while (m.find()) {
            blocks.add(m.group());
        }
        return blocks;
    }

    /**
     * 统计正则匹配次数
     */
    private int countMatches(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    /**
     * 获取评级
     */
    public String getGrade(double score) {
        if (score >= 0.90) return "S";
        if (score >= 0.80) return "A";
        if (score >= 0.65) return "B";
        if (score >= 0.50) return "C";
        if (score >= 0.35) return "D";
        return "F";
    }

    // ========== 数据类 ==========

    public static class ContentFeatures {
        // 基础统计
        public int rawLength;        // 原始长度
        public int textLength;       // 纯文本长度（去除代码）
        public int lines;
        public int paragraphs;

        // 句子分析
        public int sentenceCount;
        public double avgSentenceLength;
        public double sentenceLengthStd;

        // 词汇分析
        public int wordCount;
        public int uniqueWordCount;
        public double vocabularyRichness;
        public int topWordRepetition;

        // 富媒体
        public int codeBlockCount;
        public int inlineCodeCount;
        public double avgCodeBlockLength;
        public int imageCount;
        public int linkCount;
        public int headerCount;
        public int listItemCount;
        public int quoteCount;
        public int tableCount;

        // 专业性
        public int numberCount;
        public int citationCount;

        // 结构
        public double structureComplexity;
    }

    public record ScoreResult(double totalScore, Map<String, Double> dimensionScores, ContentFeatures features) {

        public String getGrade() {
            return new QualityScorer().getGrade(totalScore);
        }

        @Override
        public String toString() {
            return String.format("Score: %.2f (%s)\n" +
                            "- Depth: %.2f\n" +
                            "- Readability: %.2f\n" +
                            "- Richness: %.2f\n" +
                            "- Professionalism: %.2f\n" +
                            "- Originality: %.2f",
                    totalScore, getGrade(),
                    dimensionScores.get("depth"),
                    dimensionScores.get("readability"),
                    dimensionScores.get("richness"),
                    dimensionScores.get("professionalism"),
                    dimensionScores.get("originality"));
        }
    }
}