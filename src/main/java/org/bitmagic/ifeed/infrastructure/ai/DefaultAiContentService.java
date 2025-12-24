package org.bitmagic.ifeed.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.infrastructure.StopWorldHolder;
import org.bitmagic.ifeed.infrastructure.TermUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAiContentService implements AiContentService {

    // ==================== 常量定义 ====================
    private static final int MIN_TAGS = 3;
    private static final int MAX_TAGS = 7;
    private static final int DEFAULT_SUMMARY_LENGTH = 300;
    private static final int EXTERNAL_AI_LENGTH_THRESHOLD = DEFAULT_SUMMARY_LENGTH * 2;
    private static final int TAG_GENERATION_RATIO = 400;
    private static final int TITLE_KEYWORD_WEIGHT = 2;
    private static final int KEYWORD_BUFFER_MULTIPLIER = 2;
    private static final int MIN_CATEGORY_CONFIDENCE = 3; // 新增：最低置信分数阈值

    // ==================== 正则表达式预编译 ====================
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^\\w\\u4e00-\\u9fa5]+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    // ==================== 系统提示词 ====================
    private static final String SYSTEM_PROMPT = "You are an RSS article content analysis assistant responsible for generating JSON data containing abstracts (please summarize the main content of this article in concise language, highlighting core points and key information) summary, categories, tags. Format example: {summary:'',tags:[''],category:'string',aiGenerated:true} 中文";
    private static final String USER_PROMPT_TEMPLATE = "Title: %s\n\nContent:\n%s";

    // ==================== 停用词表 ====================
    private static final Set<String> STOP_WORDS = StopWorldHolder.STOP_WORDS;

    private final AiProviderProperties properties;

    private final ChatClient chatClient;

    // ==================== 分类关键词表（严格对应 20 个标准分类） ====================
    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = buildCategoryKeywords();

    private static Map<String, Set<String>> buildCategoryKeywords() {
        Map<String, Set<String>> keywords = new LinkedHashMap<>();

        keywords.put("TECH", Set.of(
                "tech", "technology", "software", "hardware", "code", "programming", "developer",
                "framework", "api", "database", "server", "cloud", "devops", "frontend", "backend",
                "app", "mobile", "web", "system", "architecture", "tool", "开源",
                "开发", "技术", "编程", "软件", "硬件", "代码", "框架", "服务", "系统",
                "应用", "平台", "架构", "设计", "工具", "前端", "后端", "部署"
        ));

        keywords.put("SCIENCE", Set.of(
                "science", "research", "study", "experiment", "discovery", "scientific",
                "physics", "biology", "chemistry", "astronomy", "genetics", "paper",
                "科学", "研究", "实验", "发现", "物理", "生物", "化学", "天文", "基因", "论文"
        ));

        keywords.put("DESIGN", Set.of(
                "design", "ui", "ux", "designer", "visual", "interface", "prototype",
                "figma", "sketch", "adobe", "creative", "art direction",
                "设计", "UI", "UX", "设计师", "视觉", "界面", "原型", "交互", "创意"
        ));

        keywords.put("EDUCATION", Set.of(
                "education", "school", "university", "college", "student", "teacher",
                "course", "learning", "training", "mooc", "edtech", "考试", "升学",
                "教育", "学校", "大学", "学生", "老师", "课程", "在线教育", "培训", "学习"
        ));

        keywords.put("GAMING", Set.of(
                "game", "gaming", "esports", "gamer", "rpg", "fps", "moba", "console",
                "pc gaming", "mobile game", "streamer", "游戏", "电竞", "玩家", "主播"
        ));

        keywords.put("CAREER", Set.of(
                "career", "job", "work", "resume", "interview", "salary", "promotion",
                "workplace", "management", "leadership", "职场", "求职", "面试", "简历",
                "职业", "管理", "领导力", "加薪", "跳槽"
        ));

        keywords.put("BUSINESS", Set.of(
                "business", "startup", "entrepreneur", "company", "product", "marketing",
                "strategy", "growth", "创业", "企业", "公司", "产品", "运营", "营销", "商业"
        ));

        keywords.put("FINANCE", Set.of(
                "finance", "stock", "investment", "fund", "economy", "market", "trading",
                "crypto", "bitcoin", "金融", "股票", "投资", "基金", "经济", "理财", "加密货币"
        ));

        keywords.put("AUTO", Set.of(
                "car", "automotive", "vehicle", "ev", "electric car", "tesla", "autonomous",
                "汽车", "新能源", "电动车", "自动驾驶", "车型", "试驾"
        ));

        keywords.put("NEWS", Set.of(
                "news", "breaking", "latest", "report", "event", "announce", "statement",
                "新闻", "报道", "快讯", "突发", "头条", "事件", "宣布", "声明"
        ));

        keywords.put("SPORTS", Set.of(
                "sports", "football", "basketball", "soccer", "tennis", "olympics",
                "match", "athlete", "体育", "足球", "篮球", "比赛", "运动员", "奥运"
        ));

        keywords.put("OPINION", Set.of(
                "opinion", "commentary", "analysis", "viewpoint", "perspective",
                "editorial", "column", "观点", "评论", "专栏", "分析", "观察", "解读"
        ));

        keywords.put("ENTERTAINMENT", Set.of(
                "movie", "film", "tv", "series", "actor", "music", "celebrity",
                "entertainment", "娱乐", "电影", "电视剧", "演员", "音乐", "明星"
        ));

        keywords.put("LIFESTYLE", Set.of(
                "lifestyle", "life", "daily", "home", "living", "wellness",
                "生活方式", "生活", "日常", "居家", "品质生活", "养生"
        ));

        keywords.put("HEALTH", Set.of(
                "health", "medical", "disease", "vaccine", "fitness", "nutrition",
                "健康", "医疗", "疾病", "健身", "营养", "饮食", "养生"
        ));

        keywords.put("FOOD", Set.of(
                "food", "recipe", "cooking", "cuisine", "restaurant", "dish",
                "美食", "食谱", "烹饪", "餐厅", "菜肴", "食材"
        ));

        keywords.put("TRAVEL", Set.of(
                "travel", "tourism", "destination", "trip", "hotel", "guide",
                "旅游", "旅行", "景点", "攻略", "酒店", "度假"
        ));

        keywords.put("CULTURE", Set.of(
                "culture", "art", "museum", "heritage", "tradition", "history",
                "文化", "艺术", "展览", "传统", "历史", "人文"
        ));

        keywords.put("FASHION", Set.of(
                "fashion", "style", "clothing", "beauty", "makeup", "trend",
                "时尚", "穿搭", "服装", "美妆", "潮流", "品牌"
        ));

        keywords.put("OTHER", Set.of(
                "general", "other", "misc", "various", "综合", "其他", "杂谈"
        ));

        return Collections.unmodifiableMap(keywords);
    }


    @Override
    public AiContent analyze(String title, String content) {
        validateContent(content);

        log.debug("Analyzing article: title='{}', contentLength={}", title, content.length());

        if (shouldUseExternalAI(content)) {
            try {
                AiContent aiResult = callExternalProvider(title, content);
                // 新增：标准化外部AI返回的分类，确保符合20个标准分类
                String standardizedCategory = standardizeCategory(aiResult.category());
                return new AiContent(aiResult.summary(), standardizedCategory, aiResult.tags(), true);
            } catch (RuntimeException ex) {
                log.warn("AI provider unavailable, using fallback: {}", ex.getMessage(), ex);
            }
        }
        return fallbackContent(title, content);
    }

    private void validateContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Article content cannot be empty");
        }
    }

    private boolean shouldUseExternalAI(String content) {
        return false;
//        return properties.isEnabled()
//                && StringUtils.hasText(properties.getBaseUrl())
//                && content.length() > EXTERNAL_AI_LENGTH_THRESHOLD;
    }

    private AiContent callExternalProvider(String title, String content) {
        ChatClient client = this.chatClient;
        if (client == null) {
            log.warn("ChatClient not initialized, using fallback");
            return fallbackContent(title, content);
        }

        AiContent result = client
                .prompt(SYSTEM_PROMPT)
                .user(USER_PROMPT_TEMPLATE.formatted(title, content))
                .call()
                .entity(AiContent.class);

        int summaryLen = result.summary() != null ? result.summary().length() : 0;
        int tagsCount = result.tags() != null ? result.tags().size() : 0;
        log.debug("AI provider returned: summaryLength={}, tagsCount={}", summaryLen, tagsCount);

        return result;
    }

    private AiContent fallbackContent(String title, String content) {
        log.debug("Using fallback heuristic summary for title='{}'", title);
        return new AiContent(
                generateSummary(content),
                guessCategory(title, content),
                generateTags(content),
                false
        );
    }

    private String generateSummary(String content) {
        String normalized = WHITESPACE_PATTERN.matcher(content).replaceAll(" ").trim();
        if (normalized.length() <= DEFAULT_SUMMARY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, DEFAULT_SUMMARY_LENGTH) + "...";
    }

    /**
     * 分类猜测：返回单一主分类（严格对应 20 个标准分类）
     */
    private String guessCategory(String title, String content) {
        String fullText = (title + " " + content).toLowerCase(Locale.ROOT);
        String titleLower = title.toLowerCase(Locale.ROOT);

        Map<String, Integer> scores = calculateCategoryScores(titleLower, fullText);

        if (scores.isEmpty() || Collections.max(scores.values()) < MIN_CATEGORY_CONFIDENCE) {
            return "OTHER";
        }

        // 返回得分最高的单一分类
        return Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private Map<String, Integer> calculateCategoryScores(String titleLower, String fullText) {
        Map<String, Integer> scores = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = calculateScore(titleLower, fullText, entry.getValue());
            if (score > 0) {
                scores.put(entry.getKey(), score);
            }
        }

        return scores;
    }

    private int calculateScore(String titleLower, String fullText, Set<String> keywords) {
        int score = 0;

        for (String keyword : keywords) {
            String lowerKw = keyword.toLowerCase(Locale.ROOT);

            // 标题命中加权
            if (titleLower.contains(lowerKw)) {
                score += TITLE_KEYWORD_WEIGHT;
            }

            // 正文出现次数计数
            score += countOccurrences(fullText, lowerKw);
        }

        return score;
    }

    /**
     * 高效字符串出现次数计数（避免 split 产生大量临时对象）
     */
    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    /**
     * 新增：标准化外部AI返回的分类，确保符合标准20个分类
     */
    private String standardizeCategory(String aiCategory) {
        if (!StringUtils.hasText(aiCategory)) {
            return "OTHER";
        }

        String normalized = aiCategory.trim().toUpperCase(Locale.ROOT);

        // 直接匹配标准分类
        if (CATEGORY_KEYWORDS.containsKey(normalized)) {
            return normalized;
        }

        // 模糊匹配（例如 "Technology News" -> "TECH"）
        String fullTextMock = normalized.toLowerCase(Locale.ROOT);
        Map<String, Integer> mockScores = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue()) {
                if (fullTextMock.contains(kw.toLowerCase(Locale.ROOT))) {
                    score += 1;
                }
            }
            if (score > 0) {
                mockScores.put(entry.getKey(), score);
            }
        }

        if (mockScores.isEmpty() || Collections.max(mockScores.values()) < MIN_CATEGORY_CONFIDENCE) {
            return "OTHER";
        }

        return Collections.max(mockScores.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private List<String> generateTags(String content) {
        return generateTagsWithTerm(content);
    }

    private List<String> generateTagsWithTerm(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }

        try {
            String cleaned = CLEAN_PATTERN.matcher(content).replaceAll(" ");
            int target = calculateTargetTagCount(content.length());

            List<String> keywords = TermUtils.keywords(cleaned,
                    target * KEYWORD_BUFFER_MULTIPLIER);

            return keywords.stream()
                    .filter(this::isValidTag)
                    .distinct() // 新增：去重
                    .limit(target)
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.error("HanLP extraction failed, returning empty tags", ex);
            return List.of();
        }
    }

    private int calculateTargetTagCount(int contentLength) {
        int calculated = contentLength / TAG_GENERATION_RATIO + 1;
        return Math.min(MAX_TAGS, Math.max(MIN_TAGS, calculated));
    }

    private boolean isValidTag(String word) {
        return word.length() >= 2
                && !STOP_WORDS.contains(word.toLowerCase(Locale.ROOT))
                && !word.matches("\\d+"); // 新增：过滤纯数字标签
    }

}