<template>
    <div class="space-y-8">
        <section class="space-y-6">
                <div class="flex flex-col gap-4">
                    <div class="flex flex-wrap items-center justify-between gap-3">
                        <div>
                            <h1 class="text-3xl f-semibold text-text">为你推荐</h1>
                            <p class="text-sm text-text-secondary">灵感来自订阅与 AI 筛选 · 分类与热门标签来自最近两周</p>
                        </div>
                        <button
                            class="inline-flex items-center gap-2 rounded-full border border-primary/20 px-4 py-2 text-sm font-semibold text-primary transition hover:bg-primary/10"
                            @click="refresh">
                            <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor"
                                stroke-width="1.6">
                                <path stroke-linecap="round" stroke-linejoin="round"
                                    d="M4.5 8.5A5.5 5.5 0 0 1 10 3a5.5 5.5 0 0 1 4.75 2.75M15.5 11.5A5.5 5.5 0 0 1 10 17a5.5 5.5 0 0 1-4.75-2.75" />
                                <path stroke-linecap="round" stroke-linejoin="round"
                                    d="M15.5 5.75V3h-2.75M4.5 14.25V17h2.75" />
                            </svg>
                            刷新推荐
                        </button>
                    </div>

                    <div class="space-y-3">
                        <!-- 分类：样式参考标签胶囊条 -->
                        <div class="flex items-center gap-2 overflow-x-auto whitespace-nowrap rounded-2xl border border-outline/20 bg-surface p-2 text-sm text-text-secondary">
                            <button
                                class="rounded-full px-4 py-2 font-medium transition bg-surface text-text-secondary cursor-default"
                                disabled>
                                分类
                            </button>
                            <!-- 全部分类按钮 -->
                            <button
                                class="rounded-full px-4 py-2 font-medium transition"
                                :class="getFilterClassForCategory('')"
                                @click="clearCategoryFilter()">
                                全部
                            </button>
                            <template v-if="insightsLoading">
                                <span class="text-text-muted">正在加载分类...</span>
                            </template>
                            <template v-else>
                                <button v-for="c in topCategories" :key="c.category"
                                    class="rounded-full px-4 py-2 font-medium transition"
                                    :class="getFilterClassForCategory(c.category)"
                                    @click="handleSelectCategory(c.category)">
                                    {{ c.category }}
                                </button>
                                <span v-if="!topCategories.length" class="text-text-muted">暂无分类统计</span>
                            </template>
                        </div>

                        <!-- 隐藏标签快速筛选：如需还原可取消注释 -->
                        <!--
                        <div class="flex items-center gap-2 overflow-x-auto whitespace-nowrap rounded-2xl border border-outline/20 bg-surface p-2 text-sm text-text-secondary">
                            <button v-for="filter in quickFilters" :key="filter.value ?? 'all'" type="button"
                                class="rounded-full px-4 py-2 font-medium transition" :class="getFilterClass(filter.value)"
                                @click="filter.value ? handleSelectTag(filter.value) : clearTagFilter()">
                                {{ filter.label }}
                            </button>
                        </div>
                        -->
                    </div>

                    <div class="flex flex-wrap gap-2 text-xs text-text-secondary">
                        <div v-if="activeFeedInfo"
                            class="flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-primary">
                            <span class="max-w-[14rem] truncate font-medium">
                                {{ activeFeedInfo.title || activeFeedInfo.siteUrl || activeFeedInfo.url }}
                            </span>
                            <button
                                class="rounded-full bg-primary px-2 py-0.5 text-[11px] font-semibold text-primary-foreground transition hover:bg-primary/90"
                                @click="openFeedChannel">
                                频道
                            </button>
                            <button class="text-xs font-semibold text-primary/80 transition hover:text-primary"
                                @click="clearFeedFilter">
                                清除
                            </button>
                        </div>
                        <div v-else-if="activeFeedId"
                            class="flex items-center gap-2 rounded-full border border-outline/30 bg-surface px-3 py-1">
                            <span class="max-w-[14rem] truncate font-medium">频道筛选：{{ activeFeedId }}</span>
                            <button class="text-xs font-semibold text-primary transition hover:underline"
                                @click="clearFeedFilter">
                                清除
                            </button>
                        </div>
                        <div v-if="activeTag"
                            class="flex items-center gap-2 rounded-full border border-outline/30 bg-surface px-3 py-1">
                            <span class="font-medium">#{{ activeTag }}</span>
                            <button class="text-xs font-semibold text-primary transition hover:underline"
                                @click="clearTagFilter">
                                移除
                            </button>
                        </div>
                    </div>
                </div>

                <div class="grid gap-6 xl:grid-cols-[2.1fr,1fr]">
                    <div
                        class="relative overflow-hidden rounded-3xl border border-outline/30 bg-surface-container px-8 py-8">
                        <div
                            class="pointer-events-none absolute -left-20 top-10 h-48 w-48 rounded-full bg-primary/15 blur-3xl">
                        </div>
                        <div
                            class="pointer-events-none absolute -right-32 bottom-0 h-56 w-56 rounded-full bg-primary/10 blur-3xl">
                        </div>
                        <div class="relative flex h-full flex-col gap-6">
                            <div
                                class="flex items-center gap-3 text-xs font-semibold uppercase tracking-[0.28em] text-primary">
                                <span class="inline-flex h-1.5 w-1.5 rounded-full bg-primary"></span>
                                今日精选
                            </div>
                            <div class="space-y-3">
                                <h2 class="text-3xl font-semibold leading-tight text-text">
                                    {{ highlight.title }}
                                </h2>
                                <p class="text-sm text-text-secondary">
                                    {{ highlight.description }}
                                </p>
                            </div>
                            <div
                                class="rounded-2xl border border-outline/20 bg-surface p-4 text-sm leading-relaxed text-text-secondary">
                                {{ highlight.summary }}
                            </div>
                            <div class="mt-auto flex flex-wrap items-center justify-between gap-3">
                                <div class="flex flex-wrap gap-2 text-xs text-primary">
                                    <button v-for="tag in highlight.tags" :key="tag" type="button"
                                        class="rounded-full bg-primary/10 px-3 py-1 font-medium transition hover:bg-primary/20"
                                        @click="handleSelectTag(tag)">
                                        #{{ tag }}
                                    </button>
                                </div>
                                <button v-if="highlight.id"
                                    class="inline-flex items-center gap-2 rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary/40"
                                    @click="handleSelect(highlight.id)">
                                    开始阅读
                                    <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor"
                                        stroke-width="1.6">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="M7 4l6 6-6 6" />
                                    </svg>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="flex flex-col gap-4">
                        <div class="rounded-3xl border border-primary/25 bg-primary/10 p-6 text-primary">
                            <div class="space-y-2">
                                <h3 class="text-lg font-semibold text-primary/90">今日阅读进度</h3>
                                <p class="text-sm text-primary/80">你已收藏 {{ stats.savedCount }} 篇文章。</p>
                            </div>
                            <div class="mt-6 space-y-4">
                                <div class="text-4xl font-bold tracking-tight text-primary/90">{{ stats.readGoalPercent
                                    }}%</div>
                                <div class="flex items-center gap-3">
                                    <div class="h-2 flex-1 overflow-hidden rounded-full bg-primary/20">
                                        <div class="h-full rounded-full bg-primary"
                                            :style="{ width: `${stats.readGoalPercent}%` }"></div>
                                    </div>
                                    <span class="text-xs font-medium text-primary/80">目标 8 篇</span>
                                </div>
                                <p v-if="stats.remaining > 0" class="text-xs text-primary/70">
                                    距离完成还差 {{ stats.remaining }} 篇，继续加油！
                                </p>
                                <p v-else class="text-xs text-primary/70">
                                    今日目标已达成，看看 AI 还推荐了什么。
                                </p>
                            </div>
                        </div>

                        <div class="rounded-3xl border border-outline/20 bg-surface p-6">
                            <h3 class="text-sm font-semibold text-text">热门标签</h3>
                            <p class="mt-2 text-xs text-text-secondary">快速探索近期高频出现的主题。</p>
                            <div class="mt-4 flex flex-wrap gap-2">
                                <template v-if="insightsLoading">
                                    <span class="text-text-muted text-xs">加载中...</span>
                                </template>
                                <template v-else>
                                    <button v-for="t in hotTags" :key="t.tag"
                                        type="button"
                                        class="rounded-full bg-surface-variant px-3 py-1 text-xs font-medium text-text-secondary transition hover:bg-primary/10 hover:text-primary"
                                        @click="handleSelectTag(t.tag)">
                                        #{{ t.tag }}
                                    </button>
                                    <div v-if="!hotTags.length" class="text-xs text-text-muted">等待新的标签更新...</div>
                                </template>
                            </div>
                        </div>
                    </div>
                </div>
        </section>

        <section class="space-y-5">
                <div class="flex flex-wrap items-center justify-between gap-4">
                    <div class="space-y-1">
                        <h2 class="text-2xl font-semibold text-text">最新推荐</h2>
                        <p class="text-sm text-text-secondary">
                            来自你的订阅源与 AI 智能推荐的精选文章。
                        </p>
                    </div>
                    <button
                        class="inline-flex items-center gap-2 rounded-full border border-primary/20 px-4 py-2 text-sm font-semibold text-primary transition hover:bg-primary/10"
                        @click="refresh">
                        <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
                            <path stroke-linecap="round" stroke-linejoin="round"
                                d="M4.5 8.5A5.5 5.5 0 0 1 10 3a5.5 5.5 0 0 1 4.75 2.75M15.5 11.5A5.5 5.5 0 0 1 10 17a5.5 5.5 0 0 1-4.75-2.75" />
                            <path stroke-linecap="round" stroke-linejoin="round"
                                d="M15.5 5.75V3h-2.75M4.5 14.25V17h2.75" />
                        </svg>
                        刷新列表
                    </button>
                </div>

                <div v-if="articlesLoading"
                    class="grid place-items-center rounded-3xl border border-outline/20 bg-surface-container py-20 text-sm text-text-muted">
                    正在加载文章...
                </div>

                <div v-else class="space-y-4">
                    <ArticleList :items="recommendedArticles" @select="handleSelect"
                        @toggle-favorite="handleToggleFavorite" @select-tag="handleSelectTag" />
                    <p v-if="articleError" class="text-sm text-danger">{{ articleError }}</p>
                </div>

                <div
                    class="flex items-center justify-between rounded-2xl border border-outline/20 bg-surface px-4 py-3 text-sm text-text-secondary">
                    <button
                        class="rounded-full border border-outline/40 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/30 disabled:text-text-muted disabled:opacity-70"
                        :disabled="!hasPrevious" @click="prevPage">
                        上一页
                    </button>
                    <span>第 {{ currentPage }} 页</span>
                    <button
                        class="rounded-full border border-outline/40 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/30 disabled:text-text-muted disabled:opacity-70"
                        :disabled="!hasNext" @click="nextPage">
                        下一页
                    </button>
                </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, useRoute } from 'vue-router';
