package org.bitmagic.ifeed.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.article.ArticleQueryService;
import org.bitmagic.ifeed.application.article.ArticleRecommendationQueryService;
import org.bitmagic.ifeed.application.search.ArticleSearchQueryService;
import org.bitmagic.ifeed.application.radar.RadarQueryApplicationService;
import org.bitmagic.ifeed.application.subscription.SubscriptionQueryService;
import org.bitmagic.ifeed.domain.service.UserCollectionService;
import org.bitmagic.ifeed.domain.service.UserLikeService;
import org.bitmagic.ifeed.domain.service.UserCollectionFolderService;
import org.bitmagic.ifeed.api.request.CollectionRequest;
import org.bitmagic.ifeed.api.response.LikeStateResponse;
import org.bitmagic.ifeed.api.response.PageResponse;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.security.CredentialType;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpTools {
    private static final String UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Set<String> TOOL_NAMES = Set.of(
            "search_articles", "get_article", "get_recommendations", "get_radar_digest", "get_radar_topic",
            "list_my_subscriptions", "list_feed_articles", "list_my_collections", "list_my_likes",
            "list_collection_folders", "set_article_collection", "set_article_like");

    private final ObjectMapper json;
    private final ArticleSearchQueryService search;
    private final ArticleQueryService articles;
    private final ArticleRecommendationQueryService recommendations;
    private final RadarQueryApplicationService radar;
    private final SubscriptionQueryService subscriptions;
    private final UserCollectionService collections;
    private final UserLikeService likes;
    private final UserCollectionFolderService folders;

    public boolean visible(String name) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return CredentialType.MCP.matches(auth) && auth.getPrincipal() instanceof UserPrincipal
                && TOOL_NAMES.contains(name);
    }

    public List<SyncToolSpecification> specifications() {
        var query = Map.of("type", "string", "minLength", 1, "maxLength", 200);
        var page = Map.of("type", "integer", "minimum", 0, "maximum", 1000);
        var size = Map.of("type", "integer", "minimum", 1, "maximum", 50);
        var id = Map.of("type", "string", "format", "uuid", "pattern", "^" + UUID_PATTERN + "$");
        var snapshotId = Map.of("type", "string", "minLength", 1, "maxLength", 128,
                "description", "get_radar_digest 返回的 meta.snapshotId；翻页时沿用同一快照");
        var bool = Map.of("type", "boolean");
        return List.of(
                tool("search_articles", "搜索文章，返回标题、摘要和来源。", fields("query", query, "type", Map.of("type", "string", "enum", List.of("keyword", "semantic")), "page", page, "size", size), List.of("query"), args -> {
                    var principal = principal();
                    var result = search.search(principal.getId(), str(args, "query"), strOr(args, "type", "semantic"), "global", null, null, null,
                            PageRequest.of(integer(args, "page", 0), integer(args, "size", 10), Sort.by(Sort.Direction.DESC, "id")));
                    return result;
                }),
                tool("get_article", "按文章 UUID 获取文章正文、摘要与来源信息。", fields("articleId", id), List.of("articleId"), args -> articles.getArticle(principal().getId(), UUID.fromString(str(args, "articleId")))),
                tool("get_recommendations", "分页获取当前用户的个性化文章推荐；page 从 0 开始，size 默认 10、最大 50。",
                        fields("page", page, "size", size), List.of(), args -> recommendations.recommendations(principal().getId(),
                                integer(args, "page", 0), integer(args, "size", 10))),
                tool("get_radar_digest", "查询近期热点话题及其文章数量，使用返回的 meta.snapshotId 和 topicId 查询话题详情。page 从 0 开始。", fields("snapshotId", snapshotId, "windowHours", Map.of("type", "integer", "minimum", 6, "maximum", 72), "page", page, "size", size), List.of(), args -> {
                    var result = radar.digest(strOr(args, "snapshotId", null), integerOrNull(args, "windowHours"), pageable(args));
                    return result;
                }),
                tool("get_radar_topic", "查询热点话题中的文章。snapshotId 和 topicId 必须取自同一次 get_radar_digest 返回；默认不包含正文。page 从 0 开始。",
                        fields("snapshotId", snapshotId, "topicId", id, "includeContent", bool, "page", page, "size", size),
                        List.of("snapshotId", "topicId"), args -> radar.topicDetail(str(args, "snapshotId"), UUID.fromString(str(args, "topicId")),
                                Boolean.TRUE.equals(args.get("includeContent")), pageable(args))),
                tool("list_my_subscriptions", "分页查看当前用户订阅的 RSS 源；使用返回的 feedId 查询源内文章。page 从 0 开始。",
                        fields("page", page, "size", size), List.of(), args -> {
                            return subscriptions.list(principal().getId(), pageable(args));
                        }),
                tool("list_feed_articles", "按 feedId 分页获取 RSS 源文章，按发布时间倒序；使用 list_my_subscriptions 返回的 feedId。page 从 0 开始。",
                        fields("feedId", id, "page", page, "size", size), List.of("feedId"),
                        args -> articles.listArticles(principal().getId(), str(args, "feedId"), null, null, "owner",
                                pageable(args, "publishedAt", "id"))),
                tool("list_my_collections", "分页查询当前用户收藏的文章。folderId 不传表示全部，default 表示默认收藏夹，其他值必须为自己收藏夹的 UUID。page 从 0 开始。",
                        fields("folderId", Map.of("type", "string", "pattern", "^(default|" + UUID_PATTERN + ")$"), "page", page, "size", size), List.of(),
                        args -> collections.listCollections(principal().getId(), strOr(args, "folderId", null), pageable(args, "collectedAt"))),
                tool("list_my_likes", "分页查看当前用户喜欢的文章，按喜欢时间倒序。page 从 0 开始。",
                        fields("page", page, "size", size), List.of(), args -> likes.list(principal().getId(), pageable(args, "likedAt"))),
                tool("list_collection_folders", "分页查看当前用户的收藏夹，使用返回的 folderId UUID 收藏文章或筛选收藏；默认收藏夹没有 UUID。page 从 0 开始。",
                        fields("page", page, "size", size), List.of(), args -> folders.list(principal().getId(), pageable(args, "createdAt"))),
                tool("set_article_collection", "设置文章收藏状态，仅在用户明确要求收藏、取消收藏或移动收藏时调用。collected=true 收藏，false 取消；重复设置不反转状态。folderId 不传保留已有收藏夹，显式 null 放入默认收藏夹，UUID 必须取自 list_collection_folders；取消收藏时不要传 folderId。",
                        fields("articleId", id, "collected", bool, "folderId", Map.of("type", List.of("string", "null"), "format", "uuid", "pattern", "^" + UUID_PATTERN + "$")),
                        List.of("articleId", "collected"), false, true, args -> {
                            String articleId = str(args, "articleId");
                            if (Boolean.TRUE.equals(args.get("collected"))) {
                                CollectionRequest request = null;
                                if (args.containsKey("folderId")) {
                                    request = new CollectionRequest();
                                    request.setFolderId((String) args.get("folderId"));
                                }
                                var state = collections.addToCollection(principal().getId(), UUID.fromString(articleId), request);
                                if (state == null) throw new IllegalStateException("Missing collection state");
                                return new CollectionResult(state.articleId(), true, state.folderId(), state.collectedAt());
                            }
                            if (args.containsKey("folderId")) throw new IllegalArgumentException("取消收藏时不能指定收藏夹");
                            collections.removeFromCollection(principal().getId(), UUID.fromString(articleId));
                            return new CollectionResult(UUID.fromString(articleId), false, null, null);
                        }),
                tool("set_article_like", "设置文章喜欢状态，仅在用户明确要求喜欢或取消喜欢时调用。liked=true 喜欢，false 取消；重复设置不反转状态。仅操作当前凭证所属用户的数据。",
                        fields("articleId", id, "liked", bool), List.of("articleId", "liked"), false, true, args -> {
                            String articleId = str(args, "articleId");
                            if (Boolean.TRUE.equals(args.get("liked"))) return likes.add(principal().getId(), UUID.fromString(articleId));
                            likes.remove(principal().getId(), UUID.fromString(articleId));
                            return new LikeStateResponse(UUID.fromString(articleId), false, null);
                        })
        );
    }

    private SyncToolSpecification tool(String name, String description, Map<String, Object> properties, List<String> required,
                                       Function<Map<String, Object>, Object> action) {
        return tool(name, description, properties, required, true, false, action);
    }

    private SyncToolSpecification tool(String name, String description, Map<String, Object> properties, List<String> required,
                                       boolean readOnly, boolean destructive, Function<Map<String, Object>, Object> action) {
        var schema = Map.of("type", "object", "properties", properties, "required", required, "additionalProperties", false);
        var spec = Tool.builder().name(name).description(description).inputSchema(schema)
                .annotations(ToolAnnotations.builder().readOnlyHint(readOnly).destructiveHint(destructive).idempotentHint(true).openWorldHint(false).build()).build();
        return new SyncToolSpecification(spec, (context, request) -> {
            if (!visible(name)) return error("凭证无权调用此工具");
            try {
                var data = action.apply(request.arguments() == null ? Map.of() : request.arguments());
                if (data instanceof Page<?> result) data = PageResponse.from(result, Map.of());
                else if (data instanceof Collection<?> result) data = Map.of("content", result);
                return CallToolResult.builder().addTextContent(json.writeValueAsString(data)).structuredContent(data).isError(false).build();
            } catch (AccessDeniedException exception) {
                return error("无权执行该操作或访问所选数据");
            } catch (ApiException exception) {
                return error(exception.getMessage());
            } catch (IllegalArgumentException exception) {
                return error("工具参数无效，请检查文章、收藏夹 ID 和参数值");
            } catch (Exception exception) {
                log.error("MCP tool {} failed", name, exception);
                return error(readOnly ? "查询未能完成，请稍后重试" : "操作结果未确认，请先查询文章的收藏或喜欢状态");
            }
        });
    }

    private UserPrincipal principal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    private static String str(Map<String, Object> args, String key) { return String.valueOf(args.get(key)); }
    private static String strOr(Map<String, Object> args, String key, String fallback) { return args.containsKey(key) ? str(args, key) : fallback; }
    private static int integer(Map<String, Object> args, String key, int fallback) { return args.containsKey(key) ? ((Number) args.get(key)).intValue() : fallback; }
    private static Integer integerOrNull(Map<String, Object> args, String key) { return args.containsKey(key) ? ((Number) args.get(key)).intValue() : null; }
    private static Pageable pageable(Map<String, Object> args, String... sort) {
        var pageable = PageRequest.of(integer(args, "page", 0), integer(args, "size", 10));
        return sort.length == 0 ? pageable : pageable.withSort(Sort.Direction.DESC, sort);
    }
    private static Map<String, Object> fields(Object... values) {
        var fields = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) fields.put((String) values[i], values[i + 1]);
        return fields;
    }
    private static CallToolResult error(String message) { return CallToolResult.builder().addTextContent(message).isError(true).build(); }

    private record CollectionResult(UUID articleId, boolean collected, UUID folderId, Instant collectedAt) { }
}
