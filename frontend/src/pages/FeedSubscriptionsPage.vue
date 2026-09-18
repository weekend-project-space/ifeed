<template>
  <div class="mx-auto">

    <!-- 文章列表 -->
    <main>
      <!-- 错误提示 -->
      <div
          v-if="articleError"
          class="mt-6 p-4 rounded-lg bg-red-50 border border-red-200"
      >
        <p class="text-sm text-red-800 font-medium mb-2">请求出错：{{ articleError }}</p>
        <button
            type="button"
            class="px-4 py-2 text-sm font-medium rounded-md bg-red-600 text-white hover:bg-red-700 transition-colors"
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

          <!-- 分类筛选 -->
          <header class="mb-6 max-w-screen-lg mx-auto">
            <div class="flex items-center gap-1.5 overflow-x-auto pb-2">
              <button
                  class="px-3 py-1 text-sm font-medium rounded-lg transition-colors whitespace-nowrap"
                  :class="!currentCategory ? 'bg-secondary text-secondary-foreground' : 'bg-secondary/5 text-secondary hover:bg-secondary/20'"
                  @click="clearCategoryFilter"
              >
                全部
              </button>

              <template v-if="insightsLoading">
                <span class="text-xs text-gray-500 px-3">加载中...</span>
              </template>
              <template v-else>
                <button
                    v-for="c in topCategories"
                    :key="c.category"
                    class="px-3 py-1 text-sm font-medium rounded-lg transition-colors whitespace-nowrap"
                    :class="currentCategory === c.category.toLowerCase() ? 'bg-secondary text-secondary-foreground' : 'bg-secondary/5 text-secondary hover:bg-secondary/20'"
                    @click="handleSelectCategory(c.category)"
                >
                  {{ c.category }} <small v-if="c.category=='Today'">({{ c.count }})</small>
                </button>
                <span v-if="!topCategories.length" class="text-sm text-gray-500 px-3">暂无分类</span>
              </template>
            </div>

            <!-- 标签筛选提示 -->
            <div v-if="activeTag" class="mt-3">
              <div
                  class="inline-flex items-center gap-2 px-3 py-1 text-xs bg-secondary/10 rounded-lg border border-secondary/20">
                <span class="text-secondary">#{{ activeTag }}</span>
                <button
                    class="text-secondary hover:text-secondary/80 font-medium"
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
              class="p-2 text-sm text-primary font-medium rounded-lg transition-colors  hover:bg-surface-container"
              to="/feeds/channels">所有订阅
          </router-link>
        </template>
        <template #empty>
          <div
              class="w-16 h-16 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center">
            <svg class="w-8 h-8 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path
                  d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
            </svg>
          </div>
          <h2 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">还没有订阅</h2>
          <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">添加你感兴趣的订阅源开始使用</p>
          <router-link
              to="/discover"
              class="inline-flex items-center gap-2 px-5 py-2 text-sm font-medium text-white bg-secondary hover:bg-secondary/90 rounded-full transition-colors"
          >
            添加订阅
          </router-link>
        </template>
      </article-list>

      <div v-if="items.length" class="mt-6 flex flex-col items-center gap-3" aria-live="polite">
        <div v-if="isFetchingNextPage" class="text-sm text-text-secondary">正在加载更多文章...</div>
        <template v-else-if="nextPageError">
          <p class="text-sm text-red-700">加载更多失败，请重试。</p>
          <button
              type="button"
              class="px-4 py-2 text-sm font-medium rounded-md bg-red-600 text-white hover:bg-red-700 transition-colors"
              @click="loadMore"
          >
            重试
          </button>
        </template>
        <p v-else-if="!hasNextPage" class="text-sm text-text-muted">已加载全部文章</p>
      </div>
      <div ref="loadMoreSentinel" class="h-1" aria-hidden="true"></div>
    </main>
  </div>
</template>

<script setup lang="ts">
import {computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {useMutation} from '@tanstack/vue-query';
import {useSubscriptionsStore} from "../stores/subscriptions";
import {useReadFeedStore} from "../stores/readfeed";
import {normalizeArticle} from '../stores/articles/types';
import {
  useInfiniteSubscriptionArticlesQuery,
  useSubscriptionInsightsQuery,
} from '../queries/subscriptionArticles';

defineOptions({name: 'FeedSubscriptionsPage'});

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

// 当前标签
const activeTag = computed(() => props.tags);

// 当前分类
const currentCategory = computed(() => props.category);

// 当前订阅源ID
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
const topCategories = computed(() => insightsQuery.data.value?.categories.slice(0,10) ?? []);
const loadMoreSentinel = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

// 构建查询参数
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

// 标签筛选
const handleSelectTag = (tag: string) => {
  if (!tag) return;
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({tags: tag.toLowerCase()})
  });
};

const clearTagFilter = () => {
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({tags: null})
  });
};


// 分类筛选
const handleSelectCategory = (category: string) => {
  if (!category) return;
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({category: category.toLowerCase(), tags: null})
  });
};

const clearCategoryFilter = () => {
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({category: null})
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
}

watch([currentFeedId, () => subscriptionsStore.items], () => {
  void readFeed();
}, {immediate: true});

const loadMore = async () => {
  if (!hasNextPage.value || isFetchingNextPage.value) return;
  try {
    await articlesQuery.fetchNextPage();
  } catch {
    // 错误状态由 Query 暴露，页面提供重试入口。
  }
};

const observeSentinel = () => {
  observer?.disconnect();
  observer = null;
  if (!loadMoreSentinel.value) return;
  observer = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting) {
      void loadMore();
    }
  }, {rootMargin: '600px 0px'});
  observer.observe(loadMoreSentinel.value);
};

const disconnectObserver = () => {
  observer?.disconnect();
  observer = null;
};

const activateObserver = () => {
  if (!observer) observeSentinel();
};

watch(loadMoreSentinel, observeSentinel);
onMounted(activateObserver);
onActivated(activateObserver);
onDeactivated(disconnectObserver);
onBeforeUnmount(disconnectObserver);
</script>
