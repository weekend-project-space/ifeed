<template>
  <div class="space-y-10">
    <template v-if="!isSearching">
      <section class="grid gap-6 md:grid-cols-3">
        <div
          class="relative overflow-hidden rounded-3xl border border-primary/20 bg-surface p-7 shadow-md-elevated transition md:col-span-2">
          <div class="pointer-events-none absolute -left-16 top-4 h-44 w-44 rounded-full bg-primary/10 blur-3xl"></div>
          <div
            class="pointer-events-none absolute -right-24 bottom-[-40px] h-56 w-56 rounded-full bg-primary/10 blur-3xl">
          </div>
          <div class="relative flex flex-col gap-6">
            <div class="space-y-3">
              <span
                class="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.3em] text-primary">
                <span class="h-1.5 w-1.5 rounded-full bg-primary"></span>
                今日精选
              </span>
              <h2 class="text-3xl font-semibold leading-tight text-text">
                {{ highlight.title }}
              </h2>
              <p class="text-sm text-text-secondary">
                {{ highlight.description }}
              </p>
            </div>
            <div
              class="rounded-2xl border border-primary/10 bg-surface-container/90 p-4 text-sm leading-relaxed text-text-secondary shadow-inner">
              {{ highlight.summary }}
            </div>
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div class="flex flex-wrap gap-2 text-xs text-primary">
                <button v-for="tag in highlight.tags" :key="tag" type="button"
                  class="rounded-full bg-primary/10 px-3 py-1 font-medium transition hover:bg-primary/20"
                  @click="handleSelectTag(tag)">
                  #{{ tag }}
                </button>
              </div>
              <button v-if="highlight.id"
                class="inline-flex items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition hover:-translate-y-0.5 hover:shadow"
                @click="handleSelect(highlight.id)">
                开始阅读
                <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7 4l6 6-6 6" />
                </svg>
              </button>
            </div>
          </div>
        </div>
        <div
          class="flex flex-col justify-between rounded-3xl border border-primary/30 bg-gradient-to-br from-primary/90 via-primary/70 to-primary/60 p-6 text-primary-foreground shadow-lg">
          <div class="space-y-2">
            <h3 class="text-lg font-semibold">今日阅读进度</h3>
            <p class="text-sm text-primary-foreground/80">你已收藏 {{ stats.savedCount }} 篇文章。</p>
          </div>
          <div class="mt-6 space-y-4">
            <div class="text-4xl font-bold tracking-tight">{{ stats.readGoalPercent }}%</div>
            <div class="flex items-center gap-3">
              <div class="h-2 flex-1 overflow-hidden rounded-full bg-primary-foreground/25">
                <div class="h-full rounded-full bg-primary-foreground" :style="{ width: `${stats.readGoalPercent}%` }">
                </div>
              </div>
              <span class="text-xs font-medium text-primary-foreground/80">目标 8 篇</span>
            </div>
            <p v-if="stats.remaining > 0" class="text-xs text-primary-foreground/70">距离完成还差 {{ stats.remaining }}
              篇，继续加油！</p>
            <p v-else class="text-xs text-primary-foreground/70">今日目标已达成，看看 AI 还推荐了什么。</p>
          </div>
        </div>
      </section>

      <section
        class="space-y-6 rounded-3xl border border-primary/15 bg-surface p-7 shadow-md-elevated backdrop-blur-xs">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div class="space-y-1">
            <h2 class="text-2xl font-semibold text-text">最新推荐</h2>
            <p class="text-sm text-text-secondary">来自你的订阅源与 AI 智能推荐的精选文章。</p>
          </div>
          <div class="flex gap-2 text-sm text-primary">
            <button
              class="inline-flex items-center gap-2 rounded-full border border-primary/20 px-4 py-2 font-medium transition hover:bg-primary/10"
              @click="refresh">
              <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M4.5 8.5A5.5 5.5 0 0 1 10 3a5.5 5.5 0 0 1 4.75 2.75M15.5 11.5A5.5 5.5 0 0 1 10 17a5.5 5.5 0 0 1-4.75-2.75" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.5 5.75V3h-2.75M4.5 14.25V17h2.75" />
              </svg>
              刷新
            </button>
          </div>
        </div>
        <div v-if="articlesLoading" class="py-20 text-center text-text-muted">正在加载文章...</div>
        <div v-else>
          <div v-if="activeFeedInfo"
            class="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-primary/20 bg-primary/10 px-4 py-2 text-sm text-primary">
            <span>
              当前筛选：{{ activeFeedInfo.title || activeFeedInfo.siteUrl || activeFeedInfo.url }}
            </span>
            <button class="font-medium text-primary underline-offset-4 hover:underline"
              @click="clearFeedFilter">查看全部文章</button>
          </div>
          <div v-if="activeTag"
            class="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-primary/15 bg-surface-variant px-4 py-2 text-sm text-text">
            <span>标签筛选：#{{ activeTag }}</span>
            <button class="font-medium text-primary hover:underline" @click="clearTagFilter">清除标签</button>
          </div>
          <ArticleList :items="recommendedArticles" @select="handleSelect" @toggle-favorite="handleToggleFavorite"
            @select-tag="handleSelectTag" />
          <p v-if="articleError" class="mt-4 text-sm text-danger">{{ articleError }}</p>
        </div>
        <div class="flex items-center justify-between border-t border-primary/10 pt-4 text-sm text-text-secondary">
          <button
            class="rounded-full border border-primary/20 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-primary/10 disabled:text-text-muted disabled:opacity-70"
            :disabled="!hasPrevious" @click="prevPage">
            上一页
          </button>
          <span>第 {{ currentPage }} 页</span>
          <button
            class="rounded-full border border-primary/20 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-primary/10 disabled:text-text-muted disabled:opacity-70"
            :disabled="!hasNext" @click="nextPage">
            下一页
          </button>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="space-y-6 rounded-3xl border border-primary/15 bg-surface p-7 shadow-md-elevated">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div class="space-y-1">
            <h2 class="text-2xl font-semibold text-text">搜索 “{{ searchQuery }}” 的结果</h2>
            <p class="text-sm text-text-secondary">共 {{ searchTotalText }}，当前第 {{ currentPage }} 页。</p>
          </div>
          <div class="flex flex-wrap items-center gap-3 text-sm">
            <div
              class="flex rounded-full border border-primary/15 bg-surface-variant px-1 py-1 font-medium text-text-muted">
              <button type="button" class="rounded-full px-4 py-1 transition"
                :class="searchType === 'keyword' ? 'bg-surface text-text shadow' : ''"
                @click="setSearchType('keyword')">
                关键词匹配
              </button>
              <button type="button" class="rounded-full px-4 py-1 transition"
                :class="searchType === 'semantic' ? 'bg-surface text-text shadow' : ''"
                @click="setSearchType('semantic')">
                语义匹配
              </button>
            </div>
            <button class="text-sm font-medium text-primary transition hover:underline" @click="clearSearch">
              返回推荐
            </button>
          </div>
        </div>

        <div v-if="searchLoading" class="py-20 text-center text-text-muted">正在搜索...</div>
        <div v-else>
          <ArticleList :items="searchArticleItems" empty-message="未找到相关结果，换个关键词试试。" @select="handleSelect"
            @toggle-favorite="handleToggleFavorite" @select-tag="handleSelectTag" />
          <p v-if="searchError" class="mt-4 text-sm text-danger">{{ searchError }}</p>
        </div>
        <div class="flex items-center justify-between border-t border-primary/10 pt-4 text-sm text-text-secondary">
          <button
            class="rounded-full border border-primary/20 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-primary/10 disabled:text-text-muted disabled:opacity-70"
            :disabled="!hasPrevious" @click="prevPage">
            上一页
          </button>
          <span>第 {{ currentPage }} 页</span>
          <button
            class="rounded-full border border-primary/20 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-primary/10 disabled:text-text-muted disabled:opacity-70"
            :disabled="!hasNext" @click="nextPage">
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

const currentPage = computed(() => (isSearching.value ? searchPage.value : page.value));
const hasNext = computed(() => (isSearching.value ? searchHasNextPage.value : hasNextPage.value));
const hasPrevious = computed(() => (isSearching.value ? searchHasPreviousPage.value : hasPreviousPage.value));

const searchTotalText = computed(() => `${searchTotal.value ?? 0} 条结果`);

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
