package org.bitmagic.ifeed.domain.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.bitmagic.ifeed.config.PromptProvider;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.ArticleEnrichment;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.repository.ArticleEnrichmentRepository;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.spec.ArticleEnrichmentSpec;
import org.bitmagic.ifeed.infrastructure.ai.rerank.RerankerModel;
import org.bitmagic.ifeed.infrastructure.score.ContentQualityEvaluator;
import org.bitmagic.ifeed.infrastructure.score.ScoreMerger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * @author yangrd
 * @date 2025/12/15
 **/
@Slf4j
@Service
public class ArticleEnrichmentService {

    private final ChatClient chatClient;
    private final RerankerModel rerankerModel;
    private final ContentQualityEvaluator qualityEvaluator = new ContentQualityEvaluator();
    private final ArticleEnrichmentRepository enrichmentRepository;
    private final FeedRepository feedRepository;


    private String summaryPromptTemplate;
    private String mindMapPromptTemplate;
    private Map<String, String> ratingPromptMap;

    public ArticleEnrichmentService(ChatClient chatClient, RerankerModel rerankerModel, PromptProvider promptProvider, ArticleEnrichmentRepository enrichmentRepository, FeedRepository feedRepository) {
        this.chatClient = chatClient;
        this.rerankerModel = rerankerModel;
        this.enrichmentRepository = enrichmentRepository;
        this.feedRepository = feedRepository;
        this.summaryPromptTemplate = promptProvider.getPrompt("summary-prompt");
        this.mindMapPromptTemplate = promptProvider.getPrompt("mind-map-prompt");
        this.ratingPromptMap = promptProvider.getPromptGroup("rating-prompt");
    }

    /**
     * 为文章生成 AI 增强信息
     */
    public ArticleEnrichment enrichArticle(Article article) {
        return enrichArticle(article, false);
    }


    public ArticleEnrichment enrichArticle(Article article, boolean genAllTop) {
        Assert.notNull(article, "article must not be null");
        log.debug("开始为文章生成增强信息: id={}, title={}", article.getId(), article.getTitle());

        // 1. 计算内容质量评分
        double contentScore = getContentScore(article);
        String contentGrade = ContentQualityEvaluator.getGrade(contentScore);
        log.debug("内容质量评分: score={}, grade={}", String.format("%.2f", contentScore), contentGrade);

        // 创建或获取 Enrichment
        ArticleEnrichment enrichment = enrichmentRepository.findById(article.getId())
                .orElse(ArticleEnrichment.builder()
                        .id(article.getId())
                        .build());

        // 如果内容质量太差（D级），直接返回
        if ("D".equals(contentGrade) || "E".equals(contentGrade) || "F".equals(contentGrade)) {
            log.debug("文章质量过低({}级)，跳过AI增强: articleId={}", contentGrade, article.getId());
            enrichment.setRating(ArticleEnrichment.Rating.D);
            return enrichment;
        }
//        有3/5 在D
        String finalGrade = contentGrade;
        if (!"C".equals(contentGrade)) {
            // 2. 计算排序分数（reranker分数）
            double rerankerScore = getRerankerScore(article);
            String rerankerGrade = toGrade(rerankerScore);
            log.debug("Reranker评分: score={}, grade={}", String.format("%.2f", rerankerScore), rerankerGrade);

            // 3. 综合评分（reranker70% + 内容30%）
            finalGrade = ScoreMerger.merge(rerankerGrade, contentGrade);
            log.debug("综合评级: {} (内容) + {} (reranker) = {} (最终)",
                    contentGrade, rerankerGrade, finalGrade);
        }

        ArticleEnrichment.Rating rating = ArticleEnrichment.Rating.valueOf(finalGrade);
        enrichment.setRating(rating);
        if (genAllTop ? !ArticleEnrichment.Rating.D.equals(rating) : ArticleEnrichment.Rating.A.equals(rating) || ArticleEnrichment.Rating.B.equals(rating)) {
            // 4. 生成AI增强内容
            log.debug("开始生成AI增强内容: articleId={}", article.getId());
            if (Strings.isBlank(enrichment.getAiSummary())) {
                String aiSummary = generateSummary(article);
                String mindMap = generateMindMap(article);
                enrichment.setAiSummary(aiSummary);
                enrichment.setMindMap(mindMap);
                log.info("文章增强信息生成完成: title=[{}] articleId={}, finalGrade={}, " +
                                "summaryLength={}, hasMindMap={}",
                        article.getTitle(),
                        article.getId(),
                        finalGrade,
                        aiSummary != null ? aiSummary.length() : 0,
                        mindMap != null && !mindMap.isEmpty());
            }

        } else {
            log.info("文章增强信息生成完成: title=[{}] articleId={}, finalGrade={}, ",
                    article.getTitle(),
                    article.getId(),
                    finalGrade);
        }

        return enrichment;
    }

