<template>
  <div class="min-h-screen text-text">
    <div class="mx-auto max-w-screen-lg px-4 py-8 sm:px-6">
      <!-- 页面头部 -->
      <div class="mb-7">
        <h1 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">
          {{ tr('subscriptionDiscoveryPage.title') }}
        </h1>
        <p class="mt-1 text-xs text-text-secondary sm:text-sm">
          {{ tr('subscriptionDiscoveryPage.subtitle') }}
        </p>
      </div>

      <!-- 搜索与操作栏：MD3 药丸胶囊搜索条与 Tonal 动作按钮 -->
      <div class="mb-8">
        <div class="flex flex-wrap items-center gap-2.5 sm:gap-3">
          <div class="relative min-w-0 flex-1">
            <svg class="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-text-secondary"
                 viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" />
              <path d="m21 21-4.35-4.35" />
            </svg>
            <input
                v-model="searchQuery"
                @input="scheduleSearch"
                type="text"
                :placeholder="tr('subscriptionDiscoveryPage.searchPlaceholder')"
                class="w-full rounded-full border border-outline/20 bg-surface-container/40 py-2.5 pl-11 pr-4 text-xs text-text outline-none transition placeholder:text-text-secondary/60 focus:border-primary focus:bg-surface focus:ring-2 focus:ring-primary/20 sm:text-sm"
            />
          </div>

          <!-- 手动添加按钮：MD3 辅助动作胶囊 -->
          <button
              type="button"
              @click="showManualAddDialog = true"
              class="inline-flex h-10 items-center gap-2 rounded-full bg-surface-container/60 px-4 text-xs font-semibold text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98] sm:text-xs"
              :title="tr('subscriptionDiscoveryPage.addManuallyTitle')"
          >
            <svg class="h-4 w-4 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 5v14M5 12h14" />
            </svg>
            <span class="hidden sm:inline">{{ tr('subscriptionDiscoveryPage.addManually') }}</span>
          </button>

          <!-- OPML 导入按钮 -->
          <button
              type="button"
              @click="showOpmlDialog = true"
              class="inline-flex h-10 items-center gap-2 rounded-full bg-surface-container/60 px-4 text-xs font-semibold text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98] sm:text-xs"
              :title="tr('subscriptionDiscoveryPage.importOpmlTitle')"
          >
            <svg class="h-4 w-4 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline stroke-linecap="round" stroke-linejoin="round" points="7 10 12 15 17 10" />
              <line stroke-linecap="round" stroke-linejoin="round" x1="12" y1="15" x2="12" y2="3" />
            </svg>
            <span class="hidden sm:inline">{{ tr('subscriptionDiscoveryPage.importOpml') }}</span>
          </button>
        </div>
      </div>

      <!-- 分类筛选：标准 MD3 Filter Chips -->
      <div class="mb-8">
        <div class="mb-3 flex items-center gap-2">
          <svg class="h-4 w-4 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" rx="2" />
            <rect x="14" y="3" width="7" height="7" rx="2" />
            <rect x="14" y="14" width="7" height="7" rx="2" />
            <rect x="3" y="14" width="7" height="7" rx="2" />
          </svg>
          <h2 class="text-xs font-semibold tracking-wide text-text-secondary">
            {{ tr('subscriptionDiscoveryPage.categories') }}
          </h2>
        </div>

        <div class="flex flex-wrap gap-2">
          <button
              v-for="category in categories"
              :key="category.id"
              @click="selectCategory(category.id)"
              class="inline-flex h-8 items-center gap-1.5 rounded-full px-3.5 text-xs font-medium transition select-none active:scale-[0.98]"
              :class="selectedCategory === category.id
                ? 'bg-primary/15 font-semibold text-primary shadow-xs'
                : 'bg-surface-container/50 text-text-secondary hover:bg-surface-container hover:text-text'"
          >
            <span v-if="category.icon">{{ category.icon }}</span>
            <span>{{ category.name }}</span>
          </button>
        </div>
      </div>

      <!-- 内容区 -->
      <div>
        <div v-if="categoriesQuery.isError.value" class="mb-4 rounded-xl bg-red-500/10 px-4 py-2.5 text-xs text-red-600 dark:text-red-400" role="alert">
          {{ tr('subscriptionDiscoveryPage.categoriesFailed') }}<button type="button" class="underline ml-1 font-semibold" @click="categoriesQuery.refetch()">{{ tr('common.retry') }}</button>
        </div>
        <div v-if="feedError" class="mb-4 rounded-xl bg-red-500/10 px-4 py-2.5 text-xs text-red-600 dark:text-red-400" role="alert">
          {{ tr('subscriptionDiscoveryPage.feedsFailed') }}<button type="button" class="underline ml-1 font-semibold" @click="feedsQuery.refetch()">{{ tr('common.retry') }}</button>
        </div>
        <div v-if="subscriptionError" class="mb-4 rounded-xl bg-red-500/10 px-4 py-2.5 text-xs text-red-600 dark:text-red-400" role="alert">
          {{ subscriptionError }}
        </div>

        <!-- 栏目标题与视图切换 -->
        <div class="mb-5 flex items-center justify-between">
          <h2 class="text-base font-bold text-text sm:text-lg">
            {{ selectedCategoryName }}
          </h2>
          <div class="flex items-center gap-1 rounded-full bg-surface-container/40 p-1">
            <button
                v-for="view in viewModes"
                :key="view.id"
                @click="viewMode = view.id"
                class="flex h-7 w-7 items-center justify-center rounded-full transition"
                :class="viewMode === view.id ? 'bg-surface text-text shadow-xs' : 'text-text-secondary hover:text-text'"
                :title="view.name"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path v-if="view.id === 'grid'" d="M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z" />
                <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8h16M4 16h16" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 骨架屏 -->
        <div v-if="loading" class="grid"
             :class="viewMode === 'grid' ? 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5' : 'grid-cols-1 gap-2.5'">
          <div v-for="i in 6" :key="i" class="flex items-center gap-3 rounded-2xl bg-surface-container/40 p-3.5 animate-pulse">
            <div class="h-10 w-10 shrink-0 rounded-full bg-surface-container"></div>
            <div class="flex-1 space-y-2">
              <div class="h-3.5 w-3/4 rounded bg-surface-container"></div>
              <div class="h-3 w-full rounded bg-surface-container"></div>
            </div>
          </div>
        </div>

        <!-- 网格视图：紧凑行与微型 32x32px 图标按钮 -->
        <div v-else-if="viewMode === 'grid'" class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <article
              v-for="feed in feeds"
              :key="feed.feedId"
              class="group flex items-center gap-3 rounded-2xl bg-surface-container/30 p-3 transition hover:bg-surface-container/60 hover:shadow-xs"
          >
            <!-- Feed 信息 -->
            <router-link :to="'/feed/' + feed.feedId" class="flex min-w-0 flex-1 items-center gap-3">
              <img
                  :src="feed.favicon"
                  :alt="feed.name"
                  class="h-10 w-10 shrink-0 rounded-full object-cover bg-surface p-1 shadow-xs"
                  @error="handleImageError"
              />
              <div class="min-w-0 flex-1">
                <h3 class="truncate text-xs font-semibold text-text transition group-hover:text-primary sm:text-sm">
                  {{ feed.name }}
                </h3>
                <p class="mt-0.5 truncate text-[11px] text-text-secondary">
                  {{ feed.description || feed.url }}
                </p>
              </div>
            </router-link>

            <!-- 订阅按钮：MD3 触控微型按钮 -->
            <button
                @click.stop.prevent="toggleSubscribe(feed)"
                :disabled="subscribing.has(feed.feedId)"
                :title="feed.subscribed ? tr('common.unsubscribe') : tr('common.subscribe')"
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full transition-all duration-200 disabled:opacity-40"
                :class="feed.subscribed
                  ? 'bg-surface-container text-text-secondary hover:bg-surface-container-high'
                  : 'bg-primary text-white shadow-xs hover:brightness-105 active:scale-95'"
            >
              <svg v-if="subscribing.has(feed.feedId)" class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M21 12a9 9 0 1 1-6.219-8.56" />
              </svg>
              <svg v-else-if="feed.subscribed" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              <svg v-else class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            </button>
          </article>
        </div>

        <!-- 列表视图 -->
        <div v-else class="space-y-2">
          <article
              v-for="feed in feeds"
              :key="feed.feedId"
              class="group flex items-center justify-between gap-3 rounded-2xl bg-surface-container/25 p-3.5 transition hover:bg-surface-container/60 hover:shadow-xs sm:px-4"
          >
            <router-link :to="'/feed/' + feed.feedId" class="flex min-w-0 flex-1 items-center gap-3.5">
              <img
                  :src="feed.favicon"
                  :alt="feed.name"
                  class="h-11 w-11 shrink-0 rounded-full object-cover bg-surface p-1 shadow-xs"
                  @error="handleImageError"
              />
              <div class="min-w-0 flex-1">
                <h3 class="truncate text-xs font-semibold text-text transition group-hover:text-primary sm:text-sm">
                  {{ feed.name }}
                </h3>
                <p class="mt-0.5 line-clamp-1 text-[11px] text-text-secondary sm:text-xs">
                  {{ feed.description || feed.url }}
                </p>
              </div>
            </router-link>

            <!-- 列表右侧胶囊按钮 -->
            <button
                @click="toggleSubscribe(feed)"
                :disabled="subscribing.has(feed.feedId)"
                class="inline-flex h-8 shrink-0 items-center justify-center rounded-full px-4 text-xs font-semibold transition active:scale-95 disabled:opacity-40"
                :class="feed.subscribed
                  ? 'bg-surface-container text-text-secondary hover:bg-surface-container-high'
                  : 'bg-primary text-white shadow-xs hover:brightness-105'"
            >
              <span v-if="subscribing.has(feed.feedId)" class="flex items-center gap-1.5">
                <svg class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                </svg>
                <span>{{ tr('common.processingShort') }}</span>
              </span>
              <span v-else>
                {{ feed.subscribed ? tr('common.subscribed') : tr('common.subscribe') }}
              </span>
            </button>
          </article>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && !feedError && feeds.length === 0" class="py-16 text-center">
          <div class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/70">
            <svg class="h-6 w-6 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" />
              <path d="m21 21-4.35-4.35" />
            </svg>
          </div>
          <h3 class="text-sm font-semibold text-text">{{ tr('subscriptionDiscoveryPage.emptyTitle') }}</h3>
          <p class="mt-1 text-xs text-text-secondary">{{ tr('subscriptionDiscoveryPage.emptyDescription') }}</p>
        </div>
      </div>

      <!-- 分页与加载状态 -->
      <div v-if="feeds.length" class="mt-8 flex flex-col items-center gap-2.5" aria-live="polite">
        <p v-if="isFetchingNextPage" class="text-xs text-text-secondary">{{ tr('subscriptionDiscoveryPage.loadingMoreFeeds') }}</p>
        <template v-else-if="nextPageError">
          <p class="text-xs text-red-600 dark:text-red-400">{{ tr('common.loadFailed') }}</p>
          <button type="button" class="inline-flex h-8 items-center rounded-full bg-surface-container px-4 text-xs font-semibold text-text hover:bg-surface-container-high" @click="loadMore">{{ tr('common.retry') }}</button>
        </template>
        <p v-else-if="!hasNextPage" class="text-xs text-text-secondary/60">{{ tr('subscriptionDiscoveryPage.allFeedsLoaded') }}</p>
      </div>
      <div ref="loadMoreSentinel" class="h-1" aria-hidden="true"></div>
    </div>

    <!-- 手动添加弹窗：MD3 28px 大圆角模态框 -->
    <transition name="modal">
      <div
          v-if="showManualAddDialog"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
          @click.self="showManualAddDialog = false"
      >
        <div class="absolute inset-0 bg-black/45" />
        <div class="relative z-10 w-full max-w-md overflow-hidden rounded-[28px] bg-surface shadow-2xl">
          <div class="flex items-center justify-between border-b border-outline/10 px-6 py-4">
            <h3 class="text-base font-semibold text-text">{{ tr('subscriptionDiscoveryPage.addManuallyTitle') }}</h3>
            <button
                @click="showManualAddDialog = false"
                class="flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
            >
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="p-6">
            <SubscriptionsAddManual @success="showManualAddDialog = false" />
          </div>
        </div>
      </div>
    </transition>

    <!-- OPML 导入弹窗：MD3 28px 大圆角模态框 -->
    <transition name="modal">
      <div
          v-if="showOpmlDialog"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
          @click.self="showOpmlDialog = false"
      >
        <div class="absolute inset-0 bg-black/45" />
        <div class="relative z-10 w-full max-w-2xl overflow-hidden rounded-[28px] bg-surface shadow-2xl">
          <div class="flex items-center justify-between border-b border-outline/10 px-6 py-4">
            <h3 class="text-base font-semibold text-text">{{ tr('subscriptionDiscoveryPage.importOpmlTitle') }}</h3>
            <button
                @click="showOpmlDialog = false"
                class="flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
            >
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="p-6">
            <SubscriptionsAddOpml @success="showOpmlDialog = false" />
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { useDocumentTitle } from '@/composables/documentTitle';
import { computed, onActivated, onBeforeUnmount, onDeactivated, ref, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useDiscoveryCategoriesQuery, useInfiniteDiscoveryFeedsQuery } from '@/queries/discovery';
import { useSubscriptionMutation } from '@/queries/subscriptions';
import { useInfiniteScroll } from '@/composables/infiniteScroll';
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
    categories.value.find(category => category.id === selectedCategory.value)?.name ?? tr('common.all'),
);
useDocumentTitle(() => tr('subscriptionDiscoveryPage.title'));
const viewModes = computed(() => [
  { id: 'grid', name: tr('subscriptionDiscoveryPage.gridView') },
  { id: 'list', name: tr('subscriptionDiscoveryPage.listView') },
]);

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
    subscriptionError.value = error instanceof Error ? error.message : tr('subscriptionDiscoveryPage.subscriptionFailed');
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

const { sentinel: loadMoreSentinel, loadMore } = useInfiniteScroll({
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
