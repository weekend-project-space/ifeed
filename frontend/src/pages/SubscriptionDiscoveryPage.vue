<template>
  <div class="min-h-screen ">
    <div class="max-w-screen-lg mx-auto px-4 sm:px-6 py-8">
      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold text-text mb-2">
          发现订阅源
        </h1>
        <p class="text-sm text-text-secondary">
          浏览并订阅你感兴趣的内容源
        </p>
      </div>

      <!-- Search Bar -->
      <div class="mb-8">
        <div class="flex items-center gap-3">
          <div class="relative flex-1 max-w-2xl">
            <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"
                 viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
            <input
                v-model="searchQuery"
                @input="scheduleSearch"
                type="text"
                placeholder="搜索订阅源..."
                class="w-full pl-12 pr-4 py-3 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-full border border-gray-200 dark:border-gray-700 focus:outline-none focus:ring-2 focus:ring-secondary focus:border-transparent transition-shadow"
            />
          </div>

          <!-- Manual Add Button -->
          <button
              @click="showManualAddDialog = true"
              class="px-4 py-3 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-200 dark:border-gray-700 rounded-full hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors flex items-center gap-2 whitespace-nowrap"
              title="手动添加订阅源"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"/>
            </svg>
            <span class="hidden sm:inline">手动添加</span>
          </button>

          <!-- OPML Import Button -->
          <button
              @click="showOpmlDialog = true"
              class="px-4 py-3 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-200 dark:border-gray-700 rounded-full hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors flex items-center gap-2 whitespace-nowrap"
              title="导入 OPML 文件"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            <span class="hidden sm:inline">导入 OPML</span>
          </button>
        </div>
      </div>

      <!-- Categories -->
      <div class="mb-8">
        <div class="flex items-center gap-2 mb-4">
          <svg class="w-5 h-5 text-gray-700 dark:text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2">
            <rect x="3" y="3" width="7" height="7" rx="1"/>
            <rect x="14" y="3" width="7" height="7" rx="1"/>
            <rect x="14" y="14" width="7" height="7" rx="1"/>
            <rect x="3" y="14" width="7" height="7" rx="1"/>
          </svg>
          <h2 class="text-lg font-semibold text-text">
            分类
          </h2>
        </div>

        <div class="flex flex-wrap gap-2">
          <button
              v-for="category in categories"
              :key="category.id"
              @click="selectCategory(category.id)"
              :class="[
                selectedCategory === category.id
                  ? 'bg-secondary text-white shadow-md border'
                  : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 border border-gray-200 dark:border-gray-700',
                'px-4 py-2 rounded-full text-sm font-medium transition-all duration-200'
              ]"
          >
            <span class="flex items-center gap-2">
              <span>{{ category.icon }}</span>
              <span>{{ category.name }}</span>
            </span>
          </button>
        </div>
      </div>

      <!-- Feed Grid -->
      <div>
        <div v-if="categoriesQuery.isError.value" class="mb-4 text-sm text-red-700" role="alert">
          分类加载失败。<button type="button" class="underline" @click="categoriesQuery.refetch()">重试</button>
        </div>
        <div v-if="feedError" class="mb-4 text-sm text-red-700" role="alert">
          订阅源加载失败。<button type="button" class="underline" @click="feedsQuery.refetch()">重试</button>
        </div>
        <p v-if="subscriptionError" class="mb-4 text-sm text-red-700" role="alert">{{ subscriptionError }}</p>
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-lg font-semibold text-text">
            {{ selectedCategoryName }}
          </h2>
          <div class="flex items-center gap-2">
            <button
                v-for="view in viewModes"
                :key="view.id"
                @click="viewMode = view.id"
                :class="[
                  viewMode === view.id
                    ? 'text-secondary bg-surface-container' : 'text-gray-500',
                  'p-2 text-sm font-medium rounded-full transition-colors whitespace-nowrap hover:bg-surface-container',
                ]"
                :title="view.name"
            >
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path v-if="view.id === 'grid'" d="M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z"/>
                <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8h16M4 16h16"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="grid"
             :class="viewMode === 'grid' ? 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6' : 'grid-cols-1 gap-4'">
          <div v-for="i in 6" :key="i" class="bg-white dark:bg-gray-800 rounded-2xl p-6 animate-pulse">
            <div class="flex items-start gap-4">
              <div class="w-10 h-10 bg-gray-200 dark:bg-gray-700 rounded-xl flex-shrink-0"></div>
              <div class="flex-1 space-y-3">
                <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
                <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-full"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Grid View -->
        <div v-else-if="viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <article
              v-for="feed in feeds"
              :key="feed.feedId"
              class="flex items-center gap-3"
          >
              <!-- Feed 信息（点击跳转详情） -->
              <router-link :to="'/feeds/' + feed.feedId" class="flex items-center gap-3 flex-1 min-w-0">
                <img
                    :src="feed.favicon"
                    :alt="feed.name"
                    class="w-10 h-10 rounded-full object-cover bg-gray-100 dark:bg-gray-700 flex-shrink-0 p-1"
                    @error="handleImageError"
                />
                <div class="flex-1 min-w-0">
                  <h3 class="text-sm font-medium text-gray-900 dark:text-gray-100 truncate group-hover:text-secondary transition-colors">
                    {{ feed.name }}
                  </h3>
                  <p class="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">
                    {{ feed.description || feed.url }}
                  </p>
                </div>
              </router-link>

              <!-- 图标化微型按钮：仅 32x32px 圆形 -->
            <button
                @click.stop.prevent="toggleSubscribe(feed)"
                :disabled="subscribing.has(feed.feedId)"
                :title="feed.subscribed ? '取消订阅' : '订阅'"
                :class="[
      feed.subscribed
        ? 'bg-gray-100 text-gray-500 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-gray-700'
        : 'bg-zinc-950 text-white hover:bg-zinc-800 dark:bg-white dark:text-zinc-950 dark:hover:bg-zinc-200 shadow-sm',
      'w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 transition-all duration-200 disabled:opacity-50'
    ]"
            >
              <!-- 加载状态 -->
              <svg v-if="subscribing.has(feed.feedId)" class="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
              </svg>

              <!-- 已订阅状态（深色背景 + 勾选标） -->
              <svg v-else-if="feed.subscribed" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>

              <!-- 未订阅状态（深色背景 + 加号） -->
              <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </button>

          </article>
        </div>

        <!-- List View -->
        <div v-else class="space-y-3">
          <article
              v-for="feed in feeds"
              :key="feed.feedId"
              class="group py-3 sm:py-4 transition-colors hover:bg-gray-50/60 dark:hover:bg-gray-800/40"
          >
            <div class="flex items-center gap-3 sm:gap-4">
              <!-- 1. 头像 -->
              <router-link :to="'/feeds/' + feed.feedId" class="flex-shrink-0">
                <img
                    :src="feed.favicon"
                    :alt="feed.name"
                    class="w-12 h-12 rounded-full object-cover bg-gray-100 dark:bg-gray-700 flex-shrink-0 p-1"
                    @error="handleImageError"
                />
              </router-link>

              <!-- 2. 标题与描述 -->
              <router-link :to="'/feeds/' + feed.feedId" class="flex-1 min-w-0">
                <h3 class="text-sm sm:text-base font-semibold text-gray-900 dark:text-gray-100 truncate group-hover:text-secondary transition-colors">
                  {{ feed.name }}
                </h3>
                <p class="text-xs sm:text-sm text-gray-500 dark:text-gray-400 line-clamp-1 mt-0.5">
                  {{ feed.description || feed.url }}
                </p>
              </router-link>

              <!-- 3. 订阅按钮 -->
              <button
                  @click="toggleSubscribe(feed)"
                  :disabled="subscribing.has(feed.feedId)"
                  :class="[
        feed.subscribed
          ? 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
          : 'bg-secondary text-white hover:bg-secondary/90',
        'px-3 py-1.5 sm:px-4 sm:py-2 text-xs sm:text-sm rounded-full font-medium flex-shrink-0 transition-colors disabled:opacity-50'
      ]"
              >
      <span v-if="subscribing.has(feed.feedId)" class="flex items-center gap-1">
        <svg class="w-3.5 h-3.5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
        </svg>
        <span>...</span>
      </span>
                <span v-else>
        {{ feed.subscribed ? '已订阅' : '订阅' }}
      </span>
              </button>
            </div>
          </article>
        </div>

        <!-- Empty State -->
        <div v-if="!loading && !feedError && feeds.length === 0" class="text-center py-16">
          <div
              class="w-20 h-20 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center">
            <svg class="w-10 h-10 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
          </div>
          <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">未找到订阅源</h3>
          <p class="text-gray-600 dark:text-gray-400">尝试选择其他分类或调整搜索关键词</p>
        </div>
      </div>

      <div v-if="feeds.length" class="mt-8 flex flex-col items-center gap-3" aria-live="polite">
        <p v-if="isFetchingNextPage" class="text-sm text-text-secondary">正在加载更多订阅源...</p>
        <template v-else-if="nextPageError">
          <p class="text-sm text-red-700">加载更多失败，请重试。</p>
          <button type="button" class="px-4 py-2 text-sm rounded-md bg-red-600 text-white" @click="loadMore">重试</button>
        </template>
        <p v-else-if="!hasNextPage" class="text-sm text-text-muted">已加载全部订阅源</p>
      </div>
      <div ref="loadMoreSentinel" class="h-1" aria-hidden="true"></div>
    </div>

    <!-- Manual Add Dialog -->
    <transition name="modal">
      <div
          v-if="showManualAddDialog"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40"
          @click.self="showManualAddDialog = false"
      >
        <div class="relative w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow-2xl">
          <!-- Dialog Header -->
          <div class="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">手动添加订阅源</h3>
            <button
                @click="showManualAddDialog = false"
                class="p-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-all"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <!-- Dialog Content -->
          <div class="p-6">
            <SubscriptionsAddManual @success="showManualAddDialog = false"/>
          </div>
        </div>
      </div>
    </transition>

    <!-- OPML Import Dialog -->
    <transition name="modal">
      <div
          v-if="showOpmlDialog"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40"
          @click.self="showOpmlDialog = false"
      >
        <div class="relative w-full max-w-2xl bg-white dark:bg-gray-800 rounded-2xl shadow-2xl">
          <!-- Dialog Header -->
          <div class="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">导入 OPML 文件</h3>
            <button
                @click="showOpmlDialog = false"
                class="p-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-all"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <!-- Dialog Content -->
          <div class="p-6">
            <SubscriptionsAddOpml @success="showOpmlDialog = false"/>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import {computed, onActivated, onBeforeUnmount, onDeactivated, ref, watch} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {useDiscoveryCategoriesQuery, useInfiniteDiscoveryFeedsQuery} from '@/queries/discovery';
