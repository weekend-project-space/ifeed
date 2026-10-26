package org.bitmagic.ifeed.application.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.bitmagic.ifeed.api.tracking.TopicTrackConfigRequest;
import org.bitmagic.ifeed.config.properties.TopicTrackingProperties;
import org.bitmagic.ifeed.domain.model.Article;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrack;
import org.bitmagic.ifeed.domain.model.tracking.TopicTrackMatch;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.tracking.TopicTrackMatchRepository;
import org.bitmagic.ifeed.domain.repository.tracking.TopicTrackRepository;
import org.bitmagic.ifeed.domain.repository.tracking.TopicTrackSubscriptionRepository;
import org.bitmagic.ifeed.domain.repository.tracking.TopicTrackSummaryRepository;
import org.bitmagic.ifeed.domain.service.UserService;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.infrastructure.text.search.tracking.TopicArticleCandidate;
import org.bitmagic.ifeed.infrastructure.text.search.tracking.TopicArticleSearchRequest;
import org.bitmagic.ifeed.infrastructure.text.search.tracking.TopicArticleSearchStore;
import org.bitmagic.ifeed.infrastructure.tracking.TopicTrackKeywordLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.LongStream;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicTrackingServiceTest {

    @Mock private TopicTrackRepository topicTrackRepository;
    @Mock private TopicTrackMatchRepository matchRepository;
    @Mock private TopicTrackSummaryRepository summaryRepository;
    @Mock private TopicTrackSubscriptionRepository subscriptionRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private TopicTrackEmbeddingService embeddingService;
    @Mock private TopicArticleSearchStore topicArticleSearchStore;
    @Mock private TopicTrackKeywordLock topicTrackKeywordLock;
    @Mock private EntityManager entityManager;
    @Mock private FeedRepository feedRepository;
    @Mock private UserService userService;
    @Mock private TopicTrackAiService aiService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private org.bitmagic.ifeed.infrastructure.tracking.TopicTrackEvaluationStore evaluationStore;
    @Spy private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Spy private TopicTrackingProperties properties = new TopicTrackingProperties();
    @InjectMocks private TopicTrackingService service;

    @Test
    void naturalLanguageRequestUsesDatabaseCategoriesWhenSiteNameSearchIsEmpty() {
        String keyword = "生成式 AI 在医疗行业的最新应用";
        Feed techFeed = feed(1, "科技资讯", "tech");
        Feed healthFeed = feed(2, "医学期刊", "health");
        when(feedRepository.countByCategory()).thenReturn(List.of(new Object[]{"tech", 12L}, new Object[]{"health", 15L}));
        when(aiService.suggest(keyword, List.of("health", "tech")))
                .thenReturn(suggestion(List.of("tech", "health")));
        when(feedRepository.searchByQueryAndCategory(eq(""), eq("tech"), any())).thenReturn(List.of(techFeed));
        when(feedRepository.searchByQueryAndCategory(eq(""), eq("health"), any())).thenReturn(List.of(healthFeed));

        var result = service.suggest(42, keyword);

        assertEquals(List.of(techFeed.getUid(), healthFeed.getUid()), result.sources().stream().map(source -> source.sourceId()).toList());
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(feedRepository).searchByQueryAndCategory(eq(""), eq("tech"), page.capture());
        assertEquals(10, page.getValue().getPageSize());
        assertNotNull(page.getValue().getSort().getOrderFor("featured"));
        assertNotNull(page.getValue().getSort().getOrderFor("id"));
    }

    @Test
    void selectedSourceCategoriesDetermineReturnedSources() {
        Feed matchedFeed = feed(1, "医学期刊", "health");
        when(feedRepository.countByCategory()).thenReturn(List.<Object[]>of(new Object[]{"health", 15L}));
        when(aiService.suggest(eq("医学期刊"), anyList()))
                .thenReturn(suggestion(List.of("health")));
        when(feedRepository.searchByQueryAndCategory(eq(""), eq("health"), any())).thenReturn(List.of(matchedFeed));

        var result = service.suggest(42, "医学期刊");

        assertEquals(matchedFeed.getUid(), result.sources().getFirst().sourceId());
        verify(feedRepository, never()).searchByQuery(eq("医学期刊"), any());
    }

    @Test
    void respectsCategoryAndPerCategorySiteLimits() {
        properties.setMaxCategories(1);
        properties.setMaxSourcesPerCategory(4);
        when(feedRepository.countByCategory()).thenReturn(List.of(new Object[]{"tech", 20L}, new Object[]{"health", 10L}));
        when(aiService.suggest(eq("AI"), anyList()))
                .thenReturn(suggestion(List.of("unknown", "tech", "health")));
        when(feedRepository.searchByQueryAndCategory(eq(""), eq("tech"), any())).thenReturn(List.of(feed(1, "科技", "tech")));

        service.suggest(42, "AI");

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(feedRepository).searchByQueryAndCategory(eq(""), eq("tech"), page.capture());
        assertEquals(4, page.getValue().getPageSize());
        verify(feedRepository, never()).searchByQueryAndCategory(eq(""), eq("health"), any());
    }

    @Test
    void emptyCatalogDoesNotInventSites() {
        when(feedRepository.countByCategory()).thenReturn(List.<Object[]>of(new Object[]{"tech", 2L}));
        when(aiService.suggest(eq("AI"), anyList()))
                .thenReturn(suggestion(List.of("tech")));
        when(feedRepository.searchByQueryAndCategory(eq(""), eq("tech"), any())).thenReturn(List.of());

        assertTrue(service.suggest(42, "AI").sources().isEmpty());
        verify(feedRepository).searchByQueryAndCategory(eq(""), eq("tech"), any());
    }

    @Test
    void totalSuggestedSourcesStayWithinSaveLimitWithLargerCategoryLimits() {
        properties.setMaxCategories(2);
        properties.setMaxSourcesPerCategory(20);
        when(feedRepository.countByCategory()).thenReturn(List.of(new Object[]{"tech", 20L}, new Object[]{"health", 20L}));
        when(aiService.suggest(eq("AI"), anyList())).thenReturn(suggestion(List.of("tech", "health")));
        List<Feed> techFeeds = IntStream.rangeClosed(1, 20).mapToObj(id -> feed(id, "科技" + id, "tech")).toList();
        List<Feed> healthFeeds = IntStream.rangeClosed(21, 40).mapToObj(id -> feed(id, "健康" + id, "health")).toList();
        when(feedRepository.searchByQueryAndCategory(eq(""), eq("tech"), any())).thenReturn(techFeeds);
        when(feedRepository.searchByQueryAndCategory(eq(""), eq("health"), any())).thenReturn(healthFeeds);

        var result = service.suggest(42, "AI");

        assertEquals(30, result.sources().size());
        assertEquals(30, result.sources().stream().map(source -> source.sourceId()).distinct().count());
        assertEquals(healthFeeds.get(9).getUid(), result.sources().getLast().sourceId());
    }

    @Test
    void categoryCatalogIsNormalizedBeforeAiSelectionAndSourceLookup() {
        when(feedRepository.countByCategory()).thenReturn(List.of(new Object[]{" TECH ", 2L}, new Object[]{"tech", 3L},
                new Object[]{"all", 5L}, new Object[]{"unknown", 1L}));
        when(aiService.suggest("AI", List.of("tech"))).thenReturn(suggestion(List.of("tech")));
        when(feedRepository.searchByQueryAndCategory(eq(""), eq("tech"), any())).thenReturn(List.of(feed(1, "科技", " TECH ")));

        assertEquals(1, service.suggest(42, "AI").sources().size());

        verify(aiService).suggest("AI", List.of("tech"));
        verify(feedRepository).searchByQueryAndCategory(eq(""), eq("tech"), any());
    }

    @Test
    void invalidKeywordDoesNotCallDatabaseOrAi() {
        assertThrows(ApiException.class, () -> service.suggest(42, " "));
        assertThrows(ApiException.class, () -> service.suggest(42, "词".repeat(241)));
        verifyNoInteractions(feedRepository, aiService);
    }

    @Test
    void existingKeywordReturnsReusableTopicWithoutCallingAi() {
        TopicTrack track = track();
        track.setTopicUid(UUID.randomUUID());
        track.setNormalizedKeyword("医疗 ai");
        when(topicTrackRepository.findByNormalizedKeyword("医疗 ai")).thenReturn(Optional.of(track));
        when(feedRepository.findAllById(Set.of(1))).thenReturn(List.of(feed(1, "医疗站点", "health")));
        when(subscriptionRepository.existsByTopicTrack_IdAndUserId(10L, 42)).thenReturn(false);

        var result = service.suggest(42, " 医疗  AI ");

        assertNotNull(result.existingTopic());
        assertEquals(track.getTopicUid(), result.existingTopic().topicId());
        assertFalse(result.existingTopic().subscribed());
        verifyNoInteractions(aiService);
    }

    @Test
    void createWithExistingKeywordSubscribesInsteadOfCreating() {
        TopicTrack track = track();
        track.setNormalizedKeyword("医疗 ai");
        when(userService.findUserById(42)).thenReturn(Optional.of(new User()));
        when(topicTrackRepository.findByNormalizedKeyword("医疗 ai")).thenReturn(Optional.of(track));
        when(subscriptionRepository.existsByTopicTrack_IdAndUserId(10L, 42)).thenReturn(false);
        when(feedRepository.findAllById(Set.of(1))).thenReturn(List.of(feed(1, "医疗站点", "health")));

        var result = service.create(42, new TopicTrackConfigRequest("新名称", "医疗 AI", List.of("AI"),
                List.of(), List.of(UUID.randomUUID()), List.of("NEWS"), "", true));

        assertFalse(result.created());
        assertEquals("医疗 AI", track.getKeyword());
        assertEquals("[\"AI\",\"医疗\"]", track.getIncludeKeywords());
        verify(subscriptionRepository).subscribe(10L, 42);
        verify(topicTrackRepository, never()).save(any());
        verify(topicTrackRepository, never()).countByOwnerId(any());
        verify(topicTrackKeywordLock).lock("医疗 ai");
        verifyNoInteractions(aiService);
    }

    @Test
    void createWithAlreadyFollowedKeywordDoesNotAddAnotherSubscription() {
        TopicTrack track = track();
        when(userService.findUserById(42)).thenReturn(Optional.of(new User()));
        when(topicTrackRepository.findByNormalizedKeyword("医疗 ai")).thenReturn(Optional.of(track));
        when(subscriptionRepository.existsByTopicTrack_IdAndUserId(10L, 42)).thenReturn(true);
        when(feedRepository.findAllById(Set.of(1))).thenReturn(List.of(feed(1, "医疗站点", "health")));

        var result = service.create(42, config("医疗 AI"));

        assertFalse(result.created());
        assertTrue(result.topic().subscribed());
        verify(subscriptionRepository, never()).save(any());
        verify(topicTrackRepository, never()).save(any());
        verifyNoInteractions(transactionTemplate);
        verify(subscriptionRepository, never()).subscribe(any(), any());
        verify(subscriptionRepository, never()).countByUserId(any());
        verify(topicTrackRepository, never()).countByOwnerId(any());
    }

    @ParameterizedTest
    @EnumSource(User.Plan.class)
    void creationLimitUsesCurrentPlan(User.Plan plan) {
        User user = new User();
        user.setCurrentPlan(plan);
        when(userService.findUserById(42)).thenReturn(Optional.of(user));
        when(topicTrackRepository.countByOwnerId(42)).thenReturn((long) plan.getMaxCreatedMixFeed());

        ApiException error = assertThrows(ApiException.class, () -> service.create(42, config("new topic")));

        assertEquals("TOPIC_CREATE_LIMIT_EXCEEDED", error.getMessage());
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatus());
        verify(topicTrackKeywordLock).lockUser(42);
        verify(topicTrackRepository, never()).save(any());
        verifyNoInteractions(subscriptionRepository, feedRepository, transactionTemplate);
    }

    @Test
    void creationChecksAutomaticSubscriptionQuotaBeforeSavingTopic() {
        User user = new User();
        user.setCurrentPlan(User.Plan.STANDARD);
        when(userService.findUserById(42)).thenReturn(Optional.of(user));
        when(subscriptionRepository.countByUserId(42)).thenReturn(300L);

        ApiException error = assertThrows(ApiException.class, () -> service.create(42, config("new topic")));

        assertEquals("TOPIC_SUBSCRIPTION_LIMIT_EXCEEDED", error.getMessage());
        verify(topicTrackRepository, never()).save(any());
        verify(subscriptionRepository, never()).subscribe(any(), any());
        verifyNoInteractions(feedRepository, transactionTemplate);
    }

    @Test
    void reusingExistingTopicStillChecksSubscriptionQuota() {
        when(userService.findUserById(42)).thenReturn(Optional.of(new User()));
        when(topicTrackRepository.findByNormalizedKeyword("医疗 ai")).thenReturn(Optional.of(track()));
        when(subscriptionRepository.countByUserId(42)).thenReturn(60L);

        ApiException error = assertThrows(ApiException.class, () -> service.create(42, config("医疗 AI")));

        assertEquals("TOPIC_SUBSCRIPTION_LIMIT_EXCEEDED", error.getMessage());
        verify(topicTrackRepository, never()).countByOwnerId(any());
        verify(topicTrackRepository, never()).save(any());
        verify(subscriptionRepository, never()).subscribe(any(), any());
    }

    @ParameterizedTest
    @EnumSource(User.Plan.class)
    void subscriptionLimitUsesCurrentPlan(User.Plan plan) {
        User user = new User();
        user.setCurrentPlan(plan);
        UUID topicUid = UUID.randomUUID();
        when(userService.findUserById(42)).thenReturn(Optional.of(user));
        when(topicTrackRepository.findByTopicUid(topicUid)).thenReturn(Optional.of(track()));
        when(subscriptionRepository.countByUserId(42)).thenReturn((long) plan.getMaxSubscriptions());

        ApiException error = assertThrows(ApiException.class, () -> service.subscribe(42, topicUid));

        assertEquals("TOPIC_SUBSCRIPTION_LIMIT_EXCEEDED", error.getMessage());
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatus());
        verify(topicTrackKeywordLock).lockUser(42);
        verify(subscriptionRepository, never()).subscribe(any(), any());
    }

    @Test
    void subscriptionBelowLimitSucceedsAndRepeatedSubscriptionSkipsQuotaCheck() {
        UUID topicUid = UUID.randomUUID();
        when(userService.findUserById(42)).thenReturn(Optional.of(new User()));
        when(topicTrackRepository.findByTopicUid(topicUid)).thenReturn(Optional.of(track()));
        when(subscriptionRepository.countByUserId(42)).thenReturn(59L);
        when(subscriptionRepository.existsByTopicTrack_IdAndUserId(10L, 42)).thenReturn(false, true);

        service.subscribe(42, topicUid);
        service.subscribe(42, topicUid);

        verify(subscriptionRepository).countByUserId(42);
        verify(subscriptionRepository).subscribe(10L, 42);
    }

    @Test
    void disabledExistingTopicCanBeFoundButCannotBeRecreatedOrSubscribed() {
        TopicTrack track = track();
        track.setEnabled(false);
        when(userService.findUserById(42)).thenReturn(Optional.of(new User()));
        when(topicTrackRepository.findByNormalizedKeyword("医疗 ai")).thenReturn(Optional.of(track));
        when(feedRepository.findAllById(Set.of(1))).thenReturn(List.of());

        assertFalse(service.suggest(42, "医疗 AI").existingTopic().enabled());
        ApiException error = assertThrows(ApiException.class, () -> service.create(42, config("医疗 AI")));

        assertEquals("TOPIC_TRACK_DISABLED", error.getMessage());
        verify(subscriptionRepository, never()).save(any());
        verify(topicTrackRepository, never()).save(any());
        verifyNoInteractions(aiService, transactionTemplate);
    }

    @Test
    void ownerCannotRenameTopicToAnotherExistingKeyword() {
        TopicTrack track = track();
        TopicTrack other = track();
        other.setId(11L);
        UUID topicUid = UUID.randomUUID();
        when(topicTrackRepository.findByTopicUidAndOwnerId(topicUid, 42)).thenReturn(Optional.of(track));
        when(topicTrackRepository.findByNormalizedKeyword("robot")).thenReturn(Optional.of(other));

        ApiException error = assertThrows(ApiException.class, () -> service.update(42, topicUid, config(" ROBOT ")));

        assertEquals("TOPIC_KEYWORD_EXISTS", error.getMessage());
        assertEquals("医疗 AI", track.getKeyword());
        assertEquals(1, track.getConfigVersion());
        verify(topicTrackRepository, never()).save(any());
    }

    @Test
    void anotherUserCannotEditTopic() {
        UUID topicUid = UUID.randomUUID();

        assertThrows(ApiException.class, () -> service.update(42, topicUid, config("医疗 AI")));

        verifyNoInteractions(feedRepository);
        verify(topicTrackRepository, never()).findByNormalizedKeyword(any());
        verify(topicTrackRepository, never()).save(any());
    }

    @Test
    void newKeywordCreatesTopicAndStartsScanAfterCommit() {
        UUID sourceUid = UUID.randomUUID();
        Feed source = feed(1, "医疗站点", "health");
        User user = new User();
        user.setCurrentPlan(User.Plan.STANDARD);
        when(userService.findUserById(42)).thenReturn(Optional.of(user));
        when(topicTrackRepository.countByOwnerId(42)).thenReturn(1L);
        when(subscriptionRepository.countByUserId(42)).thenReturn(299L);
        when(feedRepository.findByUid(sourceUid)).thenReturn(Optional.of(source));
        when(feedRepository.findAllById(Set.of(1))).thenReturn(List.of(source));
        when(topicTrackRepository.save(any())).thenAnswer(invocation -> {
            TopicTrack track = invocation.getArgument(0);
            track.setId(20L);
            return track;
        });
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            var result = service.create(42, new TopicTrackConfigRequest("新话题", " ROBOT  Medicine ", List.of("robot"),
                    List.of(), List.of(sourceUid), List.of("NEWS"), "", true));

            assertTrue(result.created());
            ArgumentCaptor<TopicTrack> saved = ArgumentCaptor.forClass(TopicTrack.class);
            verify(topicTrackRepository).save(saved.capture());
            assertEquals("robot medicine", saved.getValue().getNormalizedKeyword());
            assertEquals(42, saved.getValue().getOwnerId());
            verify(subscriptionRepository).subscribe(20L, 42);
            assertEquals(1, org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().size());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private TopicTrackConfigRequest config(String keyword) {
        return new TopicTrackConfigRequest("新名称", keyword, List.of("AI"), List.of(),
                List.of(UUID.randomUUID()), List.of("NEWS"), "", true);
    }

    @Test
    void renamingWithoutChangingFiltersPreservesSummaryVersionAndDoesNotRescan() {
        TopicTrack track = track();
        track.setScanStatus("SUCCEEDED");
        UUID topicUid = UUID.randomUUID();
        Feed feed = feed(1, "医疗站点", "health");
        when(topicTrackRepository.findByTopicUidAndOwnerId(topicUid, 42)).thenReturn(Optional.of(track));
        when(feedRepository.findByUid(feed.getUid())).thenReturn(Optional.of(feed));
        when(feedRepository.findAllById(Set.of(1))).thenReturn(List.of(feed));
        when(topicTrackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(42, topicUid, new TopicTrackConfigRequest("新名称", track.getKeyword(),
                List.of("医疗", "AI"), List.of("广告"), List.of(feed.getUid()), List.of("NEWS"), "", true));

        assertEquals(1, result.configVersion());
        assertEquals("新名称", result.name());
        assertEquals("SUCCEEDED", result.scanStatus());
        verifyNoInteractions(transactionTemplate, aiService);
    }

    @Test
    void scansAcrossPagesAndAssociatesAllMembersWithOneSummaryOutsideModelTransactions() {
        var persisted = prepareScan(track());
        properties.setCandidatePageSize(2);
        properties.setSummaryContentArticleLimit(3);
        when(topicArticleSearchStore.search(any())).thenReturn(
                List.of(new TopicArticleCandidate(1L, 1), new TopicArticleCandidate(2L, 1)),
                List.of(new TopicArticleCandidate(3L, 1)));
        stubArticleLoads();
        when(embeddingService.embed(anyList())).thenAnswer(invocation -> {
            assertFalse(inTransaction);
            List<Article> articles = invocation.getArgument(0);
            return articles.stream().map(article -> new ArticleEmbeddingRecord(article.getId(), new float[]{1, 0})).toList();
        });
        when(aiService.summarize(any(), anyList())).thenAnswer(invocation -> {
            assertFalse(inTransaction);
            return "整组总结";
        });
        stubSummarySave();

        service.scanTopic(10L);

        ArgumentCaptor<TopicArticleSearchRequest> pages = ArgumentCaptor.forClass(TopicArticleSearchRequest.class);
        verify(topicArticleSearchStore, times(2)).search(pages.capture());
        assertNull(pages.getAllValues().getFirst().afterArticleId());
        assertEquals(2L, pages.getAllValues().getLast().afterArticleId());
        assertEquals(Duration.ofDays(180), Duration.between(pages.getValue().publishedFrom(), pages.getValue().publishedTo()));
        verify(evaluationStore).record(eq(10L), eq(1), eq(List.of(1L, 2L, 3L)), any());
        assertEquals("SUCCEEDED", persisted.get().getScanStatus());
        assertNotNull(persisted.get().getLastRunAt());
        assertNull(persisted.get().getScanToken());
        verify(summaryRepository).save(argThat(summary -> summary.getArticleCount() == 3 && !summary.isTitlesOnly()));
        verify(aiService).summarize(any(), argThat(articles -> articles.stream().map(Article::getId).toList()
                .equals(List.of(1L, 2L, 3L))));
        ArgumentCaptor<List<TopicTrackMatch>> matches = ArgumentCaptor.forClass(List.class);
        verify(matchRepository, times(2)).saveAll(matches.capture());
        assertEquals(List.of(2, 1), matches.getAllValues().stream().map(List::size).toList());
        assertEquals(List.of(1L, 2L, 3L), matches.getAllValues().stream().flatMap(List::stream)
                .map(TopicTrackMatch::getArticleId).toList());
        verify(summaryRepository, times(2)).linkArticles(eq(30L), anyList());
        verify(aiService, never()).summarizeTitles(any(), anyList());
    }

    @Test
    void evaluatedPageAdvancesCursorWithoutEmbeddingOrAi() {
        var persisted = prepareScan(track());
        properties.setCandidatePageSize(1);
        when(topicArticleSearchStore.search(any())).thenReturn(List.of(new TopicArticleCandidate(1500L, 1)), List.of());
        when(evaluationStore.evaluated(10L, 1, List.of(1500L))).thenReturn(List.of(1500L));

        service.scanTopic(10L);

        verify(topicArticleSearchStore).search(argThat(request -> Long.valueOf(1500L).equals(request.afterArticleId())));
        verifyNoInteractions(articleRepository, embeddingService, aiService);
        assertEquals("SUCCEEDED", persisted.get().getScanStatus());
    }

    @Test
    void largeGroupUsesAllTitlesAndStillLinksEveryArticleToOneSummary() {
        var persisted = prepareScan(track());
        properties.setCandidatePageSize(2);
        properties.setSummaryContentArticleLimit(2);
        when(topicArticleSearchStore.search(any())).thenReturn(
                List.of(new TopicArticleCandidate(1L, 1), new TopicArticleCandidate(2L, 1)),
                List.of(new TopicArticleCandidate(3L, 1)));
        stubArticleLoads();
        when(embeddingService.embed(anyList())).thenAnswer(invocation -> {
            List<Article> articles = invocation.getArgument(0);
            return articles.stream().map(article -> new ArticleEmbeddingRecord(article.getId(), new float[]{1, 0})).toList();
        });
        when(articleRepository.findTitlesByIdIn(any())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream().map(id -> new org.bitmagic.ifeed.domain.record.ArticleTitle(UUID.randomUUID(), id, "标题-" + id)).toList();
        });
        when(aiService.summarizeTitles(any(), anyList())).thenAnswer(invocation -> {
            assertFalse(inTransaction);
            return "标题总结";
        });
        stubSummarySave();

        service.scanTopic(10L);

        verify(aiService).summarizeTitles(any(), eq(List.of("标题-1", "标题-2", "标题-3")));
        verify(aiService, never()).summarize(any(), anyList());
        verify(summaryRepository).save(argThat(summary -> summary.getArticleCount() == 3 && summary.isTitlesOnly()));
        ArgumentCaptor<List<TopicTrackMatch>> matches = ArgumentCaptor.forClass(List.class);
        verify(matchRepository, times(2)).saveAll(matches.capture());
        assertEquals(List.of(2, 1), matches.getAllValues().stream().map(List::size).toList());
        assertEquals(List.of(1L, 2L, 3L), matches.getAllValues().stream().flatMap(List::stream)
                .map(TopicTrackMatch::getArticleId).toList());
        ArgumentCaptor<List<Long>> associations = ArgumentCaptor.forClass(List.class);
        verify(summaryRepository, times(2)).linkArticles(eq(30L), associations.capture());
        assertEquals(List.of(1L, 2L, 3L), associations.getAllValues().stream().flatMap(List::stream).toList());
        assertEquals("SUCCEEDED", persisted.get().getScanStatus());
    }

    @Test
    void failedClusterKeepsSuccessfulClusterAndRetriesOnlyFailedMembers() {
        var persisted = prepareScan(track());
        when(topicArticleSearchStore.search(any())).thenReturn(
                List.of(new TopicArticleCandidate(1L, 1), new TopicArticleCandidate(2L, 1)));
        stubArticleLoads();
        when(embeddingService.embed(anyList())).thenReturn(List.of(
                new ArticleEmbeddingRecord(1L, new float[]{1, 0}), new ArticleEmbeddingRecord(2L, new float[]{0, 1})));
        when(aiService.summarize(any(), anyList())).thenAnswer(invocation -> {
            assertFalse(inTransaction);
            return "进展";
        }).thenThrow(new IllegalStateException("unavailable"));
        stubSummarySave();

        service.scanTopic(10L);

        verify(summaryRepository).save(argThat(summary -> summary.getArticleCount() == 1
                && summary.getConfigVersion() == 1 && summary.getTopicName().equals("医疗 AI")
                && summary.getKeyword().equals("医疗 AI") && summary.getSummary().equals("进展")));
        verify(evaluationStore).record(eq(10L), eq(1), eq(List.of(1L)), any());
        verify(evaluationStore, never()).record(eq(10L), eq(1), eq(List.of(2L)), any());
        assertEquals("FAILED", persisted.get().getScanStatus());
        assertNull(persisted.get().getLastRunAt());
    }

    @Test
    void activeLeasePreventsAnotherWorkerFromScanningTheTopic() {
        TopicTrack track = track();
        track.setScanToken(UUID.randomUUID());
        track.setScanLeaseUntil(Instant.now().plusSeconds(600));
        var persisted = prepareScan(track);

        service.scanTopic(10L);

        assertEquals(track.getScanToken(), persisted.get().getScanToken());
        verifyNoInteractions(topicArticleSearchStore, aiService, embeddingService);
        verify(topicTrackRepository, never()).saveAndFlush(any());
    }

    @Test
    void expiredLeaseCanBeRecoveredAndEmptyScanSucceeds() {
        TopicTrack track = track();
        track.setScanToken(UUID.randomUUID());
        track.setScanLeaseUntil(Instant.now().minusSeconds(60));
        track.setIncludeKeywords("[]");
        var persisted = prepareScan(track);
        when(topicArticleSearchStore.search(any())).thenReturn(List.of());

        service.scanTopic(10L);

        assertEquals("SUCCEEDED", persisted.get().getScanStatus());
        assertNull(persisted.get().getScanToken());
        verify(topicArticleSearchStore).search(argThat(request -> request.includeKeywords().equals(List.of("医疗 AI"))));
    }

    @Test
    void lostLeaseRejectsResultsWithoutClearingTheNewWorkersLease() {
        var persisted = prepareScan(track());
        when(topicArticleSearchStore.search(any())).thenReturn(List.of(new TopicArticleCandidate(1L, 1)));
        stubArticleLoads();
        when(embeddingService.embed(anyList())).thenReturn(List.of(new ArticleEmbeddingRecord(1L, new float[]{1, 0})));
        UUID replacementToken = UUID.randomUUID();
        when(aiService.summarize(any(), anyList())).thenAnswer(invocation -> {
            persisted.get().setScanToken(replacementToken);
            return "旧任务总结";
        });

        service.scanTopic(10L);

        assertEquals(replacementToken, persisted.get().getScanToken());
        verifyNoInteractions(summaryRepository);
        verify(evaluationStore, never()).record(any(), anyInt(), any(), any());
    }

    @Test
    void editedConfigurationRejectsOldResultsAndSchedulesNewVersion() {
        var persisted = prepareScan(track());
        when(topicArticleSearchStore.search(any())).thenReturn(List.of(new TopicArticleCandidate(1L, 1)));
        stubArticleLoads();
        when(embeddingService.embed(anyList())).thenReturn(List.of(new ArticleEmbeddingRecord(1L, new float[]{1, 0})));
        when(aiService.summarize(any(), anyList())).thenAnswer(invocation -> {
            persisted.get().setConfigVersion(2);
            persisted.get().setKeyword("新话题");
            return "旧配置总结";
        });
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.scanTopic(10L);
            assertEquals("新话题", persisted.get().getKeyword());
            assertEquals("QUEUED", persisted.get().getScanStatus());
            assertEquals(1, org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().size());
            verifyNoInteractions(summaryRepository);
            verify(evaluationStore, never()).record(any(), anyInt(), any(), any());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void failedPersistenceDoesNotMarkArticlesEvaluated() {
        var persisted = prepareScan(track());
        when(topicArticleSearchStore.search(any())).thenReturn(List.of(new TopicArticleCandidate(1L, 1)));
        stubArticleLoads();
        when(embeddingService.embed(anyList())).thenReturn(List.of(new ArticleEmbeddingRecord(1L, new float[]{1, 0})));
        when(aiService.summarize(any(), anyList())).thenReturn("进展");
        when(summaryRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));

        service.scanTopic(10L);

        assertEquals("FAILED", persisted.get().getScanStatus());
        verify(evaluationStore, never()).record(any(), anyInt(), any(), any());
    }

    @Test
    void dimensionChangeAcrossPagesFailsBeforeSummary() {
        var persisted = prepareScan(track());
        properties.setCandidatePageSize(1);
        when(topicArticleSearchStore.search(any())).thenReturn(
                List.of(new TopicArticleCandidate(1L, 1)), List.of(new TopicArticleCandidate(2L, 1)));
        stubArticleLoads();
        when(embeddingService.embed(anyList())).thenReturn(List.of(new ArticleEmbeddingRecord(1L, new float[]{1, 0})),
                List.of(new ArticleEmbeddingRecord(2L, new float[]{1, 0, 0})));

        assertThrows(IllegalStateException.class, () -> service.scanTopic(10L));

        assertEquals("FAILED", persisted.get().getScanStatus());
        assertNull(persisted.get().getLastRunAt());
        verifyNoInteractions(aiService, summaryRepository);
    }

    private boolean inTransaction;

    @Test
    void followingSummariesAreScopedAndUseStableBoundedPagination() {
        Instant before = Instant.parse("2026-10-01T00:00:00Z");
        UUID cursor = UUID.randomUUID();
        var anchor = mock(TopicTrackSummaryRepository.SummaryRow.class);
        when(anchor.getCreatedAt()).thenReturn(before);
        when(anchor.getId()).thenReturn(15L);
        when(summaryRepository.findActiveSummary(cursor)).thenReturn(Optional.of(anchor));
        when(userService.findUserById(42)).thenReturn(Optional.of(new User()));
        when(summaryRepository.findSummaries(isNull(), eq(42), eq(before), eq(15L), any())).thenReturn(List.of());

        var page = service.listSummaries(42, null, true, cursor, 500);

        assertFalse(page.hasNext());
        assertNull(page.nextCursor());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(summaryRepository).findSummaries(isNull(), eq(42), eq(before), eq(15L), pageable.capture());
        assertEquals(101, pageable.getValue().getPageSize());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertThrows(ApiException.class, () -> service.listSummaries(null, null, true, null, 20));
    }

    @Test
    void summariesUseSavedNamesAndKeywordsInsteadOfLiveConfiguration() {
        TopicTrack track = track();
        track.setTopicUid(UUID.randomUUID());
        track.setName("当前名称");
        var summary = mock(TopicTrackSummaryRepository.SummaryRow.class);
        when(summary.getId()).thenReturn(30L);
        when(summary.getTopicName()).thenReturn("生成时名称");
        when(summary.getKeyword()).thenReturn("生成时关键词");
        when(topicTrackRepository.findByTopicUid(track.getTopicUid())).thenReturn(Optional.of(track));
        when(summaryRepository.findSummaries(eq(track.getTopicUid()), isNull(), isNull(), isNull(), any())).thenReturn(List.of(summary));

        var result = service.listSummaries(null, track.getTopicUid(), false, null, 20).content().getFirst();

        assertEquals("生成时名称", result.topicName());
        assertEquals("生成时关键词", result.keyword());
    }

    @Test
    void cursorFeedReturnsOnlyRequestedSummariesAndSixArticlePreviewsPerGroup() {
        var summaries = IntStream.range(1, 4).mapToObj(index -> {
            var summary = mock(TopicTrackSummaryRepository.SummaryRow.class);
            lenient().when(summary.getId()).thenReturn((long) index);
            lenient().when(summary.getSummaryUid()).thenReturn(UUID.randomUUID());
            return summary;
        }).toList();
        when(summaryRepository.findSummaries(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(summaries);

        var page = service.listSummaries(null, null, false, null, 2);

        assertEquals(2, page.content().size());
        assertTrue(page.hasNext());
        assertEquals(summaries.get(1).getSummaryUid(), page.nextCursor());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(summaryRepository, times(2)).findArticles(anyLong(), eq(0L), pageable.capture());
        assertTrue(pageable.getAllValues().stream().allMatch(value -> value.getPageSize() == 6));
        verify(summaryRepository, never()).findArticles(eq(3L), anyLong(), any());
    }

    @Test
    void summaryArticleCursorLoadsNextBatchAndDoesNotExposeNumericIds() {
        UUID summaryUid = UUID.randomUUID();
        UUID articleCursor = UUID.randomUUID();
        var summary = mock(TopicTrackSummaryRepository.SummaryRow.class);
        when(summary.getId()).thenReturn(30L);
        when(summaryRepository.findActiveSummary(summaryUid)).thenReturn(Optional.of(summary));
        when(summaryRepository.findArticleCursor(30L, articleCursor)).thenReturn(Optional.of(100L));
        var articles = IntStream.range(1, 4).mapToObj(index -> {
            var article = mock(TopicTrackSummaryRepository.ArticleRow.class);
            lenient().when(article.getArticleId()).thenReturn(UUID.randomUUID());
            return article;
        }).toList();
        when(summaryRepository.findArticles(eq(30L), eq(100L), any())).thenReturn(articles);

        var page = service.listSummaryArticles(summaryUid, articleCursor, 2);

        assertEquals(2, page.content().size());
        assertTrue(page.hasNext());
        assertEquals(articles.get(1).getArticleId(), page.nextCursor());
        verify(summaryRepository).findArticles(eq(30L), eq(100L), argThat(value -> value.getPageSize() == 3));
    }

    @Test
    void invalidAndMismatchedSummaryCursorsAreRejected() {
        UUID topicUid = UUID.randomUUID();
        UUID cursor = UUID.randomUUID();
        when(topicTrackRepository.findByTopicUid(topicUid)).thenReturn(Optional.of(track()));
        assertThrows(ApiException.class, () -> service.listSummaries(null, topicUid, false, cursor, 20));
        var anchor = mock(TopicTrackSummaryRepository.SummaryRow.class);
        when(anchor.getTopicUid()).thenReturn(UUID.randomUUID());
        when(summaryRepository.findActiveSummary(cursor)).thenReturn(Optional.of(anchor));
        assertThrows(ApiException.class, () -> service.listSummaries(null, topicUid, false, cursor, 20));
        verify(summaryRepository, never()).findSummaries(any(), any(), any(), any(), any());
    }

    private java.util.concurrent.atomic.AtomicReference<TopicTrack> prepareScan(TopicTrack initial) {
        var persisted = new java.util.concurrent.atomic.AtomicReference<>(copy(initial));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            inTransaction = true;
            try {
                return callback.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
            } finally {
                inTransaction = false;
            }
        });
        lenient().doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback = invocation.getArgument(0);
            inTransaction = true;
            try {
                callback.accept(mock(org.springframework.transaction.TransactionStatus.class));
            } finally {
                inTransaction = false;
            }
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(topicTrackRepository.findForScan(10L)).thenAnswer(invocation -> Optional.of(copy(persisted.get())));
        lenient().when(topicTrackRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            TopicTrack track = invocation.getArgument(0);
            persisted.set(copy(track));
            return track;
        });
        lenient().when(topicTrackRepository.save(any())).thenAnswer(invocation -> {
            TopicTrack track = invocation.getArgument(0);
            persisted.set(copy(track));
            return track;
        });
        return persisted;
    }

    private TopicTrack copy(TopicTrack source) {
        TopicTrack target = new TopicTrack();
        org.springframework.beans.BeanUtils.copyProperties(source, target);
        target.setSourceIds(new java.util.HashSet<>(source.getSourceIds()));
        return target;
    }

    private void stubArticleLoads() {
        when(articleRepository.findAllWithFeedByIdIn(any())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream().map(this::article).toList();
        });
    }

    private void stubSummarySave() {
        when(summaryRepository.save(any())).thenAnswer(invocation -> {
            org.bitmagic.ifeed.domain.model.tracking.TopicTrackSummary summary = invocation.getArgument(0);
            summary.setId(30L);
            return summary;
        });
    }

    private TopicTrack track() {
        TopicTrack track = new TopicTrack();
        track.setId(10L);
        track.setKeyword("医疗 AI");
        track.setName("医疗 AI");
        track.setIncludeKeywords("[\"AI\",\"医疗\"]");
        track.setBlockedKeywords("[\"广告\"]");
        track.setCategories("[\"NEWS\"]");
        track.setSourceIds(Set.of(1));
        return track;
    }

    private Article article(Long id) {
        Article article = new Article();
        article.setId(id);
        article.setUid(UUID.randomUUID());
        article.setTitle("article-" + id);
        article.setPublishedAt(Instant.now());
        return article;
    }

    private TopicTrackAiService.Suggestion suggestion(List<String> sourceCategories) {
        return new TopicTrackAiService.Suggestion(List.of("AI"), List.of(),
                List.of("NEWS", "KNOWLEDGE"), sourceCategories, "关注重要应用");
    }

    private Feed feed(int id, String title, String category) {
        Feed feed = new Feed();
        feed.setId(id);
        feed.setUid(UUID.randomUUID());
        feed.setTitle(title);
        feed.setCategory(category);
        feed.setSiteUrl("https://example.com/" + id);
        return feed;
    }
}
