<template>
    <div class="space-y-6">
        <div class="rounded-3xl border border-outline/20 bg-surface-container px-7 py-6">
            <div class="flex flex-wrap items-center justify-between gap-4">
                <div class="mb-2 space-y-1">
                    <h2 class="text-2xl font-semibold text-text">
                        <template v-if="hasQuery">搜索 “{{ searchQuery }}” 的结果</template>
                        <template v-else>搜索</template>
                    </h2>
<!--                    <p class="text-sm text-text-secondary">
                        <template v-if="hasQuery">共 {{ searchTotalText }}，当前第 {{ currentPage }} 页。</template>
                        <template v-else>输入关键词开始搜索。</template>
                    </p>-->
                </div>
                <div class="flex flex-wrap items-center gap-3 text-sm">
                    <div
                        class="flex rounded-full border border-outline/20 bg-surface-variant px-1 py-1 text-text-muted">
                        <button type="button" class="rounded-full px-4 py-1 transition"
                            :class="searchType === 'keyword' ? 'bg-primary/15 text-primary' : 'text-text-muted'"
                            @click="setSearchType('keyword')">
                            关键词匹配
                        </button>
                        <button type="button" class="rounded-full px-4 py-1 transition"
                            :class="searchType === 'semantic' ? 'bg-primary/15 text-primary' : 'text-text-muted'"
                            @click="setSearchType('semantic')">
                            语义匹配
                        </button>
                    </div>
                    <button class="text-sm font-medium text-primary transition hover:underline" @click="goBackToHome">
                        返回推荐
                    </button>
                </div>
            </div>

            <div v-if="!hasQuery" class="py-20 text-center text-text-muted">输入关键词开始搜索。</div>
            <div v-else-if="searchLoading" class="py-20 text-center text-text-muted">正在搜索...</div>
            <div v-else class="space-y-4">
                <ArticleList :items="searchArticleItems" empty-message="未找到相关结果，换个关键词试试。" @select="handleSelect"
                    @select-tag="handleSelectTag" />
                <p v-if="searchError" class="text-sm text-danger">{{ searchError }}</p>
            </div>

            <div
                class="mt-6 flex items-center justify-between rounded-2xl border border-outline/20 bg-surface px-4 py-3 text-sm text-text-secondary">
                <button
                    class="rounded-full border border-outline/40 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/30 disabled:text-text-muted disabled:opacity-70"
                    :disabled="!hasQuery || !hasPrevious" @click="prevPage">
                    上一页
                </button>
                <span>第 {{ currentPage }} 页</span>
                <button
                    class="rounded-full border border-outline/40 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/30 disabled:text-text-muted disabled:opacity-70"
                    :disabled="!hasQuery || !hasNext" @click="nextPage">
                    下一页
                </button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import ArticleList from '../components/ArticleList.vue';
import { useArticlesStore } from '../stores/articles';
import { useSearchStore, type SearchType } from '../stores/search';

const router = useRouter();
const route = useRoute();
const articlesStore = useArticlesStore();
const searchStore = useSearchStore();

const {
    results,
    page,
    hasNextPage,
    hasPreviousPage,
    total,
    loading: searchLoading,
    error: searchError
} = storeToRefs(searchStore);


const searchQuery = computed(() => {
    const q = route.query.q;
    return typeof q === 'string' ? q.trim() : '';
});

const searchType = computed<SearchType>(() => {
    const type = route.query.type;
    return type === 'semantic' ? 'semantic' : 'keyword';
});

const routePage = computed(() => {
    const raw = Array.isArray(route.query.page) ? route.query.page[0] : route.query.page;
    const parsed = Number(raw);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
});

const hasQuery = computed(() => Boolean(searchQuery.value));

const searchArticleItems = computed(() =>
    results.value.map((item) => ({
        id: item.id,
        title: item.title ?? '未命名文章',
        summary: item.summary ?? '暂无摘要',
        thumbnail: item.thumbnail,
        feedTitle: item.feedTitle,
        timeAgo: item.timeAgo,
        tags: [] as string[],
    }))
);

const currentPage = computed(() => page.value);
const hasNext = computed(() => hasNextPage.value);
const hasPrevious = computed(() => hasPreviousPage.value);
const searchTotalText = computed(() => `${total.value ?? 0}+条结果`);

const buildSearchQuery = (overrides?: { page?: number; type?: SearchType }) => {
    const query: Record<string, string> = {};
    if (searchQuery.value) {
        query.q = searchQuery.value;
    }
    const nextType = overrides?.type ?? searchType.value;
    if (nextType !== 'keyword') {
        query.type = nextType;
    }
    const nextPage = overrides?.page ?? routePage.value;
    if (nextPage > 1) {
        query.page = String(nextPage);
    }
    const feedId = typeof route.query.feedId === 'string' ? route.query.feedId : undefined;
    if (feedId) {
        query.feedId = feedId;
    }
    const tag = typeof route.query.tags === 'string' ? route.query.tags : undefined;
    if (tag) {
        query.tags = tag;
    }
    const category = typeof route.query.category === 'string' ? route.query.category : undefined;
    if (category) {
        query.category = category;
    }
    // console.log(query)
    return query;
};

const loadData = async () => {
    if (!hasQuery.value) {
        searchStore.clear();
        return;
    }

    const tasks: Promise<unknown>[] = [];

    tasks.push(
        searchStore.searchArticles({
            query: searchQuery.value,
            page: routePage.value,
            type: searchType.value
        })
    );

    try {
        await Promise.all(tasks);
    } catch (err) {
        console.warn('搜索数据加载失败', err);
    }
};

const navigateToPage = (target: number) => {
    if (target < 1 || !hasQuery.value) {
        return;
    }
    router.push({ name: 'search', query: buildSearchQuery({ page: target }) });
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

const setSearchType = (type: SearchType) => {
    if (type === searchType.value) {
        return;
    }
    router.push({ name: 'search', query: buildSearchQuery({ page: 1, type }) });
};

const goBackToHome = () => {
    const query: Record<string, string> = {};
    const feedId = typeof route.query.feedId === 'string' ? route.query.feedId : undefined;
    if (feedId) {
        query.feedId = feedId;
    }
    const tag = typeof route.query.tags === 'string' ? route.query.tags : undefined;
    if (tag) {
        query.tags = tag;
    }
    const category = typeof route.query.category === 'string' ? route.query.category : undefined;
    if (category) {
        query.category = category;
    }
    router.push({ name: 'home', query });
};

const handleSelect = (articleId: string) => {
    articlesStore.recordHistory(articleId);
    router.push({ name: 'article-detail', params: { id: articleId } });
};

const handleSelectTag = (tag: string) => {
    if (!tag) {
        return;
    }
    router.push({ name: 'home', query: { tags: tag.toLowerCase() } });
};

watch(
    () => [searchQuery.value, searchType.value, routePage.value],
    () => {
        loadData();
    }
);

onMounted(() => {
    loadData();
});
</script>
