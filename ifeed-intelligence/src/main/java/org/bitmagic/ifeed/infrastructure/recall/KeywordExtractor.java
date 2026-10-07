package org.bitmagic.ifeed.infrastructure.recall;

import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.TermUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 关键词和实体提取服务
 * 从文章标题和摘要中提取关键词和命名实体
 */
@Slf4j
@Component
public class KeywordExtractor {

    private static final int DEFAULT_KEYWORD_COUNT = 5;
    private static final int MAX_ENTITIES = 10;
    private static final int MIN_ENTITY_LENGTH = 2;

    // 技术相关实体词典
    private static final Set<String> TECH_ENTITIES = Set.of(
            // AI/ML
            "AI", "LLM", "GPT", "ChatGPT", "Claude", "Gemini", "Copilot",
            "OpenAI", "Anthropic", "DeepMind", "Midjourney", "Stable Diffusion",
            "Transformer", "BERT", "LoRA", "RLHF", "RAG",

            // 编程语言
            "Java", "Python", "JavaScript", "TypeScript", "Go", "Rust",
            "C++", "C#", "Kotlin", "Swift", "Ruby", "PHP", "Scala",

            // 框架和库
            "React", "Vue", "Angular", "Svelte", "Next.js", "Nuxt",
            "Spring", "Django", "Flask", "FastAPI", "Express",
            "TensorFlow", "PyTorch", "Keras", "scikit-learn",

            // 基础设施和工具
            "Docker", "Kubernetes", "K8s", "Jenkins", "GitLab", "GitHub",
            "AWS", "Azure", "GCP", "Vercel", "Netlify", "Cloudflare",
            "Nginx", "Apache", "Tomcat",  "Kafka", "RabbitMQ",

            // 数据库
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Elasticsearch",
            "Cassandra", "DynamoDB", "Neo4j", "ClickHouse",

            // 操作系统和平台
            "Linux", "Ubuntu", "CentOS", "macOS", "Windows", "Android", "iOS",

            // 开发工具
            "VS Code", "IntelliJ", "PyCharm", "WebStorm", "Vim", "Emacs",
            "Git", "npm", "yarn", "Maven", "Gradle", "Webpack", "Vite",

            // 其他技术术语
            "API", "REST", "GraphQL", "gRPC", "WebSocket", "HTTP", "HTTPS",
            "OAuth", "JWT", "SSL", "TLS", "DNS", "CDN", "VPN",
            "DevOps", "CI/CD", "Agile", "Scrum", "Microservices");

    // 知名公司/组织
    private static final Set<String> KNOWN_ORGS = Set.of(
            "Google", "Microsoft", "Apple", "Amazon", "Meta", "Facebook",
            "Tesla", "SpaceX", "Netflix", "Uber", "Airbnb", "Twitter", "X",
            "ByteDance", "Tencent", "Alibaba", "Baidu", "Huawei", "Xiaomi",
            "OpenAI", "Anthropic", "DeepMind", "Stability AI",
            "GitHub", "GitLab", "Stack Overflow", "Reddit", "HackerNews",
            "阿里巴巴", "腾讯", "百度", "字节跳动", "华为", "小米", "美团", "滴滴");

    // 公司后缀模式
    private static final Set<String> ORG_SUFFIXES = Set.of(
            "Inc", "Ltd", "Corp", "Corporation", "Company", "Technologies",
            "公司", "科技", "集团", "有限公司", "股份有限公司");

    // 产品名称模式（通常是大写字母开头的多词组合）
    private static final Pattern PRODUCT_PATTERN = Pattern.compile(
            "\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)\\b");