import ArticleList from '../components/ArticleList.vue';
import { useArticlesStore } from '../stores/articles';
import { useCollectionsStore } from '../stores/collections';
import { useSubscriptionsStore } from '../stores/subscriptions';
// 使用 articles store 提供的 fetchInsights，不引入 axios

const router = useRouter();
const route = useRoute();
const articlesStore = useArticlesStore();
const collectionsStore = useCollectionsStore();
const subscriptionsStore = useSubscriptionsStore();

const {
    items,
    page,
    hasNextPage,
    hasPreviousPage,
    loading: articlesLoading,
    error: articleError
} = storeToRefs(articlesStore);

const { insights, insightsLoading } = storeToRefs(articlesStore);
const { fetchInsights } = articlesStore;
const topCategories = computed(() => insights.value.categories ?? []);
const hotTags = computed(() => insights.value.hotTags ?? []);

const { items: collectionItems } = storeToRefs(collectionsStore);
const { items: subscriptionItems } = storeToRefs(subscriptionsStore);

const routePage = computed(() => {
    const raw = Array.isArray(route.query.page) ? route.query.page[0] : route.query.page;
    const parsed = Number(raw);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
});

const activeFeedId = computed(() => {
    const raw = Array.isArray(route.query.feedId) ? route.query.feedId[0] : route.query.feedId;
    if (typeof raw === 'string') {
        const trimmed = raw.trim();
        if (trimmed.length > 0) {
            return trimmed;
        }
    }
    return null;
});

