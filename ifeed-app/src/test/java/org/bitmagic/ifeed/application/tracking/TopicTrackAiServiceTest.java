package org.bitmagic.ifeed.application.tracking;

import org.bitmagic.ifeed.config.properties.TopicTrackingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.bitmagic.ifeed.exception.ApiException;
import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrack;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneModel;

import java.util.Arrays;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TopicTrackAiServiceTest {

    private ChatClient chatClient;
    private ChatModel chatModel;
    private SystemOneModel systemOneModel;
    private TopicTrackingProperties properties;
    private TopicTrackAiService service;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        chatClient = ChatClient.create(chatModel);
        systemOneModel = mock(SystemOneModel.class);
        properties = new TopicTrackingProperties();
        service = new TopicTrackAiService(chatClient, properties);
    }

    @Test
    void callsAiWithCategoryCatalogWhenNoSiteNameMatches() throws Exception {
        var generated = new TopicTrackAiService.Suggestion(List.of("生成式 AI", "医疗人工智能"), List.of(),
                List.of("KNOWLEDGE"), List.of("tech", "health"), "优先关注临床应用");
        modelReturns(generated);

        var result = service.suggest("生成式 AI 在医疗行业的最新应用", List.of("tech", "health"));

        assertEquals(generated.includeKeywords(), result.includeKeywords());
        assertEquals(generated.sourceCategories(), result.sourceCategories());
        assertTrue(result.categories().isEmpty());
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        String text = prompt.getValue().getContents();
        assertTrue(text.contains("health（健康"));
        assertTrue(text.contains("选最多 3 个"));
        assertFalse(text.contains("sourceId"));
    }

    @Test
    void discardsUnknownSitesAndCategoriesAndHonorsConfiguredLimits() throws Exception {
        properties.setMaxCategories(2);
        var generated = new TopicTrackAiService.Suggestion(List.of(), null,
                Arrays.asList(null, " news ", "KNOWLEDGE", "COMMUNITY"),
                List.of("invented", " TECH ", "tech", "health", "science"), null);
        modelReturns(generated);

        var result = service.suggest("医疗 AI", List.of("tech", "health", "science"));

        assertEquals(List.of("tech", "health"), result.sourceCategories());
        assertTrue(result.categories().isEmpty());
        assertEquals(List.of("医疗 AI"), result.includeKeywords());
        assertFalse(result.relevancePrompt().isBlank());
    }

    @Test
    void aiFailureReturnsExplicitRetryableError() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("AI unavailable"));

        ApiException error = assertThrows(ApiException.class, () -> service.suggest("科技新闻", List.of("tech", "finance")));

        assertEquals("TOPIC_SUGGESTION_AI_FAILED", error.getMessage());
    }

    @Test
    void summarizesArticleContentWithoutSystemOne() {
        TopicTrack track = evaluationTopic();
        Article article = evaluationArticle(100L);
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
                new Generation(new AssistantMessage("正文总结")))));

        assertEquals("正文总结", service.summarize(track, List.of(article)));
        verifyNoInteractions(systemOneModel);
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertTrue(prompt.getValue().getContents().contains("内容-100"));
    }

    @Test
    void summarizesLargeGroupsUsingTitlesOnly() {
        TopicTrack track = evaluationTopic();
        properties.setSummaryTextMaxLength(2000);
        List<String> titles = List.of("标题一", "标题二", "标题三");
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
                new Generation(new AssistantMessage("标题总结")))));

        assertEquals("标题总结", service.summarizeTitles(track, titles));
        verifyNoInteractions(systemOneModel);
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertTrue(prompt.getValue().getContents().contains("标题一"));
        assertTrue(prompt.getValue().getContents().contains("标题三"));
        assertTrue(prompt.getValue().getContents().contains("仅根据以下文章标题"));
    }

    @Test
    void hugeTitleGroupIsSampledAcrossTheGroupWithinOneRequestBudget() {
        List<String> titles = LongStream.range(0, 5000)
                .mapToObj(index -> "文章-" + index + "：人工智能最新进展".repeat(10)).toList();
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
                new Generation(new AssistantMessage("标题总结")))));

        assertEquals("标题总结", service.summarizeTitles(evaluationTopic(), titles));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        String text = prompt.getValue().getContents();
        assertTrue(text.getBytes(StandardCharsets.UTF_8).length <= properties.getSummaryInputMaxBytes());
        assertTrue(text.contains("文章-0："));
        assertTrue(text.contains("文章-4999："));
        assertTrue(text.length() < String.join("\n", titles).length());
        assertEquals(properties.getSummaryMaxTokens(), prompt.getValue().getOptions().getMaxTokens());
    }

    @Test
    void contentBudgetIsSharedByEveryArticleIncludingLongRequirementsAndUnicodeTitles() {
        properties.setSummaryInputMaxBytes(4096);
        TopicTrack track = evaluationTopic();
        track.setRelevancePrompt("总结要求".repeat(750));
        List<Article> articles = LongStream.rangeClosed(1, 30).mapToObj(index -> {
            Article article = evaluationArticle(index);
            article.setContent("正文-" + index + "：" + "研究进展😀".repeat(400));
            return article;
        }).toList();
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
                new Generation(new AssistantMessage("正文总结")))));

        assertEquals("正文总结", service.summarize(track, articles));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        String text = prompt.getValue().getContents();
        assertTrue(text.getBytes(StandardCharsets.UTF_8).length <= properties.getSummaryInputMaxBytes());
        for (long index = 1; index <= 30; index++) {
            assertTrue(text.contains("标题-" + index));
            assertTrue(text.contains("正文-" + index + "："));
        }
        assertTrue(text.contains(track.getKeyword()));
        assertFalse(text.contains("\ufffd"));
        assertEquals(text, new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }

    @Test
    void duplicateAndOversizedTitlesCannotExhaustInputBudget() {
        String title = "标题😀".repeat(10000);
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
                new Generation(new AssistantMessage("标题总结")))));

        service.summarizeTitles(evaluationTopic(), Arrays.asList(null, " ", title, title));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        String text = prompt.getValue().getContents();
        assertTrue(text.getBytes(StandardCharsets.UTF_8).length <= properties.getSummaryInputMaxBytes());
        assertFalse(text.contains("\ufffd"));
        assertEquals(text, new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }

    @Test
    void realChatClientTreatsBracesAndTemplateNamesInKeywordAsLiteralData() throws Exception {
        String keyword = "跟踪 {\"ai\": true} 和 {spring_ai_soc_format} 的变化";
        modelReturns(new TopicTrackAiService.Suggestion(List.of("AI"), List.of(),
                List.of("NEWS"), List.of("tech"), "关注配置变化"));

        service.suggest(keyword, List.of("tech"));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertTrue(prompt.getValue().getContents().contains(keyword));
    }

    @Test
    void removesOversizedTermsBeforeReturningEditableSuggestion() throws Exception {
        modelReturns(new TopicTrackAiService.Suggestion(List.of("词".repeat(81), "AI"),
                List.of("词".repeat(81), "广告"), List.of("NEWS"), List.of("tech"), "规则"));

        var result = service.suggest("AI", List.of("tech"));

        assertEquals(List.of("AI"), result.includeKeywords());
        assertEquals(List.of("广告"), result.blockedKeywords());
    }

    @Test
    void longDemandWithNoUsableKeywordsDoesNotReturnAnUnsaveableFallback() throws Exception {
        modelReturns(new TopicTrackAiService.Suggestion(List.of("词".repeat(81)), List.of(),
                List.of("NEWS"), List.of("tech"), "规则"));

        assertThrows(ApiException.class, () -> service.suggest("需求".repeat(100), List.of("tech")));
    }

    @Test
    void invalidJsonEmptyResponseAndOversizedPromptReturnExplicitFailure() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("invalid JSON")))));
        assertThrows(ApiException.class, () -> service.suggest("AI", List.of("tech")));
        when(chatModel.call(any(Prompt.class))).thenReturn(null);
        assertThrows(ApiException.class, () -> service.suggest("AI", List.of("tech")));
        modelReturns(new TopicTrackAiService.Suggestion(List.of("AI"), List.of(),
                List.of("NEWS"), List.of("tech"), "词".repeat(3001)));
        assertThrows(ApiException.class, () -> service.suggest("AI", List.of("tech")));
    }

    private void modelReturns(TopicTrackAiService.Suggestion suggestion) throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
                new Generation(new AssistantMessage(new ObjectMapper().writeValueAsString(suggestion))))));
    }

    private TopicTrack evaluationTopic() {
        TopicTrack track = new TopicTrack();
        track.setKeyword("AI {spring_ai_soc_format}");
        track.setRelevancePrompt("概括应用进展");
        return track;
    }

    private Article evaluationArticle(Long id) {
        Article article = new Article();
        article.setId(id);
        article.setTitle("标题-" + id);
        article.setContent("内容-" + id + " {\"enabled\":true}");
        return article;
    }

    @Test
    void emptyFailedAndOversizedSummariesFailWithoutFallback() {
        List<Article> articles = List.of(evaluationArticle(1L));
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(" ")))));
        assertThrows(IllegalStateException.class, () -> service.summarize(evaluationTopic(), articles));
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> service.summarizeTitles(evaluationTopic(), List.of("标题")));
        properties.setSummaryTextMaxLength(5);
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("超过限制的总结文本")))));
        assertThrows(IllegalStateException.class, () -> service.summarize(evaluationTopic(), articles));
        verifyNoInteractions(systemOneModel);
    }

    @Test
    void emptyInputOrTooManyContentArticlesAreRejected() {
        properties.setSummaryContentArticleLimit(1);
        assertThrows(IllegalArgumentException.class, () -> service.summarize(evaluationTopic(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.summarize(evaluationTopic(),
                List.of(evaluationArticle(1L), evaluationArticle(2L))));
        assertThrows(IllegalArgumentException.class, () -> service.summarizeTitles(evaluationTopic(), List.of()));
        verify(chatModel, never()).call(any(Prompt.class));
    }
}
