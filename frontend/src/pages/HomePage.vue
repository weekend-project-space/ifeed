<template>
  <div class="space-y-8">
    <template v-if="!isSearching">
      <section class="grid gap-6 md:grid-cols-3">
        <div class="flex flex-col gap-4 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:col-span-2">
          <div>
            <p class="text-sm font-semibold text-blue-500">今日精选</p>
            <h2 class="text-2xl font-semibold leading-tight text-slate-900">
              {{ highlight.title }}
            </h2>
            <p class="text-sm text-slate-500">
              {{ highlight.description }}
            </p>
          </div>
          <div class="min-h-[88px] rounded-2xl bg-slate-50 p-4 text-sm leading-relaxed text-slate-600">
            {{ highlight.summary }}
          </div>
          <div class="flex flex-wrap gap-2 text-xs">
            <button
              v-for="tag in highlight.tags"
              :key="tag"
              type="button"
              class="rounded-full bg-blue-50 px-3 py-1 text-blue-600 hover:bg-blue-100 transition"
              @click="handleSelectTag(tag)"
            >
              #{{ tag }}
            </button>
          </div>
        </div>
        <div class="flex flex-col justify-between rounded-3xl bg-gradient-to-br from-blue-500 to-indigo-600 p-6 text-white shadow-lg">
          <div>
            <h3 class="text-lg font-semibold">今日阅读进度</h3>
            <p class="mt-2 text-sm text-blue-100">你已收藏 {{ stats.savedCount }} 篇文章。</p>
          </div>
          <div class="mt-6">
            <div class="text-4xl font-bold">{{ stats.readGoalPercent }}%</div>
            <p class="mt-1 text-sm text-blue-100">完成今日 AI 推荐阅读目标</p>
          </div>
        </div>
      </section>

      <section class="space-y-4 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 class="text-xl font-semibold text-slate-900">最新推荐</h2>
            <p class="text-sm text-slate-500">来自你的订阅源与 AI 推荐的最新文章。</p>
          </div>
          <div class="flex gap-2 text-sm text-slate-500">
            <button
              class="rounded-full border border-slate-200 px-3 py-1.5 transition hover:border-blue-400 hover:text-blue-600"
              @click="refresh"
            >
              刷新
            </button>
          </div>
        </div>
        <div v-if="articlesLoading" class="py-20 text-center text-slate-400">正在加载文章...</div>
        <div v-else>
          <div
            v-if="activeFeedInfo"
            class="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-blue-50 px-4 py-2 text-sm text-blue-700"
          >
            <span>
              当前筛选：{{ activeFeedInfo.title || activeFeedInfo.siteUrl || activeFeedInfo.url }}
            </span>
            <button class="text-blue-600 hover:underline" @click="clearFeedFilter">查看全部文章</button>
          </div>
          <div
            v-if="activeTag"
            class="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-emerald-50 px-4 py-2 text-sm text-emerald-700"
          >
            <span>标签筛选：#{{ activeTag }}</span>
            <button class="text-emerald-600 hover:underline" @click="clearTagFilter">清除标签</button>
          </div>
          <ArticleList
            :items="recommendedArticles"
            @select="handleSelect"
            @toggle-favorite="handleToggleFavorite"
            @select-tag="handleSelectTag"
          />
          <p v-if="articleError" class="mt-4 text-sm text-red-500">{{ articleError }}</p>
        </div>
        <div class="flex items-center justify-between border-t border-slate-100 pt-4 text-sm text-slate-500">
          <button
            class="rounded-lg border border-slate-200 px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="!hasPrevious.value"
            @click="prevPage"
          >
            上一页
          </button>
          <span>第 {{ currentPage }} 页</span>
          <button
            class="rounded-lg border border-slate-200 px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="!hasNext.value"
            @click="nextPage"
          >
            下一页
          </button>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="space-y-5 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 class="text-xl font-semibold text-slate-900">搜索 “{{ searchQuery }}” 的结果</h2>
            <p class="text-sm text-slate-500">共 {{ searchTotalText }}，当前第 {{ currentPage }} 页。</p>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <div class="flex rounded-full border border-slate-200 bg-slate-100 p-1 text-sm font-medium text-slate-600">
              <button
                type="button"
                class="rounded-full px-4 py-1 transition"
                :class="searchType === 'keyword' ? 'bg-white text-slate-900 shadow' : ''"
                @click="setSearchType('keyword')"
              >
                关键词匹配
              </button>
              <button
                type="button"
                class="rounded-full px-4 py-1 transition"
                :class="searchType === 'semantic' ? 'bg-white text-slate-900 shadow' : ''"
                @click="setSearchType('semantic')"
              >
                语义匹配
              </button>
            </div>
            <button class="text-sm text-slate-400 transition hover:text-slate-600" @click="clearSearch">
              返回推荐
            </button>
          </div>
        </div>

        <div v-if="searchLoading" class="py-20 text-center text-slate-400">正在搜索...</div>
        <div v-else>
          <ArticleList
            :items="searchArticleItems"
            empty-message="未找到相关结果，换个关键词试试。"
            @select="handleSelect"
            @toggle-favorite="handleToggleFavorite"
            @select-tag="handleSelectTag"
          />
          <p v-if="searchError" class="mt-4 text-sm text-red-500">{{ searchError }}</p>
        </div>
        <div class="flex items-center justify-between border-t border-slate-100 pt-4 text-sm text-slate-500">
          <button
            class="rounded-lg border border-slate-200 px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="!hasPrevious.value"
            @click="prevPage"
          >
            上一页
          </button>
          <span>第 {{ currentPage }} 页</span>
          <button
            class="rounded-lg border border-slate-200 px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="!hasNext.value"
            @click="nextPage"
          >
            下一页
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, useRoute } from 'vue-router';
import ArticleList from '../components/ArticleList.vue';
import { useArticlesStore } from '../stores/articles';
import { useCollectionsStore } from '../stores/collections';
import { useSearchStore, type SearchType } from '../stores/search';
import { useSubscriptionsStore } from '../stores/subscriptions';