const activeFeedInfo = computed(() => {
    if (!activeFeedId.value) {
        return null;
    }
    return subscriptionItems.value.find((item) => item.feedId === activeFeedId.value) ?? null;
});

const activeTag = computed(() => {
    const raw = Array.isArray(route.query.tags) ? route.query.tags[0] : route.query.tags;
    if (typeof raw === 'string') {
        const trimmed = raw.trim();
        if (trimmed.length > 0) {
            return trimmed;
        }
    }
    return null;
});

const recommendedArticles = computed(() =>
    items.value.map((item) => ({
        ...item,
        collected: collectionsStore.isCollected(item.id)
    }))
);

const quickFilters = computed(() => {
    const filters: Array<{ label: string; value: string | null }> = [{ label: '全部', value: null }];
    const tagMap = new Map<string, string>();
    for (const article of items.value) {
        if (!article || !Array.isArray(article.tags)) {
            continue;
        }
        for (const tag of article.tags) {
            if (!tag) {
                continue;
            }
            const normalized = tag.toLowerCase();
            if (!tagMap.has(normalized)) {
                tagMap.set(normalized, tag);
            }
            if (tagMap.size >= 11) {
                break;
            }
        }
        if (tagMap.size >= 11) {
            break;
        }
    }
    tagMap.forEach((label, value) => {
        filters.push({ label, value });
    });
    return filters;
});