    public ArticleEnrichment getEnrichment(Long articleId) {
        return enrichmentRepository.findById(articleId).orElse(null);
    }

    public boolean existsById(Long articleId) {
        return enrichmentRepository.existsById(articleId);
    }

    public boolean requiresUpgrade(Long articleId) {
        return enrichmentRepository.exists(ArticleEnrichmentSpec.toTop(articleId));
    }

    /**
     * 生成文章总结
     */
    private String generateSummary(Article article) {
        String prompt = buildSummaryPrompt(article);

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("生成文章总结失败: articleId={}", article.getId(), e);
            return "AI 总结生成失败";
        }
    }

    /**
     * 生成思维导图
     */
    private String generateMindMap(Article article) {
        String prompt = buildMindMapPrompt(article);

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("生成思维导图失败: articleId={}", article.getId(), e);
            return "思维导图生成失败";
        }
    }

    private double getRerankerScore(Article article) {
        try {
            String content = removeImagesAndLinks(article.getContent());
            String category = feedRepository.findById(article.getFeed().getId()).map(Feed::getCategory).orElse("other").toUpperCase();
            return rerankerModel.documentScore(ratingPromptMap.getOrDefault(category, ""), truncateContent(content, 650), null);
        } catch (Exception e) {
            log.error("生成打分失败: articleId={}", article.getId(), e);
            return -10;
        }
    }

    private double getContentScore(Article article) {
        String category = feedRepository.findById(article.getFeed().getId()).map(Feed::getCategory).orElse("other").toUpperCase();
        ContentQualityEvaluator.ContentCategory contentCategory = Strings.isNotBlank(category) ? ContentQualityEvaluator.ContentCategory.valueOf(category) : ContentQualityEvaluator.ContentCategory.OTHER;
        return qualityEvaluator.evaluate(contentCategory, article.getTitle(), article.getContent()).totalScore();
    }

    /**
     * 构建总结提示词
     */
    private String buildSummaryPrompt(Article article) {
        return buildPrompt(summaryPromptTemplate, article);
    }

    /**
     * 构建思维导图提示词
     */
    private String buildMindMapPrompt(Article article) {
        return buildPrompt(mindMapPromptTemplate, article);
    }

    /**
     * 将 BGE 原始分数转换为等级
     *
     * @param score BGE-Reranker-v2-m3 原始分数（如: -8, 0, 5.26）
     * @return "A", "B", "C", "D" 或 "F"
     */
    public static String toGrade(double score) {
        // 不需要 Sigmoid，直接用原始分数
        if (score >= 0) return "A";  // 最优质
        if (score >= -1.5) return "B";
        if (score >= -3.0) return "C";
        if (score >= -4.5) return "D";
        return "F";
    }


    /**
     * 构建提示词（通用方法）
     * 支持两种模式：
     * 1. 模板模式：包含 {id}, {title}, {content} 占位符
     * 2. 追加模式：没有占位符时，自动在前面添加文章信息
     */
    private String buildPrompt(String template, Article article) {
        // 检查是否包含占位符
        boolean hasPlaceholders = template.contains("{id}")
                || template.contains("{title}")
                || template.contains("{content}");

        String content = removeImagesAndLinks(article.getContent());
        if (hasPlaceholders) {
            // 模板模式：替换占位符
            return template
                    .replace("{id}", String.valueOf(article.getId()))
                    .replace("{title}", article.getTitle())
                    .replace("{content}", truncateContent(content, 6000));
        } else {
            // 追加模式：标题 + 内容 + 提示词
            StringBuilder sb = new StringBuilder();
            sb.append("标题：").append(article.getTitle()).append("\n\n");
            sb.append("内容：\n").append(truncateContent(content, 6000));
            sb.append("\n\n-----------------------------------------------------------------------\n\n");
            sb.append(template);
            return sb.toString();
        }
    }

    /**
     * 截断内容（避免超过 token 限制）
     */
    private String truncateContent(String content, int maxLength) {
        return StringUtils.truncate(content, maxLength);
    }

    public static String removeImagesAndLinks(String md) {
        if (md == null) return null;

        // 1️⃣ 图片：![alt](url) → alt
        md = md.replaceAll("!\\[([^]]*)\\]\\([^\\)]*\\)", "$1");

        // 2️⃣ 普通链接：[text](url) → text
        md = md.replaceAll("\\[([^]]*)\\]\\([^\\)]*\\)", "$1");

        // 3️⃣ 引用式图片：![alt][id]
        md = md.replaceAll("!\\[([^]]*)\\]\\[[^\\]]*\\]", "$1");

        // 4️⃣ 引用式链接：[text][id]
        md = md.replaceAll("\\[([^]]*)\\]\\[[^\\]]*\\]", "$1");

        // 5️⃣ 移除引用定义
        md = md.replaceAll("(?m)^\\s*\\[[^\\]]+\\]:\\s*\\S+.*$", "");

        return md;
    }
}