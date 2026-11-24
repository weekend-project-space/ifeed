package org.bitmagic.ifeed.application.tracking;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.config.properties.TopicTrackingProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrack;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneModel;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneQuestion;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;

/** Uses Jev to remove keyword-search candidates that are not relevant to the topic. */
@Service
@RequiredArgsConstructor
public class TopicTrackJevFilterService {

    private static final String RELEVANT = "relevant";

    private final SystemOneModel systemOneModel;
    private final TopicTrackingProperties properties;

    public List<Article> filter(TopicTrack track, List<Article> articles) {
        if (articles.isEmpty() || !properties.isJevEnabled()) return articles;
        return articles.stream().filter(article -> isRelevant(track, article)).toList();
    }

    private boolean isRelevant(TopicTrack track, Article article) {
        String state = "话题：" + track.getKeyword()
                + "\n包含关键词：" + arrayText(track.getIncludeKeywords())
                + "\n排除关键词：" + arrayText(track.getBlockedKeywords())
                + "\n文章标题：" + value(article.getTitle())
                + "\n文章内容：" + truncate(articleText(article));
        var questions = new LinkedHashMap<String, SystemOneQuestion>();
        questions.put(RELEVANT, SystemOneQuestion.noul(
                "文章是否与话题有实质关联？标题或正文仅偶然提及关键词不算相关。"));
        double score = systemOneModel.call(new SystemOneRequest(state, questions)).noul(RELEVANT).noul();
        return score >= properties.getJevRelevanceThreshold();
    }

    private String articleText(Article article) {
        if (StringUtils.hasText(article.getContent())) return article.getContent();
        if (StringUtils.hasText(article.getSummary())) return article.getSummary();
        if (StringUtils.hasText(article.getDescription())) return article.getDescription();
        return "";
    }

    private String truncate(String text) {
        if (text.length() <= properties.getJevTextMaxLength()) return text;
        return text.substring(0, properties.getJevTextMaxLength());
    }

    private String arrayText(String json) {
        if (!StringUtils.hasText(json)) return "[]";
        return json;
    }

    private String value(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
