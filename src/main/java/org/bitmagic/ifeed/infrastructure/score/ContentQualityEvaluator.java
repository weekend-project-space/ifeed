package org.bitmagic.ifeed.infrastructure.score;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 只基于文章内容本身评估质量，不依赖外部信号。
 * 评分维度：
 * 1. 内容深度 - 长度、信息密度、结构复杂度
 * 2. 可读性 - 段落结构、句子长度、一致性
 * 3. 富媒体 - 代码、图片、视频、链接等
 * 4. 专业性 - 数据引用、术语、逻辑
 * 5. 原创性 - 词汇多样性、重复检测
 * 6. 标题质量 - 长度、吸引力、相关性
 * <p>
 * 基于纯文本内容进行客观、全面的质量评估，不依赖发布时间、作者信息或用户互动数据。
 * 适用于资讯、社区、UGC 等内容平台的质量筛选、推荐排序、创作者激励等场景。
 * <p>
 * 核心特性：
 * - 支持 20 个内容类别自适应权重与阈值
 * - 采用饱和生长曲线评估内容深度，避免惩罚过长文章
 * - 中文原生优化：信息密度阈值自适应中英文比例、汉字丰富度参与原创性计算
 * - 富媒体全面支持：图片、视频、代码、表格、emoji 等多维度奖励
 * - 强原创性检测：词汇多样性 + 词级/字符级 4-gram 重复 + 句子结构一致性
 * - 标题质量独立评估，内置标题党黑名单惩罚
 * - 高性能设计，适合大规模实时评估
 * </p>
 */
public class ContentQualityEvaluator {

    // ==================== 正则表达式（预编译，提升性能） ====================

