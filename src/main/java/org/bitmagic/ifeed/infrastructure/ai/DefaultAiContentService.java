package org.bitmagic.ifeed.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
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

    // ==================== 在类顶部新增常量 ====================

    private static final int MIN_TAGS = 3;
    private static final int MAX_TAGS = 7;

    // 轻量停用词表（中英文）
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "is", "are", "was", "were",
            "this", "that", "it", "you", "i", "we", "they", "he", "she", "be", "have", "do", "will", "would",
            "的", "了", "和", "是", "在", "有", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "好", "为", "中", "来", "我", "对", "从", "以", "其", "还", "并", "等", "个", "而", "后", "将", "被", "于", "及", "与", "更", "已", "通过", "可以", "但", "或"
    );

    // 分类关键词（中英文）
    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = Map.of(
            "AI", Set.of("ai", "人工智能", "gpt", "llm", "大模型", "chatgpt", "机器学习", "深度学习", "神经网络"),
            "Technology", Set.of("tech", "技术", "软件", "硬件", "cloud", "云计算", "5g", "iot", "物联网", "编程", "开发", "代码"),
            "Business", Set.of("startup", "创业", "business", "商业", "funding", "融资", "投资", "市场", "公司", "企业"),
            "Security", Set.of("security", "安全", "privacy", "隐私", "cyber", "网络安全", "黑客", "加密", "漏洞", "攻击"),
            "Science", Set.of("science", "科学", "research", "研究", "实验", "物理", "生物", "化学", "发现"),
            "Health", Set.of("health", "健康", "medical", "医学", "疾病", "疫苗", "药物", "医院", "医生"),
            "Finance", Set.of("finance", "金融", "stock", "股票", "crypto", "加密货币", "blockchain", "区块链", "银行"),
            "Politics", Set.of("politics", "政治", "government", "政府", "election", "选举", "政策", "领导人"),
            "Entertainment", Set.of("movie", "电影", "music", "音乐", "celebrity", "明星", "娱乐", "游戏"),
            "Sports", Set.of("sports", "体育", "football", "足球", "basketball", "篮球", "比赛", "奥运")
    );

    // 正则：中文词（2-8个汉字） + 英文词（3+字母）
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]{2,8}");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]{3,}");
    private static final int DEFAULT_SUMMARY_LENGTH = 300;
    private static final String SYSTEM_PROMPT = "You are an RSS article content analysis assistant responsible for generating JSON data containing abstracts (please summarize the main content of this article in concise language, highlighting core points and key information) summary, categories, tags. Format example: {summary:'',tags:[''],category:'string',aiGenerated:true} 中文";
    private static final String USER_PROMPT_TEMPLATE = "Title: %s\n\nContent:\n%s";

    private final AiProviderProperties properties;
    private final ChatModel chatModel;

    @Override
    public AiContent analyze(String title, String content) {
        if (!StringUtils.hasText(content)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Article content cannot be empty");
        }
        log.debug("Analyze article title='{}', contentLength={}", title, content == null ? 0 : content.length());
        if (properties.isEnabled() && StringUtils.hasText(properties.getEndpoint()) && Strings.isNotBlank(content) && content.length() > DEFAULT_SUMMARY_LENGTH * 2) {
            try {
                return callExternalProvider(title, content);
            } catch (Exception ex) {
                log.warn("AI provider call failed, falling back to heuristic summary", ex);
            }
        }
        return fallbackContent(title, content);
    }

    private AiContent callExternalProvider(String title, String content) {
        AiContent result = ChatClient.create(chatModel)
                .prompt(SYSTEM_PROMPT)
                .user(USER_PROMPT_TEMPLATE.formatted(title, content))
                .call()
                .entity(AiContent.class);
        log.debug("AI provider returned summaryLength={} tagsCount={}",
                result.summary() == null ? 0 : result.summary().length(),
                result.tags() == null ? 0 : result.tags().size());
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
        var normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= DEFAULT_SUMMARY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, DEFAULT_SUMMARY_LENGTH) + "...";
    }

    private String guessCategory(String title, String content) {
        String fullText = (title + " " + content).toLowerCase(Locale.ROOT);
        String titleLower = title.toLowerCase(Locale.ROOT);

        Map<String, Integer> scores = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String cat = entry.getKey();
            int score = 0;
            for (String kw : entry.getValue()) {
                String lowerKw = kw.toLowerCase(Locale.ROOT);
                // 标题命中 ×2
                if (titleLower.contains(lowerKw)) {
                    score += 2;
                }
                // 正文出现次数
                int count = fullText.split(Pattern.quote(lowerKw)).length - 1;
                score += count;
            }
            if (score > 0) {
                scores.put(cat, score);
            }
        }

        if (scores.isEmpty()) return "General";

        // 主类别
        String primary = Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey();
        int maxScore = scores.get(primary);
        int threshold = Math.max(2, maxScore / 2); // 至少 2 分，或主类别一半

        // 次类别
        List<String> secondary = scores.entrySet().stream()
                .filter(e -> e.getValue() >= threshold && !e.getKey().equals(primary))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        return primary + (secondary.isEmpty() ? "" : "|" + String.join("|", secondary));
    }

// ==================== 替换 generateTags ====================

    private List<String> generateTags(String content) {
        if (!StringUtils.hasText(content)) return List.of();

        String lower = content.toLowerCase(Locale.ROOT);
        Map<String, Integer> freq = new HashMap<>();

        // 1. 提取中文词
        var cm = CHINESE_PATTERN.matcher(content);
        while (cm.find()) {
            String word = cm.group();
            if (!STOP_WORDS.contains(word)) {
                freq.merge(word, 1, Integer::sum);
            }
        }

        // 2. 提取英文词（简单词形归一：复数 s → 去掉）
        var em = ENGLISH_PATTERN.matcher(lower);
        while (em.find()) {
            String word = em.group();
            if (word.endsWith("s") && word.length() > 4) {
                word = word.substring(0, word.length() - 1); // 简单去 s
            }
            if (!STOP_WORDS.contains(word) && word.length() >= 3) {
                freq.merge(word, 1, Integer::sum);
            }
        }

        // 动态标签数量：每 400 字约 1 个标签，3~7
        int targetCount = Math.min(MAX_TAGS, Math.max(MIN_TAGS, content.length() / 400 + 1));

        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .limit(targetCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

//    public static void main(String[] args) throws JsonProcessingException {
//        ObjectMapper objectMapper = new ObjectMapper();
//       System.out.println(objectMapper.writeValueAsString(new DefaultAiContentService(null, null).fallbackContent("OpenAI 发布 GPT-5 大模型，性能提升 30%", "人工智能领域又迎来突破，OpenAI 最新发布 GPT-5 大模型，训练数据达 10 万亿 token，推理速度提升 30%，支持多模态输入"))); ;
//    }
}
