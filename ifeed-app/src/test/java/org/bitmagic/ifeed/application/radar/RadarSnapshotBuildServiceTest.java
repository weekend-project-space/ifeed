package org.bitmagic.ifeed.application.radar;

import org.bitmagic.ifeed.config.properties.RadarProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.radar.RadarSnapshot;
import org.bitmagic.ifeed.domain.model.radar.RadarTopicArticle;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.service.radar.RadarTopicNamingService;
import org.bitmagic.ifeed.infrastructure.ai.NSFWService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RadarSnapshotBuildServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private ArticleEmbeddingRepository embeddingRepository;
    @Mock private RadarSnapshotSaveService saveService;
    @Mock private RadarTopicNamingService namingService;
    @Mock private NSFWService nsfwService;
    private RadarSnapshotBuildService service;
    private RadarProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RadarProperties();
        service = new RadarSnapshotBuildService(properties, articleRepository,
                embeddingRepository, saveService, namingService, nsfwService);
    }

    @Test
    void onlyEarliestArticleForEachTitleReachesEmbeddingLookup() {
        Instant now = Instant.now();
        List<Article> candidates = List.of(
                article(1L, "重复标题", now),
                article(2L, "其他标题", now.minusSeconds(120)),
                article(3L, "重复标题", now.minusSeconds(60)));
        when(articleRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Article>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(candidates));

        service.build();

        verify(embeddingRepository).findAllByIds(List.of(2L, 3L));
        verifyNoInteractions(saveService, namingService, nsfwService);
    }

    @Test
    void equalPublicationTimesUseLowestIdAndBlankTitlesStaySeparate() {
        Instant now = Instant.now();
        List<Article> candidates = List.of(
                article(2L, "重复标题", now),
                article(1L, "重复标题", now),
                article(3L, "", now),
                article(4L, "", now));
        when(articleRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Article>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(candidates));

        service.build();

        verify(embeddingRepository).findAllByIds(List.of(1L, 3L, 4L));
    }

    @Test
    void readsPastTenThousandCandidatesBeforeChoosingEarliestTitle() {
        Instant now = Instant.now();
        List<Article> candidates = new ArrayList<>(IntStream.rangeClosed(1, 10000)
                .mapToObj(index -> article((long) index, "标题" + index, now)).toList());
        candidates.add(article(10001L, "标题1", now.minusSeconds(60)));
        AtomicInteger batchNumber = new AtomicInteger();
        when(articleRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Article>>any(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    int batchIndex = batchNumber.getAndIncrement();
                    Pageable pageable = invocation.getArgument(1);
                    assertEquals(0, pageable.getPageNumber());
                    assertTrue(pageable.getSort().getOrderFor("id").isAscending());
                    int start = batchIndex * pageable.getPageSize();
                    return new PageImpl<>(candidates.subList(start, Math.min(start + pageable.getPageSize(), candidates.size())));
                });

        service.build();

        ArgumentCaptor<Collection<Long>> batches = ArgumentCaptor.forClass(Collection.class);
        verify(embeddingRepository, times(10)).findAllByIds(batches.capture());
        List<Long> selectedIds = batches.getAllValues().stream().flatMap(Collection::stream).toList();
        assertEquals(10000, selectedIds.size());
        assertTrue(selectedIds.contains(10001L));
        assertFalse(selectedIds.contains(1L));
        ArgumentCaptor<Specification<Article>> queries = ArgumentCaptor.forClass(Specification.class);
        verify(articleRepository, times(11)).findAll(queries.capture(), any(Pageable.class));
        Root<Article> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Long> idPath = root.get("id");
        queries.getAllValues().getLast().toPredicate(root, null, builder);
        verify(builder).greaterThan(idPath, 10000L);
    }

    @Test
    void candidateQueryExcludesFutureDatesAndKeepsFixedWindow() {
        when(articleRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Article>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        Instant before = Instant.now();

        service.build();

        Instant after = Instant.now();
        ArgumentCaptor<Specification<Article>> query = ArgumentCaptor.forClass(Specification.class);
        verify(articleRepository).findAll(query.capture(), any(Pageable.class));
        Root<Article> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Instant> publishedAt = root.get("publishedAt");
        query.getValue().toPredicate(root, null, builder);
        ArgumentCaptor<Instant> lower = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> upper = ArgumentCaptor.forClass(Instant.class);
        verify(builder).greaterThan(eq(publishedAt), lower.capture());
        verify(builder).lessThanOrEqualTo(eq(publishedAt), upper.capture());
        assertFalse(upper.getValue().isBefore(before));
        assertFalse(upper.getValue().isAfter(after));
        assertEquals(Duration.ofHours(properties.getWindowHours()), Duration.between(lower.getValue(), upper.getValue()));
        verifyNoInteractions(embeddingRepository, saveService);
    }

    @Test
    void exactlyMinimumArticlesFormTopic() {
        stubClusters(5, 0);
        stubNaming();

        service.build();

        verify(saveService).save(any(), argThat(topics -> topics.size() == 1 && topics.getFirst().getArticleCount() == 5),
                argThat(articles -> articles.size() == 5));
    }

    @Test
    void fewerThanMinimumArticlesDoNotFormTopic() {
        stubClusters(4, 0);

        service.build();

        verifyNoInteractions(saveService, namingService, nsfwService);
    }

    @Test
    void singleArticleCanFormTopicWhenMinimumIsOne() {
        properties.setMinClusterSize(1);
        stubClusters(1, 0);
        stubNaming();

        service.build();

        verify(saveService).save(any(), argThat(topics -> topics.size() == 1), argThat(articles -> articles.size() == 1));
    }

    @Test
    void denseClustersKeepAllNeighborsAndSeparateUnrelatedArticles() {
        properties.setMinClusterSize(12);
        stubClusters(15, 15);
        stubNaming();

        service.build();

        ArgumentCaptor<List<RadarTopicArticle>> articles = ArgumentCaptor.forClass(List.class);
        verify(saveService).save(any(), argThat(topics -> topics.size() == 2
                && topics.stream().allMatch(topic -> topic.getArticleCount() == 15)), articles.capture());
        assertEquals(30, articles.getValue().size());
        assertEquals(30, articles.getValue().stream().map(RadarTopicArticle::getArticleId).distinct().count());
        assertEquals(1, articles.getValue().stream().filter(article -> article.getArticleId() <= 15)
                .map(RadarTopicArticle::getTopicId).distinct().count());
    }

    @Test
    void snapshotTtlCannotBeShorterThanUpdateInterval() {
        properties.setFixedDelay(Duration.ofHours(12));
        properties.setSnapshotTtl(Duration.ofHours(2));
        stubClusters(5, 0);
        stubNaming();
        Instant before = Instant.now();

        service.build();

        ArgumentCaptor<RadarSnapshot> snapshot = ArgumentCaptor.forClass(RadarSnapshot.class);
        verify(saveService).save(snapshot.capture(), anyList(), anyList());
        assertFalse(snapshot.getValue().getExpiresAt().isBefore(before.plus(Duration.ofHours(24))));
    }

    @Test
    void configuredLongerTtlIsRespected() {
        properties.setSnapshotTtl(Duration.ofHours(48));
        stubClusters(5, 0);
        stubNaming();
        Instant before = Instant.now();

        service.build();

        ArgumentCaptor<RadarSnapshot> snapshot = ArgumentCaptor.forClass(RadarSnapshot.class);
        verify(saveService).save(snapshot.capture(), anyList(), anyList());
        assertFalse(snapshot.getValue().getExpiresAt().isBefore(before.plus(Duration.ofHours(48))));
    }

    @Test
    void contentCheckFailureSkipsOnlyFailedTopic() {
        stubClusters(5, 5);
        stubNaming();
        when(nsfwService.isNSFW(anyCollection())).thenReturn(false);
        when(nsfwService.isNSFW(anyString())).thenThrow(new IllegalStateException("Content check unavailable")).thenReturn(false);

        service.build();

        verify(saveService).save(any(), argThat(topics -> topics.size() == 1),
                argThat(articles -> articles.size() == 5 && articles.stream().allMatch(article -> article.getArticleId() > 5)));
        verify(namingService, times(1)).nameTopic(anyList());
    }

    @Test
    void blockedTopicIsNotNamedOrPublished() {
        stubClusters(5, 0);
        when(nsfwService.isNSFW(anyCollection())).thenReturn(true);

        service.build();

        verify(saveService).save(any(), eq(List.of()), eq(List.of()));
        verifyNoInteractions(namingService);
        verify(nsfwService, never()).isNSFW(anyString());
    }

    private void stubClusters(int firstSize, int secondSize) {
        Instant now = Instant.now();
        List<Article> candidates = IntStream.rangeClosed(1, firstSize + secondSize)
                .mapToObj(index -> article((long) index, "标题" + index, now)).toList();
        when(articleRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Article>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(candidates));
        when(embeddingRepository.findAllByIds(anyCollection())).thenReturn(candidates.stream()
                .map(article -> new ArticleEmbeddingRecord(article.getId(),
                        article.getId() <= firstSize ? new float[]{1, 0} : new float[]{0, 1})).toList());
    }

    private void stubNaming() {
        when(namingService.nameTopic(anyList()))
                .thenReturn(new RadarTopicNamingService.TopicNameResult("测试话题", "描述", List.of()));
    }

    private Article article(Long id, String title, Instant publishedAt) {
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setPublishedAt(publishedAt);
        article.setLink("https://example.com/" + id);
        return article;
    }
}