const highlight = computed(() => {
    const featured = items.value[0];
    if (featured) {
        return {
            id: featured.id,
            title: featured.title ?? '今日推荐',
            description: `${featured.feedTitle ?? '推荐来源'} · ${featured.timeAgo}`,
            summary: featured.summary ?? 'AI 正在为你准备更精彩的内容。',
            tags: featured.tags.length ? featured.tags.slice(0, 4) : ['AI 推荐']
        };
    }
    const subscriptionCount = subscriptionItems.value.length;
    return {
        id: null,
        title: 'AI 智能摘要助力高效阅读',
        description: subscriptionCount
            ? `根据你订阅的 ${subscriptionCount} 个源，我们为你总结了今日最值得关注的资讯。`
            : '添加订阅源后，我们将每日为你推送精选文章与智能摘要。',
        summary: '立即开始阅读，收藏值得反复品读的内容，让知识顺畅流入你的大脑。',
        tags: ['AI', '生产力', '行业趋势', '智能推荐']
    };
});

const stats = computed(() => {
    const savedCount = collectionItems.value.length;
    const totalGoal = 8;
    const readGoalPercent = Math.min(100, Math.round((savedCount / totalGoal) * 100));
    const remaining = Math.max(totalGoal - savedCount, 0);
    return {
        savedCount,
        readGoalPercent,
        remaining
    };
});

const currentPage = computed(() => page.value);
const hasNext = computed(() => hasNextPage.value);
const hasPrevious = computed(() => hasPreviousPage.value);

const buildQuery = (overrides?: { page?: number; feedId?: string | null; tags?: string | null; category?: string | null }) => {
    const query: Record<string, string> = {};
    const hasFeedOverride = overrides && Object.prototype.hasOwnProperty.call(overrides, 'feedId');
    const feedId = hasFeedOverride ? overrides?.feedId ?? null : activeFeedId.value;
    if (feedId) {
        query.feedId = feedId;
    }
    const hasTagOverride = overrides && Object.prototype.hasOwnProperty.call(overrides, 'tags');
    const tag = hasTagOverride ? overrides?.tags ?? null : activeTag.value;
    if (tag) {
        query.tags = tag;
    }
    const hasCategoryOverride = overrides && Object.prototype.hasOwnProperty.call(overrides, 'category');
    const category = hasCategoryOverride ? overrides?.category ?? null : route.query.category as string | null;
    if (category) {
        query.category = category;
    }
    const nextPage = overrides?.page ?? routePage.value;
    if (nextPage > 1) {
        query.page = String(nextPage);
    }
    return query;
};