    private static final Pattern CODE_BLOCK = Pattern.compile("```[\\s\\S]*?```"); // Markdown 代码块
    private static final Pattern INLINE_CODE = Pattern.compile("`[^`]+`"); // 内联代码
    private static final Pattern LINK = Pattern.compile("https?://[^\\s)]+"); // 超链接
    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\([^)]+\\)"); // Markdown 图片
    private static final Pattern HEADER = Pattern.compile("^#{1,6}\\s+.+$", Pattern.MULTILINE); // 标题层级
    private static final Pattern LIST = Pattern.compile("^[*+-]\\s+|^\\d+\\.\\s+", Pattern.MULTILINE); // 无序/有序列表项
    private static final Pattern QUOTE = Pattern.compile("^>\\s+", Pattern.MULTILINE); // 引用块
    private static final Pattern TABLE = Pattern.compile("\\|.+\\|\\n\\|[-:| ]+\\|"); // Markdown 表格
    private static final Pattern NUMBER = Pattern.compile("\\d+\\.\\d+|\\d+%|\\$\\d+"); // 数字、百分比、金额
    private static final Pattern CITATION = Pattern.compile("\\[\\d+\\]|\\[\\^\\d+\\]"); // 文献引用标记
    private static final Pattern EMOJI = Pattern.compile("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"); // Emoji
    private static final Pattern VIDEO = Pattern.compile("https?://(www\\.)?(youtube\\.com|bilibili\\.com|x\\.com|twitter\\.com)/\\S+|<video>.*?</video>"); // 常见视频平台链接或 video 标签

    // ==================== 标题党黑名单（可运营动态扩展） ====================

    private static final Set<String> TITLE_PARTY_BLACKLIST = new HashSet<>(Arrays.asList(
            "震惊", "必看", "爆款", "神器", "惊人", "绝密", "超级", "顶级", "最", "!!", "??",
            "离谱", "逆天", "炸裂", "泪目", "全网怒了"
    ));

    // ==================== 内容类别配置 ====================

    /**
     * 内容类别枚举
     * 每个类别定义：
     * - targetLen：该类别优质内容的预期长度（用于饱和深度模型）
     * - 五个维度权重（总和约1.0，标题质量固定占5%）
     */
    public enum ContentCategory {
        TECH(2500, 0.35, 0.20, 0.15, 0.20, 0.10),
        SCIENCE(3000, 0.40, 0.15, 0.10, 0.30, 0.05),
        DESIGN(2000, 0.35, 0.20, 0.15, 0.20, 0.10),
        EDUCATION(2500, 0.35, 0.20, 0.15, 0.20, 0.10),
        GAMING(1500, 0.35, 0.20, 0.15, 0.20, 0.10),
        CAREER(1800, 0.30, 0.20, 0.15, 0.25, 0.10),
        BUSINESS(1800, 0.30, 0.20, 0.15, 0.25, 0.10),
        FINANCE(2000, 0.30, 0.20, 0.15, 0.25, 0.10),
        AUTO(1500, 0.30, 0.20, 0.15, 0.25, 0.10),
        NEWS(800, 0.20, 0.30, 0.20, 0.15, 0.15),
        SPORTS(1000, 0.20, 0.30, 0.20, 0.15, 0.15),
        OPINION(1500, 0.20, 0.30, 0.20, 0.15, 0.15),
        ENTERTAINMENT(1000, 0.20, 0.30, 0.30, 0.10, 0.10),
        LIFESTYLE(1200, 0.20, 0.30, 0.30, 0.10, 0.10),
        HEALTH(1500, 0.20, 0.30, 0.30, 0.10, 0.10),
        FOOD(800, 0.20, 0.30, 0.30, 0.10, 0.10),
        TRAVEL(2000, 0.20, 0.30, 0.30, 0.10, 0.10),
        CULTURE(2000, 0.20, 0.30, 0.30, 0.10, 0.10),
        FASHION(1200, 0.20, 0.30, 0.30, 0.10, 0.10),
        OTHER(2000, 0.30, 0.25, 0.20, 0.15, 0.10);

        final int targetLen;
        final double wDepth, wReadability, wRichness, wProfessionalism, wOriginality;

        ContentCategory(int targetLen, double wDepth, double wReadability, double wRichness, double wProfessionalism, double wOriginality) {
            this.targetLen = targetLen;
            this.wDepth = wDepth;
            this.wReadability = wReadability;
            this.wRichness = wRichness;
            this.wProfessionalism = wProfessionalism;
            this.wOriginality = wOriginality;
        }
    }

    // ==================== 类别自适应阈值配置 ====================

    private static final Map<ContentCategory, double[]> SENTENCE_LENGTH_THRESHOLDS = new HashMap<>();
    private static final Map<ContentCategory, int[]> PARAGRAPH_THRESHOLDS = new HashMap<>();

    static {
        double[] defaultSentence = {15, 25, 10, 35, 5, 45}; // {最佳min, 最佳max, 良好min, 良好max, 一般min, 一般max}
        int[] defaultParagraph = {5, 30, 3, 50}; // {最佳min, 最佳max, 良好min, 良好max}

        // 技术/知识类：偏长句长段
        List<ContentCategory> techGroup = Arrays.asList(ContentCategory.TECH, ContentCategory.SCIENCE, ContentCategory.DESIGN,
                ContentCategory.EDUCATION, ContentCategory.GAMING, ContentCategory.CAREER);
        techGroup.forEach(cat -> {
            SENTENCE_LENGTH_THRESHOLDS.put(cat, defaultSentence);
            PARAGRAPH_THRESHOLDS.put(cat, defaultParagraph);
        });

        // 商业/财经类
        List<ContentCategory> businessGroup = Arrays.asList(ContentCategory.BUSINESS, ContentCategory.FINANCE, ContentCategory.AUTO);
        businessGroup.forEach(cat -> {
            SENTENCE_LENGTH_THRESHOLDS.put(cat, defaultSentence);
            PARAGRAPH_THRESHOLDS.put(cat, defaultParagraph);
        });

        // 新闻/观点类：偏短句短段
        double[] newsSentence = {10, 20, 8, 30, 5, 40};
        int[] newsParagraph = {3, 20, 2, 30};
        List<ContentCategory> newsGroup = Arrays.asList(ContentCategory.NEWS, ContentCategory.SPORTS, ContentCategory.OPINION);
        newsGroup.forEach(cat -> {
            SENTENCE_LENGTH_THRESHOLDS.put(cat, newsSentence);
            PARAGRAPH_THRESHOLDS.put(cat, newsParagraph);
        });

        // 生活/娱乐类：适度短句短段，利于图文混排
        double[] lifeSentence = {12, 22, 8, 32, 5, 42};
        int[] lifeParagraph = {3, 25, 2, 40};
        List<ContentCategory> lifeGroup = Arrays.asList(ContentCategory.ENTERTAINMENT, ContentCategory.LIFESTYLE, ContentCategory.HEALTH,
                ContentCategory.FOOD, ContentCategory.TRAVEL, ContentCategory.CULTURE, ContentCategory.FASHION);
        lifeGroup.forEach(cat -> {
            SENTENCE_LENGTH_THRESHOLDS.put(cat, lifeSentence);
            PARAGRAPH_THRESHOLDS.put(cat, lifeParagraph);
        });

        // 兜底类别使用默认阈值
        SENTENCE_LENGTH_THRESHOLDS.put(ContentCategory.OTHER, defaultSentence);
        PARAGRAPH_THRESHOLDS.put(ContentCategory.OTHER, defaultParagraph);
    }

    /**
     * 主评估入口
     *
     * @param category 内容所属类别
     * @param title    文章标题（可选，若为 null 或空串则标题质量得分为 0）
     * @param content  正文内容（Markdown 或纯文本均可）
     * @return 评估结果，包含总分、各维度分和提取的特征
     */
    public EvaluationResult evaluate(ContentCategory category, String title, String content) {
        if (content == null || content.trim().isEmpty()) {
            return new EvaluationResult(0.0, new HashMap<>(), null);
        }

        ContentFeatures features = extractFeatures(content);

        Map<String, Double> dimensionScores = new HashMap<>(6);
        dimensionScores.put("depth", evaluateDepth(features, category));
        dimensionScores.put("readability", evaluateReadability(features, category));
        dimensionScores.put("richness", evaluateRichness(features, category));
        dimensionScores.put("professionalism", evaluateProfessionalism(features));
        dimensionScores.put("originality", evaluateOriginality(features, content));
        dimensionScores.put("title_quality", evaluateTitleQuality(title, content, features));

        // 标题质量固定占 5%，从深度和专业性维度各扣除 2.5%
        double totalScore = dimensionScores.get("depth") * (category.wDepth - 0.025)
                + dimensionScores.get("readability") * category.wReadability
                + dimensionScores.get("richness") * category.wRichness
                + dimensionScores.get("professionalism") * (category.wProfessionalism - 0.025)
                + dimensionScores.get("originality") * category.wOriginality
                + dimensionScores.get("title_quality") * 0.05;

        return new EvaluationResult(totalScore, dimensionScores, features);
    }

    /**
     * 提取内容统计特征（核心性能点：尽量减少重复正则匹配和字符串操作）
     */
    private ContentFeatures extractFeatures(String content) {
        ContentFeatures f = new ContentFeatures();

        // 基础长度与结构统计
        f.rawLength = content.length();
        f.lines = content.split("\n").length;
        f.paragraphs = content.split("\n\\s*\n+").length;

        // 去除代码块、图片、链接后的纯文本，用于后续词汇/句子分析
        String textOnly = CODE_BLOCK.matcher(content).replaceAll("");
        textOnly = IMAGE.matcher(textOnly).replaceAll("");
        textOnly = LINK.matcher(textOnly).replaceAll("");
        f.textLength = textOnly.length();

        // 中文比例与汉字丰富度（用于信息密度和原创性自适应）
        long totalChinese = textOnly.chars().filter(c -> c >= 0x4E00 && c <= 0x9FA5).count();
        Set<Integer> uniqueChinese = textOnly.chars()
                .filter(c -> c >= 0x4E00 && c <= 0x9FA5)
                .distinct()
                .boxed()
                .collect(Collectors.toSet());
        double chineseRichness = totalChinese > 0 ? (double) uniqueChinese.size() / totalChinese : 0.0;
        f.chineseRatio = f.textLength > 0 ? (double) totalChinese / f.textLength : 0.0;

        // 句子统计
        String[] sentences = textOnly.split("[.!?。！？；;]+");
        List<Integer> sentLengths = new ArrayList<>();
        for (String s : sentences) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                int words = trimmed.split("[\\s\\p{Punct}]+").length;
                sentLengths.add(words);
            }
        }
        f.sentenceCount = sentLengths.size();
        f.avgSentenceLength = sentLengths.isEmpty() ? 0 : sentLengths.stream().mapToInt(Integer::intValue).average().orElse(0);
        f.sentenceLengthStd = calculateStd(sentLengths);

        // 词汇统计与丰富度
        String[] words = textOnly.toLowerCase()
                .replaceAll("[^a-z0-9\\s\\u4e00-\\u9fa5]", " ")
                .split("\\s+");
        f.wordCount = words.length;
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        uniqueWords.remove("");
        f.uniqueWordCount = uniqueWords.size();
        f.vocabularyRichness = f.wordCount > 0
                ? (double) f.uniqueWordCount / f.wordCount * 0.5 + chineseRichness * 0.5
                : 0.0;

        // 最高词频（用于重复惩罚）
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String w : words) {
            if (w.length() > 3) {
                wordFreq.merge(w, 1, Integer::sum);
            }
        }
        f.topWordRepetition = wordFreq.values().stream().mapToInt(v -> v).max().orElse(0);

        // 富媒体与专业性指标统计
        f.codeBlockCount = countMatches(CODE_BLOCK, content);
        f.inlineCodeCount = countMatches(INLINE_CODE, content);
        f.imageCount = countMatches(IMAGE, content);
        f.linkCount = countMatches(LINK, content);
        f.headerCount = countMatches(HEADER, content);
        f.listItemCount = countMatches(LIST, content);
        f.quoteCount = countMatches(QUOTE, content);
        f.tableCount = countMatches(TABLE, content);
        f.emojiCount = countMatches(EMOJI, content);
        f.videoCount = countMatches(VIDEO, content);
        f.numberCount = countMatches(NUMBER, content);
        f.citationCount = countMatches(CITATION, content);

        // 代码块平均长度（仅在存在代码块时计算）
        if (f.codeBlockCount > 0) {
            f.avgCodeBlockLength = extractCodeBlocks(content).stream()
                    .mapToInt(String::length)
                    .average()
                    .orElse(0);
        }

        // 结构复杂度（加权统计）
        f.structureComplexity = calculateStructureComplexity(f);

        return f;
    }

    /**
     * 评估内容深度维度：长度（饱和模型） + 信息密度（中英文自适应） + 结构复杂度
     */
    private double evaluateDepth(ContentFeatures f, ContentCategory category) {
        double score = 0.0;

        // 饱和生长曲线：长度越接近目标越好，超长不惩罚
        double lengthScore = 1.0 - Math.exp(-(double) f.textLength / category.targetLen * 1.5);
        score += lengthScore * 0.40;

        // 信息密度自适应中文比例
        double density = f.wordCount > 0 ? (double) f.textLength / f.wordCount : 0;
        double densityScore;
        double minBest = 5, maxBest = 7;
        if (f.chineseRatio > 0.9) {
            minBest = 2.0;
            maxBest = 3.5;
        } else if (f.chineseRatio > 0.7) {
            minBest = 2.5;
            maxBest = 4.5;
        }
        if (density >= minBest && density <= maxBest) {
            densityScore = 1.0;
        } else if (density >= minBest - 1.5 && density <= maxBest + 2) {
            densityScore = 0.8;
        } else if (density >= minBest - 3 && density <= maxBest + 4) {
            densityScore = 0.6;
        } else {
            densityScore = 0.4;
        }
        score += densityScore * 0.35;

        // 结构复杂度
        double complexityScore = Math.min(1.0, f.structureComplexity / 10.0);
        score += complexityScore * 0.25;

        return score;
    }

    /**
     * 评估可读性维度：段落结构 + 句子长度（类别自适应） + 句子长度一致性
     */
    private double evaluateReadability(ContentFeatures f, ContentCategory category) {
        double score = 0.0;

        // 段落数量与平均段落长度
        int[] paraThresh = PARAGRAPH_THRESHOLDS.getOrDefault(category, PARAGRAPH_THRESHOLDS.get(ContentCategory.OTHER));
        double paragraphScore;
        if (f.paragraphs >= paraThresh[0] && f.paragraphs <= paraThresh[1]) {
            paragraphScore = 1.0;
        } else if (f.paragraphs >= paraThresh[2] && f.paragraphs <= paraThresh[3]) {
            paragraphScore = 0.8;
        } else if (f.paragraphs >= 2) {
            paragraphScore = 0.6;
        } else {
            paragraphScore = 0.3;
        }
        f.avgParaLen = f.paragraphs > 0 ? (double) f.textLength / f.paragraphs : 0;
        if (f.avgParaLen > 50 && f.avgParaLen < 400) paragraphScore += 0.1;
        score += paragraphScore * 0.40;

        // 句子长度（类别自适应阈值）
        double[] sentThresh = SENTENCE_LENGTH_THRESHOLDS.getOrDefault(category, SENTENCE_LENGTH_THRESHOLDS.get(ContentCategory.OTHER));
        double sentenceScore;
        if (f.avgSentenceLength >= sentThresh[0] && f.avgSentenceLength <= sentThresh[1]) {
            sentenceScore = 1.0;
        } else if (f.avgSentenceLength >= sentThresh[2] && f.avgSentenceLength <= sentThresh[3]) {
            sentenceScore = 0.8;
        } else if (f.avgSentenceLength >= sentThresh[4] && f.avgSentenceLength <= sentThresh[5]) {
            sentenceScore = 0.6;
        } else {
            sentenceScore = 0.4;
        }
        score += sentenceScore * 0.35;

        // 句子长度一致性（标准差越小越好）
        double consistencyScore = 1.0 - Math.min(1.0, f.sentenceLengthStd / 20.0);
        score += consistencyScore * 0.25;

        return score;
    }

    /**
     * 评估富媒体丰富度：标题、代码、图片、视频、表格、emoji、链接、引用等多维度加分
     */
    private double evaluateRichness(ContentFeatures f, ContentCategory category) {
        double score = 0.0;

        // 标题层级
        if (f.headerCount >= 4) score += 0.25;
        else if (f.headerCount >= 2) score += 0.20;
        else if (f.headerCount >= 1) score += 0.10;

        // 代码内容
        if (f.codeBlockCount >= 3) score += 0.30;
        else if (f.codeBlockCount >= 1) {
            score += 0.20;
            if (f.avgCodeBlockLength >= 100 && f.avgCodeBlockLength <= 500) score += 0.05;
        } else if (f.inlineCodeCount >= 5) score += 0.10;

        // 图片与表格
        if (f.imageCount >= 10) score += 0.25;
        else if (f.imageCount >= 5) score += 0.20;
        else if (f.imageCount >= 3) score += 0.15;
        else if (f.imageCount >= 1) score += 0.10;
        if (f.tableCount >= 1) score += 0.10;

        // 视频
        if (f.videoCount >= 3) score += 0.20;
        else if (f.videoCount >= 1) score += 0.10;

        // emoji
        if (f.emojiCount >= 10) score += 0.05;
        else if (f.emojiCount >= 5) score += 0.03;

        // 链接与引用
        if (f.linkCount >= 5) score += 0.15;
        else if (f.linkCount >= 2) score += 0.10;
        else if (f.linkCount >= 1) score += 0.05;
        if (f.quoteCount >= 2) score += 0.05;

        return Math.min(1.0, score);
    }

    /**
     * 评估专业性：数据支撑、文献引用、列表逻辑、词汇专业度
     */
    private double evaluateProfessionalism(ContentFeatures f) {
        double score = 0.4; // 基础分，保证即使无数据也有一定专业感

        // 数字使用
        if (f.numberCount >= 10) score += 0.25;
        else if (f.numberCount >= 5) score += 0.20;
        else if (f.numberCount >= 2) score += 0.15;
        else if (f.numberCount >= 1) score += 0.10;

        // 文献引用
        if (f.citationCount >= 5) score += 0.30;
        else if (f.citationCount >= 2) score += 0.20;
        else if (f.citationCount >= 1) score += 0.10;

        // 列表使用（体现逻辑清晰）
        if (f.listItemCount >= 5) score += 0.20;
        else if (f.listItemCount >= 2) score += 0.10;

        // 平均词长（长词往往更专业）
        double avgWordLength = f.wordCount > 0 ? (double) f.textLength / f.wordCount : 0;
        if (avgWordLength >= 6) score += 0.15;
        else if (avgWordLength >= 5) score += 0.10;
        else if (avgWordLength >= 4.5) score += 0.05;

        return Math.min(1.0, score);
    }

    /**
     * 评估原创性：词汇多样性 + 重复惩罚（词级/字符级4-gram + 句子结构一致性）
     */
    private double evaluateOriginality(ContentFeatures f, String content) {
        double score = 0.0;

        // 词汇丰富度（短文适当打折）
        double vocabScore = f.vocabularyRichness;
        if (f.wordCount < 100) vocabScore *= 0.5;
        else if (f.wordCount < 300) vocabScore *= 0.8;
        score += vocabScore * 0.60;

        // 重复度惩罚
        double repetitionScore = 1.0;
        if (f.topWordRepetition > 20) repetitionScore = 0.4;
        else if (f.topWordRepetition > 10) repetitionScore = 0.6;
        else if (f.topWordRepetition > 5) repetitionScore = 0.8;

        // 词级4-gram重复
        double wordNgramRatio = calculateNGramRepeatRatio(content, 4, true);
        if (wordNgramRatio > 0.05) repetitionScore -= 0.3;
        else if (wordNgramRatio > 0.02) repetitionScore -= 0.15;

        // 字符级4-gram重复（仅在词级已异常时触发，节省性能）
        if (wordNgramRatio > 0.02) {
            double charNgramRatio = calculateNGramRepeatRatio(content, 4, false);
            if (charNgramRatio > 0.05) repetitionScore -= 0.3;
            else if (charNgramRatio > 0.02) repetitionScore -= 0.15;
        }

        // 句子长度过于一致（常见AI生成特征）
        if (f.sentenceLengthStd < 5.0) repetitionScore -= 0.2;

        repetitionScore = Math.max(0.0, repetitionScore);
        score += repetitionScore * 0.40;

        return score;
    }

    /**
     * 评估标题质量：长度、吸引力、内容相关性、标题党惩罚
     */
    private double evaluateTitleQuality(String title, String content, ContentFeatures f) {
        if (title == null || title.trim().isEmpty()) return 0.0;

        double score = 0.5; // 基础分

        // 长度评估
        int len = title.length();
        if (len >= 10 && len <= 35) score += 0.3;
        else if (len >= 6 && len <= 50) score += 0.2;
        else if (len >= 5) score += 0.1;

        // 吸引力元素
        if (NUMBER.matcher(title).find() || title.contains("?") || title.contains("!")) score += 0.2;

        // 与正文相关性
        String[] titleWords = title.toLowerCase().split("\\s+");
        double matchRatio = 0.0;
        for (String tw : titleWords) {
            if (tw.length() > 3 && content.toLowerCase().contains(tw)) {
                matchRatio += 1.0 / titleWords.length;
            }
        }
        score += matchRatio * 0.3;
        if (content.contains(title.substring(0, Math.min(title.length(), 4)))) score += 0.1;

        // 标题党惩罚
        String lowerTitle = title.toLowerCase();
        long blackCount = TITLE_PARTY_BLACKLIST.stream()
                .filter(word -> lowerTitle.contains(word.toLowerCase()))
                .count();
        if (blackCount >= 3) score -= 0.4;
        else if (blackCount >= 1) score -= 0.2;

        return Math.max(0.0, Math.min(1.0, score));
    }

    // ==================== 辅助计算方法 ====================

    /**
     * 计算结构复杂度（加权统计多种结构元素）
     */
    private double calculateStructureComplexity(ContentFeatures f) {
        return f.headerCount * 1.5 +
                f.paragraphs * 0.5 +
                f.listItemCount * 0.3 +
                f.codeBlockCount * 2.0 +
                f.tableCount * 2.0 +
                f.quoteCount * 1.0;
    }

    /**
     * 计算整数列表标准差（用于句子长度一致性评估）
     */
    private double calculateStd(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        double avg = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    /**
     * 提取所有代码块（用于计算平均长度）
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
     * 正则匹配出现次数统计
     */
    private int countMatches(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    /**
     * 计算 n-gram 重复比率（支持词级和字符级）
     */
    private double calculateNGramRepeatRatio(String content, int n, boolean wordMode) {
        if (content.length() < n * 10) return 0.1;

        Map<String, Integer> freq = new HashMap<>();
        if (wordMode) {
            String[] words = content.toLowerCase().split("\\s+");
            if (words.length < n) return 0.0;
            for (int i = 0; i <= words.length - n; i++) {
                String gram = String.join(" ", Arrays.copyOfRange(words, i, i + n));
                freq.merge(gram, 1, Integer::sum);
            }
        } else {
            for (int i = 0; i <= content.length() - n; i++) {
                String gram = content.substring(i, i + n);
                freq.merge(gram, 1, Integer::sum);
            }
        }

        int repeatCount = (int) freq.values().stream().filter(v -> v > 1).count();
        return (double) repeatCount / freq.size();
    }

    /**
     * 根据总分映射质量等级
     */
    public static String getGrade(double score) {
        double normalized = Math.max(0.0, Math.min(1.0, score));
        if (normalized >= 0.93) return "S";
        if (normalized >= 0.83) return "A";
        if (normalized >= 0.73) return "B";
        if (normalized >= 0.63) return "C";
        if (normalized >= 0.35) return "D";
        return "F";
    }

    // ==================== 数据结构 ====================

    /**
     * 内容统计特征（所有评估维度依赖的原始指标）
     */
    public static class ContentFeatures {
        public int rawLength, textLength, lines, paragraphs, sentenceCount, wordCount, uniqueWordCount, topWordRepetition;
        public int codeBlockCount, inlineCodeCount, imageCount, linkCount, headerCount, listItemCount, quoteCount, tableCount, emojiCount, videoCount;
        public int numberCount, citationCount;
        public double avgSentenceLength, sentenceLengthStd, vocabularyRichness, avgCodeBlockLength, structureComplexity, chineseRatio, avgParaLen;
    }

    /**
     * 评估结果（总分 + 各维度分 + 特征）
     */
    public record EvaluationResult(double totalScore, Map<String, Double> dimensionScores, ContentFeatures features) {

        /**
         * 获取质量等级
         */
        public String getGrade() {
            return ContentQualityEvaluator.getGrade(totalScore);
        }

        /**
         * 友好打印结果
         */
        @Override
        public String toString() {
            return String.format("Total Score: %.2f (%s)\n" +
                            "- Depth:         %.2f\n" +
                            "- Readability:   %.2f\n" +
                            "- Richness:      %.2f\n" +
                            "- Professionalism: %.2f\n" +
                            "- Originality:   %.2f\n" +
                            "- Title Quality: %.2f",
                    totalScore, getGrade(),
                    dimensionScores.get("depth"),
                    dimensionScores.get("readability"),
                    dimensionScores.get("richness"),
                    dimensionScores.get("professionalism"),
                    dimensionScores.get("originality"),
                    dimensionScores.getOrDefault("title_quality", 0.0));
        }
    }
}