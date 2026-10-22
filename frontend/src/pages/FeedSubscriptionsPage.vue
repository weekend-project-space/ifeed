<template>
  <div class="mx-auto text-text">
    <main>
      <!-- 错误提示 -->
      <div
          v-if="articleError"
          class="mb-6 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400"
      >
        <p class="font-semibold mb-2">请求出错：{{ articleError }}</p>
        <button
            type="button"
            class="inline-flex h-8 items-center justify-center rounded-full bg-red-600 px-4 text-xs font-semibold text-white shadow-xs transition hover:bg-red-700 active:scale-95"
            @click="refresh"
        >
          重试
        </button>
      </div>

      <article-list
          title="最新"
          :items="items"
          :loading="articlesLoading"
          @select-tag="handleSelectTag"
          @refresh="refresh"
      >
        <template #header>
          <!-- 分类筛选：标准 MD3 Filter Chips -->
          <header class="mb-6 max-w-screen-lg mx-auto">
            <div class="flex items-center gap-1.5 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              <button
                  class="inline-flex h-8 shrink-0 items-center rounded-full px-3.5 text-xs font-medium transition select-none active:scale-95"
                  :class="!currentCategory
                    ? 'bg-primary/15 font-semibold text-primary shadow-xs'
                    : 'bg-surface-container/60 text-text-secondary hover:bg-surface-container hover:text-text'"
                  @click="clearCategoryFilter"
              >
                全部
              </button>

              <template v-if="insightsLoading">
                <span class="text-xs text-text-secondary px-3">加载中...</span>
              </template>
              <template v-else>
                <button
                    v-for="c in topCategories"
                    :key="c.category"
                    class="inline-flex h-8 shrink-0 items-center gap-1 rounded-full px-3.5 text-xs font-medium transition select-none active:scale-95"
                    :class="currentCategory === c.category.toLowerCase()
                      ? 'bg-primary/15 font-semibold text-primary shadow-xs'
                      : 'bg-surface-container/60 text-text-secondary hover:bg-surface-container hover:text-text'"
                    @click="handleSelectCategory(c.category)"
                >
                  <span>{{ c.category }}</span>
                  <small v-if="c.category === 'Today'" class="opacity-75">({{ c.count }})</small>
                </button>
                <span v-if="!topCategories.length" class="text-xs text-text-secondary px-3">暂无分类</span>
              </template>
            </div>

            <!-- 标签筛选提示胶囊 -->
            <div v-if="activeTag" class="mt-3">
              <div class="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
                <span>#{{ activeTag }}</span>
                <button
                    class="hover:opacity-75 font-bold"
                    @click="clearTagFilter"
                >
                  ✕
                </button>
              </div>
            </div>
          </header>
        </template>

        <template #action>
          <router-link
              class="inline-flex h-8 items-center rounded-full bg-surface-container/60 px-3.5 text-xs font-medium text-text-secondary hover:bg-surface-container hover:text-text transition"
              to="/feed/channels">
            所有订阅
          </router-link>
        </template>

        <template #empty>
          <div class="py-16 text-center">
            <div class="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/80">
              <svg class="h-6 w-6 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
              </svg>
            </div>
            <h2 class="text-base font-bold text-text">还没有订阅</h2>
            <p class="mt-1 text-xs text-text-secondary mb-5">添加你感兴趣的订阅源开始使用</p>
            <router-link
                to="/discover"
                class="inline-flex h-9 items-center gap-1.5 rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98]"
            >
              添加订阅
            </router-link>
          </div>
        </template>
      </article-list>

      <div v-if="items.length" class="mt-8 flex flex-col items-center gap-2.5" aria-live="polite">
        <div v-if="isFetchingNextPage" class="text-xs text-text-secondary">正在加载更多文章...</div>
        <template v-else-if="nextPageError">
          <p class="text-xs text-red-600 dark:text-red-400">加载失败</p>
          <button
              type="button"
              class="inline-flex h-8 items-center rounded-full bg-surface-container px-4 text-xs font-semibold text-text hover:bg-surface-container-high transition"
              @click="loadMore"
          >
            重试
          </button>
        </template>
        <p v-else-if="!hasNextPage" class="text-xs text-text-secondary/60">已加载全部文章</p>
      </div>
      <div ref="loadMoreSentinel" class="h-1" aria-hidden="true"></div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useMutation } from '@tanstack/vue-query';