const loadData = async () => {
    const tasks: Promise<unknown>[] = [];
    if (!collectionItems.value.length) {
        tasks.push(collectionsStore.fetchCollections());
    }

    if (activeFeedId.value && !subscriptionItems.value.length) {
        tasks.push(subscriptionsStore.fetchSubscriptions());
    }

    tasks.push(
        articlesStore.fetchArticles({
            size: 20,
            page: routePage.value,
            feedId: activeFeedId.value,
            tags: activeTag.value,
            // 从路由读取 category 传递给 store 请求
            category: (route.query.category as string | undefined) ?? undefined
        })
    );

    try {
        await Promise.all(tasks);
    } catch (err) {
        console.warn('数据加载失败', err);
    }
};

const refresh = async () => {
    await loadData();
};

const navigateToPage = (target: number) => {
    if (target < 1) {
        return;
    }
    router.push({ name: 'home', query: buildQuery({ page: target }) });
};

const nextPage = () => {
    if (!hasNext.value) {
        return;
    }
    navigateToPage(routePage.value + 1);
};

const prevPage = () => {
    if (!hasPrevious.value) {
        return;
    }
    navigateToPage(Math.max(1, routePage.value - 1));
};

const handleSelect = (articleId: string) => {
    articlesStore.recordHistory(articleId);
    router.push({ name: 'article-detail', params: { id: articleId } });
};

const handleToggleFavorite = async (articleId: string) => {
    const target = recommendedArticles.value.find((item) => item.id === articleId);
    try {
        await collectionsStore.toggleCollection(articleId, { title: target?.title });
    } catch (err) {
        console.warn('收藏操作失败', err);
    }
};

const clearFeedFilter = () => {
    router.push({ name: 'home', query: buildQuery({ feedId: null, page: 1 }) });
};

const clearTagFilter = () => {
    router.push({ name: 'home', query: buildQuery({ tags: null, page: 1 }) });
};

const openFeedChannel = () => {
    const feedId = activeFeedInfo.value?.feedId ?? activeFeedId.value;
    if (!feedId) {
        return;
    }
    router.push({ name: 'feed', params: { feedId } });
};

const handleSelectTag = (tag: string) => {
    if (!tag) {
        return;
    }
    router.push({ name: 'home', query: buildQuery({ page: 1, tags: tag.toLowerCase() }) });
};

const handleSelectCategory = (category: string) => {
    if (!category) {
        return;
    }
    router.push({ name: 'home', query: buildQuery({ page: 1, category: category.toLowerCase(), tags: null }) });
};

const clearCategoryFilter = () => {
    router.push({ name: 'home', query: buildQuery({ page: 1, category: null }) });
};

const getFilterClass = (filterValue: string | null) => {
    if (filterValue) {
        return activeTag.value === filterValue
            ? 'bg-primary text-primary-foreground shadow-sm'
            : 'bg-surface text-text-secondary hover:bg-primary/10 hover:text-primary';
    }
    return !activeTag.value
        ? 'bg-primary text-primary-foreground shadow-sm'
        : 'bg-surface text-text-secondary hover:bg-primary/10 hover:text-primary';
};

// 分类的选中样式与标签一致，但依据当前路由的 category 判断
const getFilterClassForCategory = (category: string) => {
    const current = ((route.query.category as string | undefined) ?? '').toLowerCase();
    const value = (category ?? '').toLowerCase();
    const isSelected = (!current && !value) || (current && current === value);
    return isSelected
        ? 'bg-primary text-primary-foreground shadow-sm'
        : 'bg-surface text-text-secondary hover:bg-primary/10 hover:text-primary';
};

onMounted(() => {
    loadData();
    fetchInsights();
});

watch(
    () => [
        routePage.value,
        activeFeedId.value,
        activeTag.value,
        route.query.category // 确保分类变化触发数据刷新
    ],
    () => {
        loadData();
        // 点击分类/标签时不再刷新概览，避免重复请求
    }
);

watch(
    activeFeedId,
    async (id) => {
        if (id && !subscriptionItems.value.length) {
            try {
                await subscriptionsStore.fetchSubscriptions();
            } catch (err) {
                console.warn('订阅信息加载失败', err);
            }
        }
    },
    { immediate: true }
);
</script>