    // 版本号模式
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "\\b([A-Za-z]+\\s*\\d+(?:\\.\\d+)*)\\b");

    /**
     * 从标题和摘要中提取关键词
     *
     * @param title   文章标题
     * @param summary 文章摘要
     * @return 关键词列表
     */
    public List<String> extractKeywords(String title, String summary) {
        String combined = combineText(title, summary);
        if (combined.isBlank()) {
            return List.of();
        }

        try {
            return TermUtils.keywords(combined, DEFAULT_KEYWORD_COUNT);
        } catch (Exception e) {
            log.warn("Failed to extract keywords: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 从标题和摘要中提取实体
     *
     * @param title   文章标题
     * @param summary 文章摘要
     * @return 实体列表
     */
    public List<String> extractEntities(String title, String summary) {
        String combined = combineText(title, summary);
        if (combined.isBlank()) {
            return List.of();
        }

        Set<String> entities = new HashSet<>();

        try {
            // 1. 提取技术实体
            entities.addAll(findTechEntities(combined));

            // 2. 提取组织名称
            entities.addAll(findOrganizations(combined));

            // 3. 提取产品名称
            entities.addAll(findProducts(combined));

            // 4. 提取版本号相关
            entities.addAll(findVersionedTerms(combined));

        } catch (Exception e) {
            log.warn("Failed to extract entities: {}", e.getMessage());
        }

        return entities.stream()
                .filter(e -> e.length() >= MIN_ENTITY_LENGTH)
                .distinct()
                .limit(MAX_ENTITIES)
                .collect(Collectors.toList());
    }

    /**
     * 合并标题和摘要文本
     */
    private String combineText(String title, String summary) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append(title);
        }
        if (summary != null && !summary.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(summary);
        }
        return sb.toString();
    }

    /**
     * 查找技术相关实体
     */
    private List<String> findTechEntities(String text) {
        List<String> found = new ArrayList<>();

        for (String entity : TECH_ENTITIES) {
            // 使用单词边界匹配，避免部分匹配
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(entity) + "\\b",
                    Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(text).find()) {
                found.add(entity);
            }
        }

        return found;
    }

    /**
     * 查找组织名称
     */
    private List<String> findOrganizations(String text) {
        List<String> found = new ArrayList<>();

        // 1. 匹配已知组织
        for (String org : KNOWN_ORGS) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(org) + "\\b");
            if (pattern.matcher(text).find()) {
                found.add(org);
            }
        }

        // 2. 匹配带公司后缀的词组
        for (String suffix : ORG_SUFFIXES) {
            // 匹配 "词 + 后缀" 模式
            Pattern pattern = Pattern.compile(
                    "([\\p{L}\\p{N}]+(?:\\s+[\\p{L}\\p{N}]+)*?)\\s+" + Pattern.quote(suffix) + "\\b");
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String orgName = matcher.group(1) + " " + suffix;
                if (orgName.length() >= MIN_ENTITY_LENGTH) {
                    found.add(orgName.trim());
                }
            }
        }

        return found;
    }

    /**
     * 查找产品名称
     */
    private List<String> findProducts(String text) {
        List<String> found = new ArrayList<>();

        // 匹配大写字母开头的多词组合（如 "iPhone Pro", "MacBook Air"）
        Matcher matcher = PRODUCT_PATTERN.matcher(text);
        while (matcher.find()) {
            String product = matcher.group(1);
            // 过滤掉常见的非产品词组
            if (!isCommonPhrase(product)) {
                found.add(product);
            }
        }

        return found;
    }

    /**
     * 查找带版本号的术语
     */
    private List<String> findVersionedTerms(String text) {
        List<String> found = new ArrayList<>();

        Matcher matcher = VERSION_PATTERN.matcher(text);
        while (matcher.find()) {
            String term = matcher.group(1);
            // 只保留包含技术相关词的版本号
            if (containsTechTerm(term)) {
                found.add(term);
            }
        }

        return found;
    }

    /**
     * 判断是否为常见短语（非产品名）
     */
    private boolean isCommonPhrase(String phrase) {
        // 常见的非产品词组
        Set<String> commonPhrases = Set.of(
                "The Best", "New York", "San Francisco", "Los Angeles",
                "United States", "Machine Learning", "Artificial Intelligence");
        return commonPhrases.contains(phrase);
    }

    /**
     * 判断是否包含技术术语
     */
    private boolean containsTechTerm(String text) {
        String lower = text.toLowerCase();
        return TECH_ENTITIES.stream()
                .anyMatch(tech -> lower.contains(tech.toLowerCase()));
    }
}
