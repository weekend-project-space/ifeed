package org.bitmagic.ifeed.application.tracking;

import org.bitmagic.ifeed.config.properties.TopicTrackingProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrack;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneAnswer;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneModel;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneResponse;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneUsage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TopicTrackJevFilterServiceTest {

    @Test
    void keepsOnlyArticlesAboveConfiguredRelevanceThreshold() {
        SystemOneModel model = mock(SystemOneModel.class);
        when(model.call(any())).thenReturn(response(0.9), response(0.2));
        TopicTrackingProperties properties = new TopicTrackingProperties();
        properties.setJevRelevanceThreshold(0.5);
        TopicTrackJevFilterService service = new TopicTrackJevFilterService(model, properties);
        TopicTrack track = new TopicTrack();
        track.setKeyword("医疗 AI");
        track.setIncludeKeywords("[\"医疗\",\"AI\"]");
        track.setBlockedKeywords("[]");
        Article first = article(1L, "医疗模型发布");
        Article second = article(2L, "无关内容");

        assertEquals(List.of(first), service.filter(track, List.of(first, second)));
        verify(model, times(2)).call(any());
    }

    @Test
    void disabledFilterDoesNotCallJev() {
        SystemOneModel model = mock(SystemOneModel.class);
        TopicTrackingProperties properties = new TopicTrackingProperties();
        properties.setJevEnabled(false);
        TopicTrackJevFilterService service = new TopicTrackJevFilterService(model, properties);
        TopicTrack track = new TopicTrack();
        Article article = article(1L, "标题");

        assertEquals(List.of(article), service.filter(track, List.of(article)));
        verifyNoInteractions(model);
    }

    private SystemOneResponse response(double score) {
        return new SystemOneResponse("test", Map.of("relevant", new SystemOneAnswer.Noul(score)),
                new SystemOneUsage(1, 1));
    }

    private Article article(Long id, String title) {
        Article article = new Article();
        article.setId(id);
        article.setUid(UUID.randomUUID());
        article.setTitle(title);
        article.setLink("https://example.com/" + id);
        article.setContent("正文 " + title);
        article.setPublishedAt(Instant.now());
        return article;
    }
}