import {useSubscriptionMutation} from '@/queries/subscriptions';
import {useInfiniteScroll} from '@/composables/infiniteScroll';
import SubscriptionsAddManual from './components/SubscriptionsAddManual.vue';
import SubscriptionsAddOpml from './components/SubscriptionsAddOpml.vue';

defineOptions({ name: 'SubscriptionDiscoveryPage' });
const props = defineProps<{ query: string; category: string }>();
const router = useRouter();
const route = useRoute();
const active = computed(() => route.name === 'discover');
const searchQuery = ref(props.query);
const selectedCategory = computed(() => props.category);
const viewMode = ref<'grid' | 'list'>('grid');
const showManualAddDialog = ref(false);
const showOpmlDialog = ref(false);
const categoriesQuery = useDiscoveryCategoriesQuery();
const feedsQuery = useInfiniteDiscoveryFeedsQuery(
  computed(() => props.query), selectedCategory, active,
);
const categories = computed(() => categoriesQuery.data.value?.categories ?? []);
const feeds = computed(() => {
  const items = feedsQuery.data.value?.pages.flatMap(page => page.content) ?? [];
  return [...new Map(items.map(feed => [feed.feedId, feed])).values()];
});
const loading = feedsQuery.isPending;
const hasNextPage = feedsQuery.hasNextPage;
const isFetchingNextPage = feedsQuery.isFetchingNextPage;
const nextPageError = feedsQuery.isFetchNextPageError;
const feedError = computed(() => feedsQuery.isError.value && !nextPageError.value);
const selectedCategoryName = computed(() =>
  categories.value.find(category => category.id === selectedCategory.value)?.name ?? '全部',
);
const viewModes = [
  {id: 'grid', name: '网格视图'},
  {id: 'list', name: '列表视图'},
];