const router = useRouter();
const route = useRoute();
const articlesStore = useArticlesStore();
const collectionsStore = useCollectionsStore();
const searchStore = useSearchStore();
const subscriptionsStore = useSubscriptionsStore();

const {
  items,
  page,
  hasNextPage,
  hasPreviousPage,
  total,
  loading: articlesLoading,
  error: articleError
} = storeToRefs(articlesStore);

const {
  results,
  page: searchPage,
  hasNextPage: searchHasNextPage,
  hasPreviousPage: searchHasPreviousPage,
  total: searchTotal,
  loading: searchLoading,
  error: searchError
} = storeToRefs(searchStore);

const { items: collectionItems } = storeToRefs(collectionsStore);
const { items: subscriptionItems } = storeToRefs(subscriptionsStore);

const searchQuery = computed(() => {
  const q = route.query.q;
  if (typeof q === 'string') {
    return q.trim();
  }
  return '';
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

const isSearching = computed(() => Boolean(searchQuery.value));

const recommendedArticles = computed(() =>
  items.value.map((item) => ({
    ...item,
    collected: collectionsStore.isCollected(item.id)
  }))
);

const searchArticleItems = computed(() =>
  results.value.map((item) => {
    const scoreText = typeof item.score === 'number' ? `相关度 ${(item.score * 100).toFixed(0)}%` : '相关度未知';
    return {
      id: item.id,
      title: item.title ?? '未命名文章',
      summary: item.summary ?? '暂无摘要',
      feedTitle: '搜索结果',
      timeAgo: scoreText,
      tags: [] as string[],
      collected: collectionsStore.isCollected(item.id)
    };
  })
);

const displayedArticles = computed(() => (isSearching.value ? searchArticleItems.value : recommendedArticles.value));

const highlight = computed(() => {
  const first = items.value[0];
  if (!first) {
    return {
      title: 'AI 关注你的兴趣，实时推荐高价值内容',
      description: 'AI 会根据你的阅读行为为你推送值得关注的主题。',
      summary: '开始添加订阅源，让系统为你构建专属信息流。',
      tags: ['AI 摘要', '个性化推荐', '阅读效率']
    };
  }
  return {
    title: first.title,
    description: `${first.feedTitle} · ${first.timeAgo}`,
    summary: first.summary,
    tags: first.tags.length ? first.tags : ['AI 推荐']
  };
});

const stats = computed(() => ({
  savedCount: collectionItems.value.length,
  readGoalPercent: Math.min(100, collectionItems.value.length * 10 + (total.value ?? recommendedArticles.value.length) * 2)
}));

const currentPage = computed(() => (isSearching.value ? searchPage.value : page.value));
const hasNext = computed(() => (isSearching.value ? searchHasNextPage.value : hasNextPage.value));
const hasPrevious = computed(() => (isSearching.value ? searchHasPreviousPage.value : hasPreviousPage.value));

const searchTotalText = computed(() => {
  const count = searchTotal.value ?? 0;
  return `${count} 条结果`;
});

const buildQuery = (overrides?: { page?: number; type?: SearchType; feedId?: string | null; tags?: string | null }) => {
  const query: Record<string, string> = {};
  if (isSearching.value && searchQuery.value) {
    query.q = searchQuery.value;
    const type = overrides?.type ?? searchType.value;
    if (type !== 'keyword') {
      query.type = type;
    }
  }
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

  if (isSearching.value) {
    tasks.push(
      searchStore.searchArticles({
        query: searchQuery.value,
        page: routePage.value,
        type: searchType.value
      })
    );
  } else {
    if (results.value.length) {
      searchStore.clear();
    }
    tasks.push(articlesStore.fetchArticles({ page: routePage.value, feedId: activeFeedId.value, tags: activeTag.value }));
  }

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
  const target = displayedArticles.value.find((item) => item.id === articleId);
  try {
    await collectionsStore.toggleCollection(articleId, { title: target?.title });
  } catch (err) {
    console.warn('收藏操作失败', err);
  }
};

const setSearchType = (type: SearchType) => {
  if (!isSearching.value || type === searchType.value) {
    return;
  }
  router.push({ name: 'home', query: buildQuery({ page: 1, type }) });
};

const clearSearch = () => {
  const nextQuery: Record<string, string> = {};
  if (activeFeedId.value) {
    nextQuery.feedId = activeFeedId.value;
  }
  if (activeTag.value) {
    nextQuery.tags = activeTag.value;
  }
  router.push({ name: 'home', query: nextQuery });
};

const clearFeedFilter = () => {
  router.push({ name: 'home', query: buildQuery({ feedId: null, page: 1 }) });
};

const clearTagFilter = () => {
  router.push({ name: 'home', query: buildQuery({ tags: null, page: 1 }) });
};

const handleSelectTag = (tag: string) => {
  if (!tag) {
    return;
  }
  router.push({ name: 'home', query: buildQuery({ page: 1, tags: tag.toLowerCase() }) });
};

onMounted(() => {
  loadData();
});

watch(
  () => [searchQuery.value, searchType.value, routePage.value, activeFeedId.value, activeTag.value],
  () => {
    loadData();
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
