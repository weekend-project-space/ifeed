package org.bitmagic.ifeed.infrastructure.recall.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.bitmagic.ifeed.domain.record.ArticleIdPair;
import org.bitmagic.ifeed.domain.repository.ArticleRepository;
import org.bitmagic.ifeed.domain.repository.UserBehaviorRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户行为数据访问的共享基础设施
 * 提供通用的数据获取、过滤、映射功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBehaviorDataAccessor {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ArticleRepository articleRepository;

    /**
     * 获取用户行为文档
     */
    public Optional<UserBehaviorDocument> getUserBehavior(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userBehaviorRepository.findById(userId.toString());
    }

    /**
     * 过滤和排序文章引用列表
     *
     * @param refs 原始引用列表
     * @param windowDays 时间窗口（天数），0或负数表示不限制
     * @param limit 返回数量限制
     * @return 过滤排序后的列表
     */
    public List<UserBehaviorDocument.ArticleRef> filterAndSortRefs(
            List<UserBehaviorDocument.ArticleRef> refs,
            int windowDays,
            int limit) {

        if (refs == null || refs.isEmpty()) {
            return List.of();
        }

        Instant cutoff = windowDays > 0
                ? Instant.now().minus(Duration.ofDays(windowDays))
                : null;

        return refs.stream()
                .filter(ref -> ref.getArticleId() != null)
                .filter(ref -> ref.getTimestamp() != null) // 只保留有时间戳的
                .filter(ref -> cutoff == null || !ref.getTimestamp().isBefore(cutoff))
                .sorted(Comparator.comparing(
                        UserBehaviorDocument.ArticleRef::getTimestamp,
                        Comparator.naturalOrder()).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 批量转换 articleId (String UUID) 到数据库 ID (Long)
     *
     * @param articleIds UUID字符串列表
     * @return UUID到数据库ID的映射
     */
    public Map<UUID, Long> batchMapArticleIds(List<String> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }

        List<UUID> uuids = articleIds.stream()
                .map(this::safeUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uuids.isEmpty()) {
            return Map.of();
        }

        try {
            return articleRepository.findIdByUIdIn(uuids).stream()
                    .collect(Collectors.toMap(ArticleIdPair::uid, ArticleIdPair::id));
        } catch (Exception e) {
            log.error("Failed to batch map article IDs", e);
            return Map.of();
        }
    }

    /**
     * 构建互动次数映射
     *
     * @param document 用户行为文档
     * @return 文章ID到互动次数的映射
     */
    public Map<String, Long> buildInteractionCountMap(UserBehaviorDocument document) {
        if (document == null
                || document.getInteractionHistory() == null
                || document.getInteractionHistory().isEmpty()) {
            return Map.of();
        }

        return document.getInteractionHistory().stream()
                .filter(inter -> inter.getArticleId() != null)
                .collect(Collectors.groupingBy(
                        UserBehaviorDocument.Interaction::getArticleId,
                        Collectors.counting()));
    }

    /**
     * 安全的 UUID 转换
     */
    public UUID safeUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            log.debug("Invalid UUID format: {}", value);
            return null;
        }
    }

    /**
     * 从引用列表中提取文章ID集合
     */
    public Set<String> extractArticleIds(List<UserBehaviorDocument.ArticleRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return Set.of();
        }
        return refs.stream()
                .map(UserBehaviorDocument.ArticleRef::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}