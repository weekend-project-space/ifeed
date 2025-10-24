package org.bitmagic.ifeed.service.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.config.vectore.SearchRequestTurbo;
import org.bitmagic.ifeed.config.vectore.VectorStoreTurbo;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final VectorStoreTurbo vectorStoreTurbo;
    private final EmbeddingModel embeddingModel;

    private final ChatModel chatModel;
    private final UserBehaviorRepository userBehaviorRepository;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final ArticleRepository articleRepository;
    private final Map<UUID, List<UUID>> user2Items = new ConcurrentHashMap<>();

    private final static String SYSTEM_TEXT = """
            # iFeed 新闻记者播报员提示词：汇总今日您喜欢的热门信息
                                                                
            ## 角色定位
            你是一位专业新闻记者播报员，为iFeed（RSS阅读器软件）用户提供每日资讯，风格客观、权威，类似NPR或BBC主播。专注于以紧凑、生动的语言汇总全球热点，吸引用户关注。
                         
            ## 任务要求
            - **内容汇总**：精选当日（基于当前日期）全球热门资讯，覆盖政治、经济、科技、社会、娱乐等领域。优先通过实时工具（如web_search、x_keyword_search、browse_page）从RSS源或其他可靠渠道获取最新信息，确保内容准确、全面。
            - **信息筛选**：聚焦5-8条重大事件，按地域或主题分组（如美国、亚洲、国际、社交媒体）。提供事实、数据、背景，避免主观评论；每条资讯需引用来源，使用inline citation。
            - **输出格式**：纯Markdown，模拟广播脚本。结构包括：开场白（提及iFeed、日期、问候）、主体（分段播报，每段1-2事件，用粗体标题）、结束语（引导互动）。总长度800-1200字，节奏明快。
            - **风格要求**：第一人称播报，语言专业且引人入胜（e.g., “直击新闻前沿！”、“全球风云激荡”）。使用过渡词（如“转向欧洲”）连接段落，适度加入emoji（如🌎、📰）增强视觉效果。鼓励用户通过iFeed评论或分享。
                     
                         
            ## 输出示例
            ```markdown
            ## iFeed 头条
                         
            **各位iFeed用户，早上好！我是您的新闻播报员，为您献上今日的您关注的热点汇总。** 
                        
            🌎 从北美的政坛动荡到亚洲的经济热潮，iFeed带您最新动态！让我们开启今日播报！
                         
            **美国动态：政府关门僵局，移民政策引热议** 
            特朗普宣布提高关税。 ...  [详细...](/articles/:文章id)
            美国联邦政府关门进入第23天，75万雇员受影响，创历史第二长纪录。 [详细...](/articles/:文章id)
                         
            **亚洲焦点：印度经济火热，泰国娱乐热潮**
            印度迪瓦利节销售额达6.05万亿卢比，同比增长25%。  [详细...](/articles/:文章id)
                         
            **结束语**：今日iFeed头条到此结束，世界瞬息万变，资讯尽在掌握！感谢您的收听，明天同一时间再会！📰
                         
            #新闻 #iFeed头条
            ```
                         
            ## 注意事项
            - **长度控制**：每段控制在80字，保持节奏紧凑。优先选择高影响力、新鲜事件，避免过时内容。
            """;


    public Page<ArticleSummaryView> rank(UUID userId, int page, int size) {
        List<UUID> aIds = user2Items.getOrDefault(userId, Collections.emptyList());
        if (page == 0) {
            aIds = recall(userId).stream().limit(100).map(Document::getId).map(UUID::fromString).toList();
            user2Items.put(userId, aIds);
        }
        return articleRepository.findArticleSummariesByIds(aIds, PageRequest.of(page, size));
    }

    public Flux<String> recommendations(UUID uuid, RecommendationScope scope) {
        switch (scope) {
            case RECENT:
                return Flux.fromIterable(List.of("hello world"));
            case PERSONAL:
            default:
                // 默认返回个性化推荐
                log.info("list info");
                List<RecommendationArticle> list = top(uuid, 10, Instant.now().minusSeconds(48 * 3600));
                if (list.isEmpty()) {
                    return Flux.fromIterable(List.of("立即开始阅读，收藏值得反复品读的内容，让信息顺畅的在这会合。"));
                }
//        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_TEXT);
//        Message systemMessage = systemPromptTemplate.createMessage();
                String userText = "这是今天的热门文章的数据```" + list.stream().map(item -> "文章id:%s\n 正文：%s".formatted(item.id(), item.summary())).collect(Collectors.joining("\n\n")) + "```\n请输出markdown 格式内容";
//        Message userMessage = new UserMessage(userText);
//        Prompt prompt = new Prompt(List.of(userMessage, systemMessage));
                log.info("prompt");
                return ChatClient.create(chatModel)
                        .prompt(SYSTEM_TEXT + "\n" + userText)
                        .stream().content();
        }

    }

    private List<Document> recall(UUID userId) {
        return userEmbeddingRepository.findById(userId).map(userEmbedding -> {
            List<String> articleIds = userBehaviorRepository.findById(userId.toString()).map(UserBehaviorDocument::getReadHistory).orElse(Collections.emptyList()).stream().map(UserBehaviorDocument.ArticleRef::getArticleId).toList();
            float[] embed = embeddingModel.embed(userEmbedding.getContent());
            float[] accumulator = minix(embed, userEmbedding.getEmbedding(), 0.8f, 0.2f);
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return vectorStoreTurbo.similaritySearch(SearchRequestTurbo.builder().embedding(accumulator).topK(100).filterExpression(b.nin("articleId", articleIds.toArray()).build()).build());
        }).orElse(Collections.emptyList());
    }

    private List<RecommendationArticle> top(UUID userId, Integer topK, Instant start) {
        return userEmbeddingRepository.findById(userId).map(userEmbedding -> {
            float[] embed = embeddingModel.embed(userEmbedding.getContent());
            float[] accumulator = minix(embed, userEmbedding.getEmbedding(), 0.8f, 0.2f);
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            return vectorStoreTurbo.similaritySearch(SearchRequestTurbo.builder().embedding(accumulator).topK(topK).filterExpression(b.gte("publishedAt", start.getEpochSecond()).build()).build());
        }).orElse(Collections.emptyList()).stream().map(Document::getMetadata).map(meta -> new RecommendationArticle(meta.get("articleId").toString(), meta.get("feedTitle").toString(), meta.get("summary").toString(), meta.get("feedTitle").toString())).toList();
    }


    private float[] minix(float[] embed1, float[] embed2, float weight1, float weight2) {
        float[] accumulator = new float[embed1.length];
        for (int i = 0; i < accumulator.length; i++) {
            accumulator[i] = embed1[i] * weight1;
        }
        for (int i = 0; i < accumulator.length; i++) {
            accumulator[i] += embed2[i] * weight2;
        }
        return accumulator;
    }
}