const subscriptionMutation = useSubscriptionMutation();
const subscribing = ref(new Set<string>());
const subscriptionError = ref('');
const toggleSubscribe = async (feed: typeof feeds.value[number]) => {
  if (subscribing.value.has(feed.feedId)) return;
  subscribing.value.add(feed.feedId);
  subscriptionError.value = '';
  try {
    await subscriptionMutation.mutateAsync({
      feedId: feed.feedId, feedUrl: feed.url, subscribed: feed.subscribed,
    });
  } catch (error) {
    subscriptionError.value = error instanceof Error ? error.message : '订阅操作失败，请重试';
  } finally {
    subscribing.value.delete(feed.feedId);
  }
};

let searchTimeout: ReturnType<typeof setTimeout> | undefined;
const cancelSearch = () => {
  clearTimeout(searchTimeout);
  searchTimeout = undefined;
};
const navigateFilters = (query: string, category: string) => {
  cancelSearch();
  if (query === props.query && category === props.category) return;
  void router.push({
    name: 'discover',
    query: { q: query || undefined, category: category === 'all' ? undefined : category },
  });
};
const selectCategory = (category: string) => navigateFilters(searchQuery.value.trim(), category);
const scheduleSearch = () => {
  cancelSearch();
  searchTimeout = setTimeout(() => {
    if (active.value) navigateFilters(searchQuery.value.trim(), props.category);
  }, 500);
};
watch(() => [props.query, props.category], () => {
  cancelSearch();
  searchQuery.value = props.query;
});
watch(active, (value) => {
  if (!value) cancelSearch();
}, { flush: 'sync' });

const {sentinel: loadMoreSentinel, loadMore} = useInfiniteScroll({
  enabled: active,
  hasNextPage,
  isLoading: feedsQuery.isFetching,
  hasError: feedsQuery.isError,
  onLoadMore: () => feedsQuery.fetchNextPage(),
});
onActivated(() => {
  searchQuery.value = props.query;
});
onDeactivated(cancelSearch);
onBeforeUnmount(cancelSearch);

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement;
  img.onerror = null;
  img.src = '/logo.svg';
};
</script>

<style scoped>
.modal-enter-active, .modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from, .modal-leave-to {
  opacity: 0;
}
</style>
