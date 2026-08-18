package org.bitmagic.ifeed.domain.service.radar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.api.response.PageResponse;
import org.bitmagic.ifeed.api.response.radar.RadarItemResponse;
import org.bitmagic.ifeed.api.response.radar.RadarTopicResponse;
import org.bitmagic.ifeed.domain.model.radar.RadarSnapshot;
import org.bitmagic.ifeed.domain.model.radar.RadarTopic;
import org.bitmagic.ifeed.domain.model.radar.RadarTopicArticle;
import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.radar.RadarTopicArticleRepository;
import org.bitmagic.ifeed.domain.repository.radar.RadarTopicRepository;
import org.bitmagic.ifeed.domain.spec.RadarTopicArticleSpecs;
import org.bitmagic.ifeed.domain.spec.RadarTopicSpecs;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.infrastructure.util.DateUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RadarQueryService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };

    private final RadarSnapshotService snapshotService;
    private final RadarTopicRepository topicRepository;
    private final RadarTopicArticleRepository topicArticleRepository;
    private final ArticleRepository articleRepository;

    public PageResponse<RadarTopicResponse> digest(String snapshotId, Integer windowHours, Pageable pageable) {
        RadarSnapshot snapshot = snapshotService.resolveSnapshot(snapshotId, windowHours);

        Page<RadarTopic> topics = topicRepository.findAll(
                RadarTopicSpecs.snapshotId(snapshot.getSnapshotId()),
                pageable
        );
        Page<RadarTopicResponse> mapped = topics.map(this::toTopicResponse);
        return PageResponse.from(mapped, Map.of(
                "snapshotId", snapshot.getSnapshotId(),
                "generatedAt", snapshot.getGeneratedAt(),
                "windowHours", snapshot.getWindowHours()
        ));
    }

    public PageResponse<RadarItemResponse> topicDetail(String snapshotId, UUID topicId, boolean includeContent, Pageable pageable) {
        RadarSnapshot snapshot = snapshotService.resolveSnapshot(snapshotId);

        RadarTopic topic = topicRepository.findById(topicId)
                .filter(t -> Objects.equals(snapshot.getSnapshotId(), t.getSnapshotId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND"));

        Page<RadarTopicArticle> refs = topicArticleRepository.findAll(
                RadarTopicArticleSpecs.bySnapshotAndTopic(snapshot.getSnapshotId(), topicId),
                pageable
        );
        List<Long> articleIds = refs.getContent().stream().map(RadarTopicArticle::getArticleId).toList();
        Map<Long, ArticleSummaryView> summaries = articleRepository.findArticleSummariesByIds(articleIds).stream()
                .collect(Collectors.toMap(ArticleSummaryView::articleId, v -> v));

        Map<Long, String> contents = Collections.emptyMap();
        if (includeContent && !articleIds.isEmpty()) {
            contents = articleRepository.findContentsByArticleIds(articleIds).stream()
                    .collect(Collectors.toMap(row -> (Long) row[0], row -> (String) row[1]));
        }

        final Map<Long, String> resolvedContents = contents;
        List<RadarItemResponse> items = refs.getContent().stream().map(ref -> {
            ArticleSummaryView view = summaries.get(ref.getArticleId());
            if (view == null) {
                return null;
            }
            return toItemResponse(view, ref.getScore(), includeContent ? resolvedContents.get(ref.getArticleId()) : null);
        }).filter(Objects::nonNull).toList();

        Page<RadarItemResponse> itemPage = new PageImpl<>(items, pageable, refs.getTotalElements());
        Map<String, Object> meta = new HashMap<>();
        meta.put("snapshotId", snapshot.getSnapshotId());
        meta.put("includeContent", includeContent);
        meta.put("topic", toTopicResponse(topic));
        return PageResponse.from(itemPage, meta);
    }

    private RadarTopicResponse toTopicResponse(RadarTopic topic) {
        return new RadarTopicResponse(
                topic.getTopicId().toString(),
                topic.getTitle(),
                topic.getDescription(),
                topic.getCreatedAt(),
                topic.getUpdatedAt(),
                topic.getArticleCount() == null ? 0 : topic.getArticleCount(),
                parseKeywords(topic.getTopKeywords())
        );
    }

    private List<String> parseKeywords(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(rawJson, TAGS_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse keywords JSON: {}", rawJson, e);
            return Collections.emptyList();
        }
    }

    private RadarItemResponse toItemResponse(ArticleSummaryView view, double score, String content) {
        return new RadarItemResponse(
                view.id().toString(),
                view.title(),
                view.summary(),
                content,
                view.thumbnail(),
                view.enclosure(),
                view.feedTitle(),
                view.publishedAt() != null ? view.publishedAt().toString() : null,
                extractTags(view.tags()),
                score,
                view.publishedAt() != null ? DateUtils.formatRelativeTime(view.publishedAt()) : null
        );
    }

    private List<String> extractTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(raw, TAGS_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse tags JSON: {}", raw, e);
            return Collections.emptyList();
        }
    }
}
