<template>
    <div class="space-y-6 sm:space-y-8">
        <section class="space-y-5 sm:space-y-6">
            <div class="flex flex-col gap-3 sm:gap-4">
                <div class="flex flex-wrap items-center justify-between gap-3">
                    <div>
                        <h1 class="text-lg font-semibold text-text">最新</h1>
                        <p class="text-sm text-text-secondary">来自你的订阅源</p>
                    </div>
                    <div class="flex flex-wrap items-center gap-2">
                        <button type="button"
                            class="inline-flex items-center gap-1 rounded-full border border-outline/40 bg-surface-container px-3 py-1.5 text-xs text-text-secondary transition hover:border-primary/50 hover:text-primary"
                            @click="refresh" :disabled="articlesLoading">
                            <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor"
                                stroke-width="1.6">
                                <path stroke-linecap="round" stroke-linejoin="round"
                                    d="M4 10a6 6 0 0 1 10-4.24M16 10a6 6 0 0 1-10 4.24M4 6V3.5M4 3.5h2.5M4 3.5 6.5 6M16 14v2.5M16 16.5h-2.5M16 16.5 13.5 14" />
                            </svg>
                            <span>{{ articlesLoading ? '刷新中…' : '刷新' }}</span>
                        </button>
                    </div>
                </div>

                <div class="space-y-3">
                    <!-- 分类：样式参考标签胶囊条 -->
                    <div
                        class="flex items-center gap-2 overflow-x-auto whitespace-nowrap rounded-2xl border border-outline/20 bg-surface p-2 text-sm text-text-secondary">
                        <button
                            class="rounded-full px-3 py-1.5 font-medium transition bg-surface text-text-secondary cursor-default sm:px-4 sm:py-2"
                            disabled>
                            分类
                        </button>
                        <!-- 全部分类按钮 -->
                        <button class="rounded-full px-3 py-1.5 font-medium transition sm:px-4 sm:py-2"
                            :class="getFilterClassForCategory('')" @click="clearCategoryFilter()">
                            全部
                        </button>
                        <template v-if="insightsLoading">
                            <span class="text-text-muted">正在加载分类...</span>
                        </template>
                        <template v-else>
                            <button v-for="c in topCategories" :key="c.category"
                                class="rounded-full px-3 py-1.5 font-medium transition sm:px-4 sm:py-2"
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
                    <!-- Feed 筛选相关 UI 已移除 -->
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


            <!--            <div class="rounded-3xl border border-outline/20 bg-surface p-6">-->
            <!--                <h3 class="text-sm font-semibold text-text">热门标签</h3>-->
            <!--                <p class="mt-2 text-xs text-text-secondary">快速探索近期高频出现的主题。</p>-->
            <!--                <div class="mt-4 flex flex-wrap gap-2">-->
            <!--                    <template v-if="insightsLoading">-->
            <!--                        <span class="text-text-muted text-xs">加载中...</span>-->
            <!--                    </template>-->
            <!--                    <template v-else>-->
            <!--                        <button v-for="t in hotTags" :key="t.tag" type="button"-->
            <!--                            class="rounded-full bg-surface-variant px-3 py-1 text-xs font-medium text-text-secondary transition hover:bg-primary/10 hover:text-primary"-->
            <!--                            @click="handleSelectTag(t.tag)">-->
            <!--                            #{{ t.tag }}-->
            <!--                        </button>-->
            <!--                        <div v-if="!hotTags.length" class="text-xs text-text-muted">等待新的标签更新...</div>-->
            <!--                    </template>-->
            <!--                </div>-->
            <!--            </div>-->
        </section>

        <section class="space-y-5">
            <!--            <div class="flex flex-wrap items-center justify-between gap-4">
                <div class="space-y-1">
                    <h2 class="text-2xl font-semibold text-text">最新</h2>
                    <p class="text-sm text-text-secondary">
                        来自你的订阅源
                    </p>
                </div>
                <button
                    class="inline-flex items-center gap-2 rounded-full border border-primary/20 px-4 py-2 text-sm font-semibold text-primary transition hover:bg-primary/10"
                    @click="refresh">
                    <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
                        <path stroke-linecap="round" stroke-linejoin="round"
                            d="M4.5 8.5A5.5 5.5 0 0 1 10 3a5.5 5.5 0 0 1 4.75 2.75M15.5 11.5A5.5 5.5 0 0 1 10 17a5.5 5.5 0 0 1-4.75-2.75" />
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15.5 5.75V3h-2.75M4.5 14.25V17h2.75" />
                    </svg>
                    刷新列表
                </button>
            </div>-->

            <div v-if="articlesLoading"
                class="grid place-items-center rounded-3xl border border-outline/20 bg-surface-container py-20 text-sm text-text-muted">
                正在加载文章...
            </div>

            <div v-else class="space-y-3 sm:space-y-4">
                <ArticleList :items="items" @select="handleSelect" @select-tag="handleSelectTag" />
                <p v-if="articleError" class="text-sm text-danger">{{ articleError }}</p>
            </div>

            <div
                class="flex items-center justify-between rounded-2xl border border-outline/20 bg-surface px-3 py-2 text-sm text-text-secondary sm:px-4 sm:py-3">
                <button
                    class="rounded-full border border-outline/40 px-2.5 py-1.5 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/30 disabled:text-text-muted disabled:opacity-70 sm:px-3 sm:py-2"
                    :disabled="!hasPrevious" @click="prevPage">
                    上一页
                </button>
                <span>第 {{ currentPage }} 页</span>
                <button
                    class="rounded-full border border-outline/40 px-2.5 py-1.5 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/30 disabled:text-text-muted disabled:opacity-70 sm:px-3 sm:py-2"
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
// @ts-ignore: Vue SFC default export typing
import ArticleList from '../components/ArticleList.vue';
import { useArticlesStore } from '../stores/articles';
import { useCollectionsStore } from '../stores/collections';
import { useSubscriptionsStore } from '../stores/subscriptions';

