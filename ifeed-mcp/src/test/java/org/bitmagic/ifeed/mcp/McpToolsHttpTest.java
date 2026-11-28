package org.bitmagic.ifeed.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.bitmagic.ifeed.application.article.ArticleQueryService;
import org.bitmagic.ifeed.application.article.ArticleRecommendationQueryService;
import org.bitmagic.ifeed.application.search.ArticleSearchQueryService;
import org.bitmagic.ifeed.application.radar.RadarQueryApplicationService;
import org.bitmagic.ifeed.application.subscription.SubscriptionQueryService;
import org.bitmagic.ifeed.domain.service.UserCollectionService;
import org.bitmagic.ifeed.domain.service.UserLikeService;
import org.bitmagic.ifeed.domain.service.UserCollectionFolderService;
import org.bitmagic.ifeed.security.CredentialType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.bitmagic.ifeed.api.request.CollectionRequest;
import org.bitmagic.ifeed.api.response.*;
import org.bitmagic.ifeed.api.response.radar.RadarTopicResponse;
import org.bitmagic.ifeed.application.recommendation.RecResponse;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Runs the official MCP transport and JSON Schema validation with request-local test identities. */
class McpToolsHttpTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private ObjectMapper json;
    private ArticleQueryService articles;
    private ArticleRecommendationQueryService recommendations;
    private RadarQueryApplicationService radar;
    private SubscriptionQueryService subscriptions;
    private UserCollectionService collections;
    private UserLikeService likes;
    private UserCollectionFolderService folders;
    private final UUID articleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters((Filter) (request, response, chain) -> {
                    var header = ((jakarta.servlet.http.HttpServletRequest) request).getHeader("Authorization");
                    try {
                        if (header != null && header.startsWith("Bearer ")) {
                            String token = header.substring(7);
                            int userId = token.endsWith("8") ? 8 : 7;
                            var type = token.startsWith("mcp") ? CredentialType.MCP : CredentialType.WEB;
                            var auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(user(userId)), null,
                                    List.of(new SimpleGrantedAuthority(type.authority())));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                        chain.doFilter(request, response);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }).build();
        json = context.getBean(ObjectMapper.class);
        articles = context.getBean(ArticleQueryService.class);
        recommendations = context.getBean(ArticleRecommendationQueryService.class);
        radar = context.getBean(RadarQueryApplicationService.class);
        subscriptions = context.getBean(SubscriptionQueryService.class);
        collections = context.getBean(UserCollectionService.class);
        likes = context.getBean(UserLikeService.class);
        folders = context.getBean(UserCollectionFolderService.class);

    }

    @AfterEach
    void tearDown() {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        context.close();
    }

    @Test
    void discoversAllToolsAndMarksStateChangesAsIdempotentWrites() throws Exception {
        var tools = rpc("mcp-7", "tools/list", Map.of()).at("/result/tools");
        assertThat(tools).hasSize(12);
        for (var tool : tools) {
            boolean write = tool.path("name").asText().startsWith("set_article_");
            assertThat(tool.at("/annotations/readOnlyHint").asBoolean()).isEqualTo(!write);
            assertThat(tool.at("/annotations/destructiveHint").asBoolean()).isEqualTo(write);
            assertThat(tool.at("/annotations/idempotentHint").asBoolean()).isTrue();
            assertThat(tool.at("/inputSchema/additionalProperties").asBoolean()).isFalse();
            assertThat(tool.at("/inputSchema/properties/userId").isMissingNode()).isTrue();
        }
        verifyNoInteractions(articles, recommendations, radar, subscriptions, collections, likes, folders);
    }

    @Test
    void recommendationsUseTheCurrentCredentialUserAndBoundedPagination() throws Exception {
        var pageable = PageRequest.of(2, 5);
        var recommendation = new RecResponse(articleId, "Recommended", "Summary", UUID.randomUUID(),
                "Feed", null, Instant.now().toString(), null, null, "global", 0.91, "For you");
        var resultPage = new PageImpl<>(List.of(new ArticleWithStateResponse<>(recommendation, false, true, null)), pageable, 11);
        when(recommendations.recommendations(8, 2, 5)).thenReturn(resultPage);

        var result = call("mcp-8", "get_recommendations", Map.of("page", 2, "size", 5));
        var page = result.at("/result/structuredContent");
        assertThat(result.at("/result/isError").asBoolean()).isFalse();
        assertThat(page.path("page").asInt()).isEqualTo(2);
        assertThat(page.path("size").asInt()).isEqualTo(5);
        assertThat(page.path("totalElements").asLong()).isEqualTo(11);
        assertThat(page.at("/content/0/id").asText()).isEqualTo(articleId.toString());
        assertThat(page.at("/content/0/liked").asBoolean()).isTrue();
        verify(recommendations).recommendations(8, 2, 5);

        when(recommendations.recommendations(7, 0, 10)).thenReturn(new PageImpl<>(List.of()));
        assertThat(call("mcp-7", "get_recommendations", Map.of()).at("/result/isError").asBoolean()).isFalse();
        verify(recommendations).recommendations(7, 0, 10);

        assertFailed(call("mcp-7", "get_recommendations", Map.of("size", 51)));
        assertFailed(call("mcp-7", "get_recommendations", Map.of("page", -1)));
        verify(recommendations, never()).recommendations(anyInt(), eq(0), eq(51));
        verify(recommendations, never()).recommendations(anyInt(), eq(-1), anyInt());
    }

    @Test
    void requiresMcpIdentityAtTheProtocolBoundary() throws Exception {
        mvc.perform(post("/api/mcp").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(request("web-7", "tools/list", Map.of())).andExpect(status().isUnauthorized());
        verifyNoInteractions(articles, recommendations, radar, subscriptions, collections, likes, folders);
    }

    @Test
    void radarTopicUsesSnapshotAndTopicFromDigestAndDefaultsToSummaries() throws Exception {
        var topicId = UUID.randomUUID();
        var snapshot = "radar_20261127_w72h";
        var topic = new RadarTopicResponse(topicId.toString(), "Topic", "Summary", Instant.now(), Instant.now(), 1, List.of());
        when(radar.digest(eq(null), eq(null), any())).thenReturn(
                new PageResponse<>(List.of(topic), 0, 10, 1, 1, Map.of("snapshotId", snapshot)));
        when(radar.topicDetail(eq(snapshot), eq(topicId), eq(false), any())).thenReturn(
                new PageResponse<>(List.of(), 0, 10, 0, 0, Map.of("snapshotId", snapshot)));
        var digest = call("mcp-7", "get_radar_digest", Map.of());
        assertThat(digest.at("/result/structuredContent/meta/snapshotId").asText()).isEqualTo(snapshot);
        var result = call("mcp-7", "get_radar_topic", Map.of("snapshotId", snapshot, "topicId", topicId.toString()));
        assertThat(result.at("/result/isError").asBoolean()).isFalse();
        verify(radar).topicDetail(snapshot, topicId, false, PageRequest.of(0, 10));
        assertFailed(call("mcp-7", "get_radar_topic", Map.of("topicId", topicId.toString())));
    }

    @Test
    void subscriptionsPaginateAndNeverExposeSpringPageInternals() throws Exception {
        var values = IntStream.range(0, 3).mapToObj(index -> new SubscriptionResponse(UUID.randomUUID().toString(),
                "Feed " + index, null, null, null, null, null, null, false, 0, null,
                "FEED", null)).toList();
        when(subscriptions.list(7, PageRequest.of(1, 2))).thenReturn(new PageImpl<>(values.subList(2, 3), PageRequest.of(1, 2), 3));
        when(subscriptions.list(7, PageRequest.of(10, 2))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(10, 2), 3));
        var result = call("mcp-7", "list_my_subscriptions", Map.of("page", 1, "size", 2)).at("/result/structuredContent");
        assertThat(result.path("totalElements").asInt()).isEqualTo(3);
        assertThat(result.path("totalPages").asInt()).isEqualTo(2);
        assertThat(result.path("page").asInt()).isEqualTo(1);
        assertThat(result.path("content")).hasSize(1);
        assertThat(result.at("/content/0/type").asText()).isEqualTo("FEED");
        assertThat(result.has("pageable")).isFalse();
        assertThat(result.has("sort")).isFalse();
        var emptyPage = call("mcp-7", "list_my_subscriptions", Map.of("page", 10, "size", 2));
        assertThat(emptyPage.at("/result/structuredContent/content")).isEmpty();
    }

    @Test
    void feedArticleListsReuseCurrentUserAndStableNewestFirstOrder() throws Exception {
        var feedId = UUID.randomUUID().toString();
        var pageable = PageRequest.of(2, 5, Sort.Direction.DESC, "publishedAt", "id");
        when(articles.listArticles(any(), eq(feedId), isNull(), isNull(), eq("owner"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        var result = call("mcp-8", "list_feed_articles", Map.of("feedId", feedId, "page", 2, "size", 5));
        assertThat(result.at("/result/isError").asBoolean()).isFalse();
        verify(articles).listArticles(eq(8), eq(feedId), isNull(), isNull(), eq("owner"), eq(pageable));
        when(articles.listArticles(any(), eq(feedId), isNull(), isNull(), eq("owner"), eq(pageable)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Feed not found"));
        assertFailed(call("mcp-7", "list_feed_articles", Map.of("feedId", feedId, "page", 2, "size", 5)));
    }

    @Test
    void likesUseExplicitStateAndStayIsolatedBetweenUsers() throws Exception {
        when(likes.add(anyInt(), eq(articleId))).thenReturn(new LikeStateResponse(articleId, true, Instant.now()));
        var args = Map.<String, Object>of("articleId", articleId.toString(), "liked", true);
        for (int i = 0; i < 2; i++) {
            assertThat(call("mcp-7", "set_article_like", args).at("/result/structuredContent/liked").asBoolean()).isTrue();
        }
        call("mcp-8", "set_article_like", args);
        verify(likes, times(2)).add(7, articleId);
        verify(likes).add(8, articleId);
        verify(likes, never()).remove(any(), any());
        var unlike = call("mcp-7", "set_article_like", Map.of("articleId", articleId.toString(), "liked", false));
        assertThat(unlike.at("/result/structuredContent/liked").asBoolean()).isFalse();
        verify(likes).remove(7, articleId);
    }

    @Test
    void collectionDistinguishesOmittedFolderFromNullAndRequiresOwnedFolder() throws Exception {
        when(collections.addToCollection(anyInt(), eq(articleId), any()))
                .thenReturn(new CollectionStateResponse(articleId, null, Instant.now()));
        var args = new HashMap<String, Object>();
        args.put("articleId", articleId.toString());
        args.put("collected", true);
        assertThat(call("mcp-7", "set_article_collection", args).at("/result/structuredContent/collected").asBoolean()).isTrue();
        verify(collections).addToCollection(eq(7), eq(articleId), isNull());
        args.put("folderId", null);
        assertThat(call("mcp-7", "set_article_collection", args).at("/result/isError").asBoolean()).isFalse();
        verify(collections).addToCollection(anyInt(), eq(articleId),
                argThat(r -> r != null && r.isFolderSpecified() && r.getFolderId() == null));
        var folderId = UUID.randomUUID();
        when(collections.addToCollection(anyInt(), any(UUID.class), argThat(r -> r != null && folderId.equals(r.getFolderId()))))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Collection folder not found"));
        args.put("folderId", folderId.toString());
        assertFailed(call("mcp-7", "set_article_collection", args));
        verify(collections, never()).removeFromCollection(any(), any());
        args.put("collected", false);
        assertFailed(call("mcp-7", "set_article_collection", args));
        verify(collections, never()).removeFromCollection(any(), any());
        args.remove("folderId");
        var removed = call("mcp-7", "set_article_collection", args);
        assertThat(removed.at("/result/structuredContent/collected").asBoolean()).isFalse();
        verify(collections).removeFromCollection(7, articleId);
    }

    @Test
    void invalidArgumentsNeverReachBusinessControllers() throws Exception {
        for (var args : List.of(
                Map.of("articleId", articleId.toString()),
                Map.of("articleId", articleId.toString(), "liked", "false"),
                Map.of("articleId", "not-a-uuid", "liked", true),
                Map.of("articleId", articleId.toString(), "liked", true, "userId", 8))) {
            assertFailed(call("mcp-7", "set_article_like", args));
        }
        assertFailed(call("mcp-7", "set_article_collection", Map.of("articleId", articleId.toString(), "collected", true, "folderId", "bad-id")));
        assertFailed(call("mcp-7", "list_my_subscriptions", Map.of("size", 51)));
        assertFailed(call("mcp-7", "list_feed_articles", Map.of("feedId", UUID.randomUUID().toString(), "page", -1)));
        verifyNoInteractions(articles, recommendations, radar, subscriptions, collections, likes, folders);
    }

    @Test
    void personalListsUseAuthenticatedUserAndSupportDefaultFolder() throws Exception {
        when(collections.listCollections(anyInt(), eq("default"), any())).thenReturn(new PageImpl<>(List.of()));
        when(likes.list(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(folders.list(any(), any())).thenReturn(new PageImpl<>(List.of()));
        for (String name : List.of("list_my_collections", "list_my_likes", "list_collection_folders")) {
            var args = name.equals("list_my_collections") ? Map.of("folderId", "default") : Map.<String, Object>of();
            var result = call("mcp-8", name, args);
            assertThat(result.at("/result/isError").asBoolean()).isFalse();
            assertThat(result.at("/result/structuredContent/content").isArray()).isTrue();
        }
        verify(collections).listCollections(eq(8), eq("default"), any());
        verify(likes).list(eq(8), any());
        verify(folders).list(eq(8), any());
    }

    private JsonNode call(String token, String name, Map<String, ?> args) throws Exception {
        return rpc(token, "tools/call", Map.of("name", name, "arguments", args));
    }

    private JsonNode rpc(String token, String method, Object params) throws Exception {
        var result = mvc.perform(request(token, method, params)).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private MockHttpServletRequestBuilder request(String token, String method, Object params) throws Exception {
        return post("/api/mcp").header("Authorization", "Bearer " + token)
                .contentType("application/json").accept("application/json", "text/event-stream")
                .content(json.writeValueAsBytes(Map.of("jsonrpc", "2.0", "id", 1, "method", method, "params", params)));
    }

    private static void assertFailed(JsonNode response) {
        assertThat(response.has("error") || response.at("/result/isError").asBoolean()).isTrue();
    }

    private static User user(int id) {
        var user = new User();
        user.setId(id);
        user.setUsername("mcp-user-" + id);
        return user;
    }

    @Configuration
    @EnableWebMvc
    @Import({McpController.class, McpTools.class})
    static class Config {
        @Bean ObjectMapper json() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean ArticleSearchQueryService search() { return mock(ArticleSearchQueryService.class); }
        @Bean ArticleQueryService articles() { return mock(ArticleQueryService.class); }
        @Bean ArticleRecommendationQueryService recommendations() { return mock(ArticleRecommendationQueryService.class); }
        @Bean RadarQueryApplicationService radar() { return mock(RadarQueryApplicationService.class); }
        @Bean SubscriptionQueryService subscriptions() { return mock(SubscriptionQueryService.class); }
        @Bean UserCollectionService collections() { return mock(UserCollectionService.class); }
        @Bean UserLikeService likes() { return mock(UserLikeService.class); }
        @Bean UserCollectionFolderService folders() { return mock(UserCollectionFolderService.class); }
    }
}
