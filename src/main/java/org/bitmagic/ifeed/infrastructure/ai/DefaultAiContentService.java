package org.bitmagic.ifeed.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hankcs.hanlp.HanLP;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAiContentService implements AiContentService {


    private static final int MIN_TAGS = 3;
    private static final int MAX_TAGS = 7;

    // 轻量停用词表（中英文）
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "is", "are", "was", "were",
            "this", "that", "it", "you", "i", "we", "they", "he", "she", "be", "have", "do", "will", "would",
            "目前", "根据", "报道", "表示", "认为", "进行", "开展", "实现", "推进", "方面", "以及",
            "其中", "包括", "例如", "相关", "主要", "已经", "能够", "可能", "需要", "应该",
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

    private static final int DEFAULT_SUMMARY_LENGTH = 300;
    private static final String SYSTEM_PROMPT = "You are an RSS article content analysis assistant responsible for generating JSON data containing abstracts (please summarize the main content of this article in concise language, highlighting core points and key information) summary, categories, tags. Format example: {summary:'',tags:[''],category:'string',aiGenerated:true} 中文";
    private static final String USER_PROMPT_TEMPLATE = "Title: %s\n\nContent:\n%s";

    private final AiProviderProperties properties;
    private final ChatClient chatClient;

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
        AiContent result = chatClient
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


    private List<String> generateTags(String content) {
        return generateTagsWithHanLP(content);
    }

    private List<String> generateTagsWithHanLP(String content) {
        if (!StringUtils.hasText(content)) return List.of();

        String cleaned = content.replaceAll("[^\\w\\u4e00-\\u9fa5]+", " ");
        int target = Math.min(MAX_TAGS, Math.max(MIN_TAGS, content.length() / 400 + 1));

        List<String> keywords = HanLP.extractKeyword(cleaned, target * 2);

        return keywords.stream()
                .filter(word -> word.length() >= 2 && !STOP_WORDS.contains(word))
                .limit(target)
                .toList();
    }

    public static void main(String[] args) throws JsonProcessingException {
      System.out.println(new ObjectMapper().writeValueAsString(new DefaultAiContentService(null,null).fallbackContent("KPI 是解药也是毒药", "KPI 需要量化，量化的数字越具体，行动点越明确，那么事情做起来目标感就会越强。这也带来了另外一个问题，大家会把思考都聚焦在这个数字上，去想其他东西的时间变得越来越少。想在 KPI 的文化下把创新做好，是比较有挑战的事情，主要依靠那些除了能够把 KPI 做好，还有余力去思考更多价值的人。\n" +
              "\n" +
              "OKR 的作用在于凝聚共识，在上下对焦的过程中，想清楚如何为用户创造价值，不断明确 Objectives，确定思路，形成取舍。再回头看看 KPI，它并不直接关心用户价值这件事情，虽然完成 KPI 上的指标可以一定程度实现用户价值，但它强调的是组织「要什么」，而不是「要怎么做」，更不会回答为什么要这么做。\n" +
              "\n" +
              "无论是 KPI 还是 OKR 都只是公司和团队管理的一种工具，既然是工具，就有可以改造和升级的地方。如果你看到有人把 OKR 用的像 KPI，或者把 KPI 用出了 OKR 的味道，也不用惊讶，没有人要求一定要按照书本上的方式玩这些工具。\n" +
              "\n" +
              "在原文中打开\n")));  ;
    }

}
