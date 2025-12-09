package org.bitmagic.ifeed.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.bitmagic.ifeed.exception.ApiException;
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
    private static final int MIN_CATEGORY_SCORE = 2;
    private static final int CATEGORY_SCORE_HALF_WEIGHT = 2;
    private static final int TITLE_KEYWORD_WEIGHT = 2;
    private static final int KEYWORD_BUFFER_MULTIPLIER = 2;
    private static final int MIN_CATEGORY_THRESHOLD = 0;
    private static final int MAX_SECONDARY_CATEGORIES = 1;
    // ==================== 正则表达式预编译 ====================
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^\\w\\u4e00-\\u9fa5]+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    // ==================== 系统提示词 ====================
    private static final String SYSTEM_PROMPT = "You are an RSS article content analysis assistant responsible for generating JSON data containing abstracts (please summarize the main content of this article in concise language, highlighting core points and key information) summary, categories, tags. Format example: {summary:'',tags:[''],category:'string',aiGenerated:true} 中文";
    private static final String USER_PROMPT_TEMPLATE = "Title: %s\n\nContent:\n%s";

    // ==================== 停用词表 ====================
    private static final Set<String> STOP_WORDS = buildStopWords();

    private static Set<String> buildStopWords() {
        Set<String> words = new HashSet<>();

        // === 英文通用停用词 ===
        words.addAll(List.of(
                // 冠词
                "the", "a", "an",
                // 连接词
                "and", "or", "but", "yet", "so", "because", "since", "although", "though",
                "whereas", "while", "if", "unless", "until", "before", "after",
                // 代词
                "this", "that", "it", "you", "i", "we", "they", "he", "she", "me", "us", "him", "her", "them",
                // 被动/助动词
                "is", "are", "was", "were", "be", "been", "being",
                "have", "has", "had", "do", "does", "did",
                "will", "would", "should", "could", "may", "might", "must", "can",
                // 介词
                "in", "on", "at", "to", "for", "of", "with", "from", "by", "as", "about", "into",
                "through", "during", "before", "after", "above", "below", "under", "between",
                "among", "across", "around", "over", "through", "without", "within",
                // 高频词汇
                "that", "which", "who", "what", "when", "where", "why", "how",
                "all", "each", "every", "both", "either", "neither",
                "any", "some", "many", "much", "more", "most", "less", "least",
                "such", "so", "very", "really", "quite", "rather", "fairly", "pretty",
                "just", "only", "even", "also", "too", "as well",
                "new", "old", "same", "different", "similar", "other", "another",
                "first", "second", "last", "next", "other",
                // 情态/过渡动词
                "seems", "appears", "looks", "feels", "sounds", "becomes", "turned", "proved",
                "according", "say", "said", "says", "report", "reports", "reported"
        ));

        // === 中文通用停用词 ===
        words.addAll(List.of(
                // 代词和疑问词
                "的", "了", "和", "是", "在", "有", "就", "不", "人", "都", "一", "一个", "上", "也", "很",
                "到", "说", "要", "去", "好", "为", "中", "来", "我", "对", "从", "以", "其", "还", "并",
                "等", "个", "而", "后", "将", "被", "于", "及", "与", "更", "已", "通过", "可以", "但",
                "谁", "什么", "哪个", "哪里", "哪些", "如何", "为什么", "怎样", "怎么", "多少", "多久", "几个",
                "某个", "某些", "彼此",
                // 时间相关词
                "今年", "去年", "明年", "今月", "上月", "下月", "昨天", "今天", "明天",
                "早上", "上午", "中午", "下午", "晚上", "夜间", "白天", "当时", "当日", "日前",
                "前日", "翌日", "次日", "近日", "最近", "随后", "之后", "之前", "期间",
                "一直", "总是", "经常", "常常", "有时", "偶尔", "从不", "从来",
                // 引述和新闻用语
                "称", "表示", "据", "指出", "宣布", "透露", "声称", "显示", "提到", "强调",
                "认为", "觉得", "似乎", "看起来", "显然", "看来", "比如", "例如", "说来", "讲到",
                // 介词和方位词
                "里", "里面", "里边", "上面", "下面", "前面", "后面", "里头", "头上", "地上",
                "天下", "左边", "右边", "中间", "两边", "周围", "附近", "旁边", "外面", "当中",
                "之间", "之中", "之外",
                // 数字量词（基础）
                "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "百", "千", "万",
                "个", "只", "件", "种", "次", "回", "天", "年", "月", "周", "小时", "分钟", "秒",
                "米", "厘米", "毫米", "克", "千克", "斤", "两", "升", "毫升",
                // 语气词和虚词
                "呢", "吗", "吧", "啦", "哈", "嘛", "哪", "呀", "啊", "哦", "喔",
                "而已", "罢了", "似的", "样", "般", "左右", "上下", "前后",
                "因此", "所以", "既然", "要不然", "否则", "另外", "反而", "反正", "总之",
                "总的来说", "一般来说", "通常", "往往", "好像", "恐怕", "大概",
                // 网络用语和社交词汇
                "转发", "转载", "评论", "点赞", "分享", "截图", "投票", "回复",
                "关注", "取关", "粉丝", "博主", "主播", "网友", "用户", "大V",
                "微博", "抖音", "小红书", "B站", "哔哩哔哩",
                // 新闻媒体用语
                "本报", "本网", "本站", "编者按", "记者", "通讯员", "小编",
                "报道", "采访", "采集", "调查", "分析", "评论", "观点", "声音", "话题",
                "热点", "焦点", "聚焦", "关注", "跟踪", "深度", "独家", "发布", "公布",
                // 其他常用虚词
                "需要", "能够", "可能", "应该", "方面", "根据", "报道", "进行", "开展",
                "实现", "推进", "以及", "其中", "包括", "相关", "主要", "已经"
        ));

        return Collections.unmodifiableSet(words);
    }

    // ==================== 分类关键词表 ====================
    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = buildCategoryKeywords();

    private static Map<String, Set<String>> buildCategoryKeywords() {
        Map<String, Set<String>> keywords = new LinkedHashMap<>();
        // 第一阶段：核心必须分类（15个）
        keywords.put("News", Set.of(
                "news", "breaking", "breaking news", "latest", "alert", "报道", "新闻", "快讯",
                "突发", "实时", "头条", "热点", "事件", "发生", "发出", "宣布",
                "声明", "公告", "通告", "消息", "讯息", "资讯"
        ));

        keywords.put("Technology", Set.of(
                "tech", "technology", "software", "hardware", "code", "programming", "framework",
                "api", "database", "server", "network", "cloud", "devops", "开发", "技术",
                "编程", "软件", "硬件", "代码", "框架", "服务", "系统", "应用", "平台",
                "架构", "设计", "工具", "界面"
        ));

        keywords.put("AI", Set.of(
                "ai", "artificial intelligence", "gpt", "llm", "large language model", "chatgpt",
                "machine learning", "deep learning", "neural network", "transformer", "bert",
                "llamat", "claude", "gemini", "text-to-image", "diffusion",
                "prompt", "fine-tuning", "rag", "agent",
                "人工智能", "大模型", "生成式", "机器学习", "深度学习", "神经网络",
                "自然语言", "计算机视觉", "强化学习", "预训练", "微调", "提示词"
        ));

        keywords.put("Business", Set.of(
                "startup", "entrepreneurship", "entrepreneur", "innovation",
                "product", "marketing", "sales", "growth", "strategy", "business model",
                "venture", "accelerator", "incubator", "创业", "创新", "企业", "公司",
                "产品", "运营", "营销", "市场", "销售", "增长", "战略", "商业模式",
                "融资轮", "融资", "投资方", "孵化器", "加速器", "创业者"
        ));

        keywords.put("Finance", Set.of(
                "finance", "financial", "stock", "stocks", "crypto", "cryptocurrency", "blockchain",
                "bitcoin", "ethereum", "trading", "investment", "portfolio", "fund", "banking",
                "insurance", "economic", "economy", "fiscal", "monetary",
                "金融", "股票", "加密货币", "区块链", "交易", "投资", "基金", "银行",
                "保险", "经济", "财务", "理财", "收益", "分红", "涨跌", "涨停", "跌停"
        ));

        keywords.put("Security", Set.of(
                "security", "cyber", "cybersecurity", "privacy", "encryption", "vulnerability",
                "exploit", "hack", "hacking", "malware", "virus", "breach", "attack",
                "defense", "protection", "threat", "安全", "隐私", "网络安全", "黑客",
                "加密", "漏洞", "攻击", "病毒", "恶意", "数据泄露", "防护", "防卫"
        ));

        keywords.put("Science", Set.of(
                "science", "research", "study", "experiment", "discovery", "scientific",
                "physics", "biology", "chemistry", "astronomy", "genetics", "research paper",
                "科学", "研究", "实验", "发现", "物理", "生物", "化学", "天文",
                "基因", "遗传", "论文", "学术", "期刊", "科研"
        ));

        keywords.put("Health", Set.of(
                "health", "healthcare", "medical", "medicine", "disease", "vaccine", "drug",
                "hospital", "doctor", "patient", "treatment", "therapy", "fitness", "wellness",
                "nutrition", "diet", "exercise", "健康", "医学", "医疗", "疾病", "疫苗",
                "药物", "医院", "医生", "患者", "治疗", "康复", "健身", "营养", "饮食"
        ));

        keywords.put("Sports", Set.of(
                "sports", "football", "basketball", "baseball", "soccer", "tennis", "rugby",
                "olympic", "olympics", "competition", "tournament", "match", "game", "athlete",
                "体育", "足球", "篮球", "棒球", "网球", "橄榄球", "乒乓球", "羽毛球",
                "比赛", "竞技", "运动员", "奥运", "冠军", "联赛", "赛事"
        ));

        keywords.put("Politics", Set.of(
                "politics", "political", "government", "politician", "election", "voting",
                "parliament", "congress", "law", "policy", "legislation", "diplomat",
                "政治", "政府", "政策", "选举", "议会", "立法", "法案", "官员",
                "外交", "国务", "人大", "政协", "部门", "官方"
        ));

        keywords.put("Entertainment", Set.of(
                "movie", "film", "cinema", "actor", "actress", "director", "music", "song",
                "album", "musician", "artist", "concert", "performance", "show", "entertainment",
                "电影", "影视", "演员", "导演", "制作", "音乐", "歌曲", "专辑",
                "歌手", "艺术家", "演唱会", "表演", "相声", "小品"
        ));

        keywords.put("Culture", Set.of(
                "culture", "art", "artist", "arts", "design", "designer", "architecture",
                "exhibit", "museum", "gallery", "creative", "creativity", "painting", "sculpture",
                "文化", "艺术", "设计", "建筑", "展览", "美术", "摄影", "绘画",
                "雕塑", "创意", "工艺", "手工", "艺术家", "创作"
        ));

        // 第二阶段：常见补充分类（10个）
        keywords.put("Lifestyle", Set.of(
                "lifestyle", "living", "home", "interior", "design", "decoration", "diy",
                "hack", "tips", "advice", "wellness", "life", "daily", "personal",
                "生活方式", "居家", "装修", "室内设计", "家居", "生活技巧", "建议",
                "品味", "生活", "日常", "个人", "改造", "布置"
        ));

        keywords.put("Travel", Set.of(
                "travel", "tourism", "tourist", "destination", "hotel", "lodging", "booking",
                "airline", "flight", "airport", "guide", "trip", "vacation", "adventure",
                "旅游", "旅行", "度假", "酒店", "民宿", "航班", "目的地", "景点",
                "攻略", "游记", "路线", "机票", "住宿", "旅游指南"
        ));

        keywords.put("Food", Set.of(
                "food", "recipe", "cooking", "cuisine", "restaurant", "chef", "dish",
                "meal", "dessert", "beverage", "drink", "wine", "coffee", "taste",
                "美食", "烹饪", "食谱", "餐厅", "厨师", "菜肴", "料理", "饮品",
                "甜品", "咖啡", "葡萄酒", "美酒", "品尝", "食材"
        ));

        keywords.put("Fashion", Set.of(
                "fashion", "style", "outfit", "clothing", "apparel", "wear", "dress",
                "designer", "brand", "luxury", "beauty", "makeup", "skincare", "cosmetics",
                "时尚", "穿搭", "服装", "衣服", "设计师", "品牌", "奢侈品", "美妆",
                "护肤", "化妆品", "美容", "时装周", "潮流", "风格"
        ));

        keywords.put("Real Estate", Set.of(
                "real estate", "property", "housing", "apartment", "house", "condo",
                "rent", "rental", "lease", "property price", "housing market", "developer",
                "房产", "房地产", "楼盘", "房价", "公寓", "住宅", "租赁", "出租",
                "房源", "开发商", "置业", "买房", "售房", "小区"
        ));

        keywords.put("Automotive", Set.of(
                "automotive", "car", "vehicle", "automobile", "electric vehicle", "ev", "autonomous",
                "self-driving", "tesla", "battery", "engine", "transport", "driving",
                "汽车", "新能源", "电动车", "自动驾驶", "充电", "电池", "车型",
                "试驾", "维修", "保养", "驾驶", "汽车产业", "出行"
        ));

        keywords.put("Education", Set.of(
                "education", "school", "university", "college", "student", "teacher",
                "course", "online course", "learning", "training", "mooc", "edtech",
                "教育", "学校", "大学", "学生", "老师", "课程", "在线教育", "培训",
                "学习", "教学", "考试", "排名", "scholarship", "升学"
        ));

        keywords.put("Gaming", Set.of(
                "game", "gaming", "video game", "esports", "gamer", "rpg", "fps", "moba",
                "console", "pc gaming", "mobile gaming", "tournament", "streamer",
                "游戏", "电竞", "游戏产业", "职业选手", "主播", "直播", "竞技",
                "游戏厂商", "游戏发布", "游戏评测", "玩家"
        ));

        keywords.put("Environment", Set.of(
                "environment", "environmental", "climate", "climate change", "green",
                "sustainability", "sustainable", "renewable", "energy", "carbon", "esg",
                "pollution", "ecology", "环保", "环境", "气候变化", "碳中和",
                "绿色能源", "可持续", "生态", "污染", "ESG", "绿色"
        ));

        // 第三阶段：垂直和扩展分类（其他）
        keywords.put("Social Media", Set.of(
                "social media", "social network", "platform", "content creator", "influencer",
                "trending", "viral", "instagram", "twitter", "tiktok", "youtube", "facebook",
                "live streaming", "short video", "community",
                "社交", "社交媒体", "平台", "内容创作", "博主", "网红", "粉丝经济",
                "直播", "短视频", "话题", "热搜", "抖音", "小红书", "B站", "微博"
        ));

        keywords.put("Law", Set.of(
                "law", "legal", "lawyer", "court", "judge", "case", "lawsuit", "legislation",
                "verdict", "trial", "contract", "legal advice", "regulation",
                "法律", "法律事务", "律师", "法院", "判决", "诉讼", "合同",
                "法案", "条款", "司法", "法规", "法治"
        ));

        keywords.put("Cryptocurrency", Set.of(
                "cryptocurrency", "crypto", "bitcoin", "ethereum", "blockchain", "defi",
                "nft", "token", "wallet", "exchange", "trading", "mining", "staking",
                "加密货币", "区块链", "比特币", "以太坊", "DeFi", "NFT", "钱包",
                "交易所", "挖矿", "质押", "智能合约"
        ));

        keywords.put("E-Commerce", Set.of(
                "e-commerce", "online shopping", "marketplace", "retail", "seller", "buyer",
                "shopping", "purchase", "order", "logistics", "delivery", "payment",
                "电商", "在线购物", "电商平台", "卖家", "买家", "商城", "零售",
                "快递", "配送", "支付", "退货", "售后"
        ));

        keywords.put("Opinion", Set.of(
                "opinion", "editorial", "column", "commentary", "analysis", "perspective",
                "viewpoint", "discuss", "debate", "argument", "thought", "insights",
                "观点", "评论", "专栏", "评论家", "评论员", "分析", "议论", "讨论",
                "思考", "观察", "解读", "见解", "深度分析"
        ));

        keywords.put("Literature", Set.of(
                "literature", "novel", "fiction", "author", "book", "poetry", "poem",
                "writing", "writer", "published", "manuscript", "story", "tale",
                "文学", "小说", "网文", "作者", "书籍", "诗歌", "创意", "写作",
                "文章", "连载", "出版", "文创", "同人"
        ));

        keywords.put("Animation", Set.of(
                "anime", "animation", "manga", "comic", "cartoon", "animated",
                "series", "episode", "character", "fan", "otaku", "cosplay",
                "动画", "漫画", "番剧", "二次元", "ACG", "同好", "Cosplay",
                "人物", "剧集", "IP改编", "衍生品"
        ));

        keywords.put("Energy", Set.of(
                "energy", "power", "electricity", "renewable", "solar", "wind",
                "nuclear", "fossil fuel", "oil", "gas", "coal", "grid", "smart grid",
                "能源", "电力", "电能", "太阳能", "风能", "核能", "油气",
                "新能源", "电网", "光伏", "风电", "储能"
        ));

        keywords.put("Manufacturing", Set.of(
                "manufacturing", "factory", "production", "supply chain", "industrial",
                "industry", "maker", "fabrication", "automation", "robotics", "quality",
                "制造", "工业", "工厂", "生产", "产业链", "供应链", "自动化",
                "机器人", "质量", "产能", "制造业", "工业制造"
        ));

        return Collections.unmodifiableMap(keywords);
    }

    private final AiProviderProperties properties;
    private final ChatClient chatClient;

    @Override
    public AiContent analyze(String title, String content) {
        validateContent(content);

        log.debug("Analyzing article: title='{}', contentLength={}", title, content.length());

        if (shouldUseExternalAI(content)) {
            try {
                return callExternalProvider(title, content);
            } catch (RuntimeException ex) {
                // 捕获AI调用相关的运行时异常
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
        return properties.isEnabled()
                && StringUtils.hasText(properties.getBaseUrl())
                && content.length() > EXTERNAL_AI_LENGTH_THRESHOLD;
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

    private String guessCategory(String title, String content) {
        String fullText = (title + " " + content).toLowerCase(Locale.ROOT);
        String titleLower = title.toLowerCase(Locale.ROOT);

        Map<String, Integer> scores = calculateCategoryScores(titleLower, fullText);

        if (scores.isEmpty()) {
            return "General";
        }

        return buildCategoryString(scores);
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

            // 正文出现次数 - 使用高效算法
            score += countOccurrences(fullText, lowerKw);
        }

        return score;
    }

    /**
     * 高效的字符串计数算法,避免split产生大量临时数组
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

    private String buildCategoryString(Map<String, Integer> scores) {
        // 找出主类别
        String primary = Collections.max(scores.entrySet(),
                Map.Entry.comparingByValue()).getKey();
        int maxScore = scores.get(primary);

        // 动态阈值,避免次类别过多
        int threshold = Math.max(MIN_CATEGORY_SCORE,
                maxScore / CATEGORY_SCORE_HALF_WEIGHT);
        threshold = Math.max(threshold, MIN_CATEGORY_THRESHOLD);

        // 次类别:按分数降序,然后按名称排序,限制数量
        int finalThreshold = threshold;
        List<String> secondary = scores.entrySet().stream()
                .filter(e -> e.getValue() >= finalThreshold && !e.getKey().equals(primary))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_SECONDARY_CATEGORIES)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return secondary.isEmpty()
                ? primary
                : primary + "|" + String.join("|", secondary);
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
                    .limit(target).map(String::trim).map(String::toUpperCase)
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
                && !STOP_WORDS.contains(word.toLowerCase(Locale.ROOT));
    }


//    public static void main(String[] args) throws JsonProcessingException {
//        System.out.println(new ObjectMapper().writeValueAsString(new DefaultAiContentService(null,null).fallbackContent("KPI 是解药也是毒药", "KPI 需要量化，量化的数字越具体，行动点越明确，那么事情做起来目标感就会越强。这也带来了另外一个问题，大家会把思考都聚焦在这个数字上，去想其他东西的时间变得越来越少。想在 KPI 的文化下把创新做好，是比较有挑战的事情，主要依靠那些除了能够把 KPI 做好，还有余力去思考更多价值的人。\n" +
//                "\n" +
//                "OKR 的作用在于凝聚共识，在上下对焦的过程中，想清楚如何为用户创造价值，不断明确 Objectives，确定思路，形成取舍。再回头看看 KPI，它并不直接关心用户价值这件事情，虽然完成 KPI 上的指标可以一定程度实现用户价值，但它强调的是组织「要什么」，而不是「要怎么做」，更不会回答为什么要这么做。\n" +
//                "\n" +
//                "无论是 KPI 还是 OKR 都只是公司和团队管理的一种工具，既然是工具，就有可以改造和升级的地方。如果你看到有人把 OKR 用的像 KPI，或者把 KPI 用出了 OKR 的味道，也不用惊讶，没有人要求一定要按照书本上的方式玩这些工具。\n" +
//                "\n" +
//                "在原文中打开\n")));  ;
//    }

}