// 使用 articles store 提供的 fetchInsights
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


const quickFilters = computed(() => {
    const filters: Array<{ label: string; value: string | null }> = [{ label: '全部', value: null }];
    const tagMap = new Map<string, string>();
    for (const article of items.value) {
        if (!article || !Array.isArray(article.tags)) {
            continue;
        }
        for (const tag of article.tags) {
            if (!tag) continue;
            const normalized = tag.toLowerCase();
            if (!tagMap.has(normalized)) {
                tagMap.set(normalized, tag);
            }
            if (tagMap.size >= 11) break;
        }
        if (tagMap.size >= 11) break;
    }
    tagMap.forEach((label, value) => filters.push({ label, value }));
    return filters;
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

const buildQuery = (overrides?: { page?: number; tags?: string | null; category?: string | null }) => {
    const query: Record<string, string> = {};
    const hasTagOverride = overrides && Object.prototype.hasOwnProperty.call(overrides, 'tags');
    const tag = hasTagOverride ? overrides?.tags ?? null : activeTag.value;
    if (tag) query.tags = tag;
    const hasCategoryOverride = overrides && Object.prototype.hasOwnProperty.call(overrides, 'category');
    const category = hasCategoryOverride ? overrides?.category ?? null : (route.query.category as string | null);
    if (category) query.category = category;
    const nextPage = overrides?.page ?? routePage.value;
    if (nextPage > 1) query.page = String(nextPage);
    return query;
};

const loadData = async () => {
    const tasks: Promise<unknown>[] = [];
    if (!collectionItems.value.length) tasks.push(collectionsStore.fetchCollections());
    if (!subscriptionItems.value.length) tasks.push(subscriptionsStore.fetchSubscriptions());

    tasks.push(
        articlesStore.fetchArticles({
            size: 20,
            page: routePage.value,
            tags: activeTag.value,
            feedId: null,
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

// 已移除 SSE / AI 摘要相关代码

const refresh = async () => {
    await loadData();
};

const navigateToPage = (target: number) => {
    if (target < 1) return;
    router.push({ name: 'feedsSubscriptions', query: buildQuery({ page: target }) });
};

const nextPage = () => {
    if (!hasNext.value) return;
    navigateToPage(routePage.value + 1);
};

const prevPage = () => {
    if (!hasPrevious.value) return;
    navigateToPage(Math.max(1, routePage.value - 1));
};

const handleSelect = (articleId: string) => {
    articlesStore.recordHistory(articleId);
    router.push({ name: 'article-detail', params: { id: articleId } });
};

const clearTagFilter = () => {
    router.push({ name: 'feedsSubscriptions', query: buildQuery({ tags: null, page: 1 }) });
};

const handleSelectTag = (tag: string) => {
    if (!tag) return;
    router.push({ name: 'feedsSubscriptions', query: buildQuery({ page: 1, tags: tag.toLowerCase() }) });
};

const handleSelectCategory = (category: string) => {
    if (!category) return;
    router.push({ name: 'feedsSubscriptions', query: buildQuery({ page: 1, category: category.toLowerCase(), tags: null }) });
};

const clearCategoryFilter = () => {
    router.push({ name: 'feedsSubscriptions', query: buildQuery({ page: 1, category: null }) });
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
    () => [routePage.value, activeTag.value, route.query.category],
    () => {
        loadData();
    }
);
</script>