import { useSubscriptionsStore } from "../stores/subscriptions";
import { useReadFeedStore } from "../stores/readfeed";
import { normalizeArticle } from '../stores/articles/types';
import {
  useInfiniteSubscriptionArticlesQuery,
  useSubscriptionInsightsQuery,
} from '../queries/subscriptionArticles';
import { useInfiniteScroll } from '../composables/infiniteScroll';
import ArticleList from '../components/ArticleList.vue';

defineOptions({ name: 'FeedSubscriptionsPage' });

const router = useRouter();
const props = defineProps<{
  tags: string | null;
  category: string | null;
  feedId: string | null;
}>();
const subscriptionsStore = useSubscriptionsStore();
const readFeedStore = useReadFeedStore();
const readFeedMutation = useMutation({
  mutationFn: (feedId: string) => readFeedStore.recordFeedRead(feedId),
  onSuccess: () => subscriptionsStore.fetchSubscriptions(),
});
const readFeedAttempted = new Set<string>();

const activeTag = computed(() => props.tags);
const currentCategory = computed(() => props.category);
const currentFeedId = computed(() => props.feedId);

const articlesQuery = useInfiniteSubscriptionArticlesQuery(activeTag, currentCategory, currentFeedId);
const insightsQuery = useSubscriptionInsightsQuery();
const items = computed(() => articlesQuery.data.value?.pages
    .flatMap((page) => page.content)
    .map(normalizeArticle) ?? []);
const articlesLoading = computed(() => articlesQuery.isPending.value);
const isFetchingNextPage = computed(() => articlesQuery.isFetchingNextPage.value);
const hasNextPage = computed(() => Boolean(articlesQuery.hasNextPage.value));
const articleError = computed(() => {
  const error = articlesQuery.error.value;
  if (items.value.length > 0) return null;
  return error instanceof Error ? error.message : error ? '订阅文章加载失败' : null;
});
const nextPageError = computed(() => articlesQuery.isFetchNextPageError.value);
const insightsLoading = computed(() => insightsQuery.isPending.value);
const topCategories = computed(() => insightsQuery.data.value?.categories.slice(0, 10) ?? []);

const buildQuery = (overrides?: {
  tags?: string | null;
  category?: string | null;
  feedId?: string | null
}) => {
  const query: Record<string, string> = {};
  const tag = overrides?.hasOwnProperty('tags') ? overrides.tags : activeTag.value;
  if (tag) query.tags = tag;
  const category = overrides?.hasOwnProperty('category') ? overrides.category : currentCategory.value;
  if (category) query.category = category;
  const feedId = overrides?.hasOwnProperty('feedId') ? overrides.feedId : currentFeedId.value;
  if (feedId) query.feedId = feedId;
  return query;
};

const refresh = () => articlesQuery.refetch();

const handleSelectTag = (tag: string) => {
  if (!tag) return;
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({ tags: tag.toLowerCase() })
  });
};

const clearTagFilter = () => {
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({ tags: null })
  });
};

const handleSelectCategory = (category: string) => {
  if (!category) return;
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({ category: category.toLowerCase(), tags: null })
  });
};

const clearCategoryFilter = () => {
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({ category: null })
  });
};

const readFeed = async () => {
  const feedId = currentFeedId.value;
  const canRead = feedId && subscriptionsStore.items.some(item => !item.isRead && item.feedId === feedId);
  if (canRead && feedId && !readFeedMutation.isPending.value && !readFeedAttempted.has(feedId)) {
    readFeedAttempted.add(feedId);
    try {
      await readFeedMutation.mutateAsync(feedId);
    } catch {
      readFeedAttempted.delete(feedId);
    }
  }
};

watch([currentFeedId, () => subscriptionsStore.items], () => {
  void readFeed();
}, { immediate: true });

const { sentinel: loadMoreSentinel, loadMore } = useInfiniteScroll({
  hasNextPage,
  isLoading: articlesQuery.isFetching,
  hasError: articlesQuery.isError,
  onLoadMore: () => articlesQuery.fetchNextPage(),
});
</script>