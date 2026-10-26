package org.bitmagic.ifeed.application.tracking;

import org.bitmagic.ifeed.config.properties.TopicTrackingProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class TopicTrackEmbeddingServiceTest {

    private EmbeddingModel model;
    private TopicTrackingProperties properties;
    private TopicTrackEmbeddingService service;

    @BeforeEach
    void setUp() {
        model = mock(EmbeddingModel.class);
        properties = new TopicTrackingProperties();
        service = new TopicTrackEmbeddingService(model, properties);
    }

    @Test
    void batchesTextsAndPreservesArticleIdOrderAndVectorAssociation() {
        properties.setEmbeddingBatchSize(2);
        Article first = article(3L, " 标题 ", "摘要", "描述", "正文");
        Article second = article(1L, "标题二", " ", "描述二", "正文二");
        Article third = article(9L, "标题三", null, null, "正文三");
        when(model.embed(anyList())).thenReturn(List.of(new float[]{1, 0}, new float[]{0, 1}), List.of(new float[]{1, 1}));

        var results = service.embed(List.of(first, second, third));

        assertEquals(List.of(3L, 1L, 9L), results.stream().map(result -> result.id()).toList());
        assertArrayEquals(new float[]{0, 1}, results.get(1).embedding());
        ArgumentCaptor<List<String>> texts = ArgumentCaptor.forClass(List.class);
        verify(model, times(2)).embed(texts.capture());
        assertEquals(List.of(List.of("标题\n摘要", "标题二\n描述二"), List.of("标题三\n正文三")), texts.getAllValues());
    }

    @Test
    void limitsTextLengthAndSupportsTitleOnlyAndContentOnlyArticles() {
        properties.setEmbeddingTextMaxLength(5);
        when(model.embed(anyList())).thenReturn(List.of(new float[]{1}, new float[]{2}, new float[]{3}));

        service.embed(List.of(article(1L, "长标题123456", "内容", null, null),
                article(2L, " 标题 ", null, null, null), article(3L, null, null, null, " 正文 ")));

        verify(model).embed(List.of("长标题12", "标题", "正文"));
    }

    @Test
    void emptyInputDoesNotCallModelAndEmptyArticleTextFailsExplicitly() {
        assertTrue(service.embed(List.of()).isEmpty());
        assertThrows(IllegalStateException.class, () -> service.embed(List.of(article(1L, " ", null, " ", null))));
        verifyNoInteractions(model);
    }

    @Test
    void rejectsMissingMismatchedAndInvalidVectors() {
        List<Article> articles = List.of(article(1L, "标题", null, null, null));
        when(model.embed(anyList())).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.embed(articles));
        when(model.embed(anyList())).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> service.embed(articles));
        for (float[] vector : new float[][]{null, new float[]{}, new float[]{0, 0}, new float[]{Float.NaN}, new float[]{Float.POSITIVE_INFINITY}}) {
            when(model.embed(anyList())).thenReturn(Arrays.asList(vector));
            assertThrows(IllegalStateException.class, () -> service.embed(articles));
        }
    }

    @Test
    void rejectsDimensionChangesBetweenBatchesAndPropagatesModelFailures() {
        properties.setEmbeddingBatchSize(1);
        List<Article> articles = List.of(article(1L, "一", null, null, null), article(2L, "二", null, null, null));
        when(model.embed(anyList())).thenReturn(List.of(new float[]{1, 0}), List.of(new float[]{1}));
        assertThrows(IllegalStateException.class, () -> service.embed(articles));
        when(model.embed(anyList())).thenThrow(new IllegalStateException("model unavailable"));
        assertThrows(IllegalStateException.class, () -> service.embed(articles));
    }

    @Test
    void embeddingRequestBatchesDoNotSplitClustersAtSixteenArticles() {
        List<Article> articles = LongStream.rangeClosed(1, 18)
                .mapToObj(id -> article(id, "同一事件-" + id, "相同主题", null, null)).toList();
        when(model.embed(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return texts.stream().map(text -> new float[]{1, 0}).toList();
        });
        TopicArticleClusters clusters = new TopicArticleClusters(0.85);

        clusters.add(articles.stream().map(Article::getId).toList(), service.embed(articles));

        assertEquals(List.of(LongStream.rangeClosed(1, 18).boxed().toList()), clusters.articleGroups());
        ArgumentCaptor<List<String>> batches = ArgumentCaptor.forClass(List.class);
        verify(model, times(2)).embed(batches.capture());
        assertEquals(List.of(16, 2), batches.getAllValues().stream().map(List::size).toList());
    }

    private Article article(Long id, String title, String summary, String description, String content) {
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setSummary(summary);
        article.setDescription(description);
        article.setContent(content);
        return article;
    }
}
