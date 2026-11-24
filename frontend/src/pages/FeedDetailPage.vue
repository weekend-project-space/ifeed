<template>
  <div class="mx-auto max-w-screen-lg py-6 sm:py-8 text-text">
    <article-list
        :items="items"
        :loading="articlesLoading"
        :empty-message="tr('feedDetailPage.empty')"
        @select-tag="handleSelectTag"
        @refresh="refreshArticles">
      <template #header>
        <!-- 频道头部：MD3 动态表面包裹卡片 -->
        <section class="mb-8 rounded-[28px] bg-surface-container/40 p-5 sm:p-7">
          <div class="flex flex-col gap-5 sm:flex-row sm:items-start sm:gap-6">
            <!-- 频道头像 -->
            <div class="shrink-0">
              <a :href="detail?.siteUrl" target="_blank" rel="noopener noreferrer">
                <img
                    v-if="detail?.avatar"
                    :src="detail.avatar"
                    class="h-16 w-16 rounded-full bg-surface object-cover shadow-xs p-0.5 sm:h-20 sm:w-20"
                    :alt="tr('feedDetailPage.feedAvatar')"/>
                <div
                    v-else
                    class="flex h-16 w-16 items-center justify-center rounded-full bg-surface-container-high text-xl font-bold text-text sm:h-20 sm:w-20 shadow-xs">
                  {{ channelInitial }}
                </div>
              </a>
            </div>

            <!-- 信息主体与操作 -->
            <div class="min-w-0 flex-1 space-y-3.5">
              <div>
                <h1 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">
                  <a :href="detail?.siteUrl" target="_blank" rel="noopener noreferrer" class="hover:text-primary transition-colors">
                    {{ channelTitle }}
                  </a>
                </h1>
                <p class="mt-1 text-xs text-text-secondary leading-relaxed sm:text-sm">
                  {{ tr('feedDetailPage.descriptionMeta', { description: detail?.description || tr('feedDetailPage.noDescription'), updated: latestUpdateText, fetched: lastFetchedText }) }}
                </p>
              </div>

              <!-- 统计数据 -->
              <div class="flex flex-wrap gap-4 text-xs text-text-secondary">
                <span v-if="detail?.sources?.length" class="flex items-center gap-1.5 font-medium">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
                  </svg>
                  {{ tr('common.sourceCount', { count: detail.sources.length }) }}
                </span>
                <span v-else class="flex items-center gap-1.5 font-medium">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                  {{ tr('common.articleCount', { count: detail?.articleCount ?? 0 }) }}
                </span>

                <span class="flex items-center gap-1.5 font-medium">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/>
                  </svg>
                  {{ tr('common.subscriberCount', { count: detail?.subscriberCount ?? 0 }) }}
                </span>
              </div>

              <!-- 混合源包含的子源 -->
              <div v-if="detail?.sources && detail.sources.length > 0" class="flex flex-wrap gap-1.5 pt-1">
                <span
                    v-for="sourceId in detail.sources"
                    :key="sourceId"
                    class="inline-flex items-center gap-1 rounded-full bg-surface px-3 py-1 text-[11px] font-medium text-text-secondary shadow-2xs"
                >
                  <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/>
                  </svg>
                  {{ sourceId }}
                </span>
              </div>

              <!-- 操作按钮：锁定宽度 w-20 (80px)，精简文案 -->
              <div class="flex flex-wrap items-center gap-2.5 pt-2">
                <button
                    type="button"
                    class="group relative inline-flex h-9.5 w-20 shrink-0 items-center justify-center gap-1.5 rounded-full text-xs font-semibold shadow-xs transition-all duration-200 select-none active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
                    :class="[
                      detail?.subscribed
                        ? 'border border-outline/30 bg-surface text-text hover:border-red-500/40 hover:bg-red-500/10 hover:text-red-600 dark:hover:text-red-400'
                        : 'bg-primary text-white hover:brightness-105 hover:shadow-sm'
                    ]"
                    :disabled="subscriptionSubmitting || feedLoading"
                    @click="toggleSubscription">
                  <!-- 正在处理态 -->
                  <template v-if="subscriptionSubmitting">
                    <svg class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                    </svg>
                  </template>

                  <!-- 已订阅态：默认对勾，Hover时变叉 -->
                  <template v-else-if="detail?.subscribed">
                    <span class="inline-flex items-center gap-1 group-hover:hidden">
                      <svg class="h-3.5 w-3.5 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                      <span>{{ tr('common.subscribedShort') }}</span>
                    </span>
                    <span class="hidden items-center gap-1 group-hover:inline-flex">
                      <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                      </svg>
                      <span>{{ tr('common.unsubscribeShort') }}</span>
                    </span>
                  </template>

                  <!-- 未订阅态 -->
                  <template v-else>
                    <svg class="h-3.5 w-3.5 stroke-[2.5]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="12" y1="5" x2="12" y2="19" />
                      <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                    <span>{{ tr('common.subscribe') }}</span>
                  </template>
                </button>

                <a
                    v-if="detail?.siteUrl"
                    :href="detail.siteUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex h-9.5 items-center gap-1.5 rounded-full bg-surface px-4.5 text-xs font-medium text-text shadow-xs transition hover:bg-surface-container active:scale-95">
                  <span>{{ tr('feedDetailPage.visitWebsite') }}</span>
                  <svg class="h-3.5 w-3.5 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
                  </svg>
                </a>
              </div>

              <!-- 抓取异常警告 -->
              <div
                  v-if="hasFetchIssue"
                  class="flex items-start gap-2 rounded-2xl bg-red-500/10 p-3 text-xs text-red-600 dark:text-red-400">
                <svg class="mt-0.5 h-4 w-4 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd"
                        d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                        clip-rule="evenodd"/>
                </svg>
                <span class="truncate">{{ fetchIssueLabel }}</span>
              </div>

              <p v-if="feedError" class="text-xs text-red-600 dark:text-red-400">{{ feedError }}</p>
              <p v-if="subscriptionError" class="text-xs text-red-600 dark:text-red-400" role="alert">{{ subscriptionError }}</p>
            </div>
          </div>
        </section>

        <!-- 标签筛选条 -->
        <div
            v-if="selectedTagDisplay"
            class="mb-6 flex items-center justify-between gap-4 rounded-2xl bg-primary/10 px-4 py-2 text-xs font-medium text-primary">
          <span>{{ tr('feedDetailPage.selectedTag', { tag: selectedTagDisplay }) }}</span>
          <button class="hover:underline font-semibold" @click="clearTag">
            {{ tr('common.clear') }}
          </button>
        </div>
      </template>
    </article-list>

    <p v-if="articleError" class="text-xs text-red-600 dark:text-red-400 text-center py-4">
      {{ articleError }}
    </p>

    <!-- 底部滚动指示 -->
    <div v-if="items.length" class="mt-8 flex flex-col items-center gap-2.5" aria-live="polite">
      <div v-if="isFetchingNextPage" class="text-xs text-text-secondary">{{ tr('feedDetailPage.loadingArticles') }}</div>
      <template v-else-if="nextPageError">
        <p class="text-xs text-red-600 dark:text-red-400">{{ tr('common.loadFailed') }}</p>
        <button
            type="button"
            class="inline-flex h-8 items-center rounded-full bg-surface-container px-4 text-xs font-semibold text-text hover:bg-surface-container-high transition"
            @click="loadMore"
        >
          {{ tr('common.retry') }}
        </button>
      </template>
      <p v-else-if="!hasNextPage" class="text-xs text-text-secondary/60">{{ tr('feedDetailPage.allArticlesLoaded') }}</p>
    </div>
    <div ref="loadMoreSentinel" class="h-1" aria-hidden="true"></div>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useMutation } from '@tanstack/vue-query';
import { normalizeArticle } from '../stores/articles/types';
import { useFeedQuery, useInfiniteFeedArticlesQuery } from '../queries/feed';
import { useSubscriptionMutation } from '../queries/subscriptions';
import { useDocumentTitle } from '../composables/documentTitle';
import { useSubscriptionsStore } from '../stores/subscriptions';
import { useReadFeedStore } from '../stores/readfeed';
import { formatRelativeTime } from '../utils/datetime';
import { useInfiniteScroll } from '../composables/infiniteScroll';
import ArticleList from '../components/ArticleList.vue';

defineOptions({ name: 'FeedDetailPage' });

const props = defineProps<{
  feedId: string;
  tags: string | null;
}>();
const router = useRouter();
const subscriptionsStore = useSubscriptionsStore();
const readFeedStore = useReadFeedStore();

const currentFeedId = computed(() => props.feedId);
const routeTag = computed(() => props.tags);

const normalizedTag = computed(() => {
  const tag = routeTag.value;
  return tag ? tag.toLowerCase() : null;
});

const feedQuery = useFeedQuery(currentFeedId);
const feedArticlesQuery = useInfiniteFeedArticlesQuery(currentFeedId, normalizedTag);
const detail = computed(() => feedQuery.data.value ?? null);
const items = computed(() => feedArticlesQuery.data.value?.pages
    .flatMap((page) => page.content)
    .map(normalizeArticle) ?? []);
const feedLoading = computed(() => feedQuery.isPending.value);
const feedError = computed(() => feedQuery.error.value instanceof Error ? feedQuery.error.value.message : null);
const articlesLoading = computed(() => feedArticlesQuery.isPending.value);
const isFetchingNextPage = computed(() => feedArticlesQuery.isFetchingNextPage.value);
const hasNextPage = computed(() => Boolean(feedArticlesQuery.hasNextPage.value));
const articleError = computed(() => {
  const error = feedArticlesQuery.error.value;
  if (items.value.length > 0) return null;
  return error instanceof Error ? error.message : error ? tr('feedDetailPage.articleLoadError') : null;
});
const nextPageError = computed(() => feedArticlesQuery.isFetchNextPageError.value);
useDocumentTitle(() => (
    detail.value?.title?.trim()
    || detail.value?.siteUrl?.trim()
    || detail.value?.url?.trim()
));

const selectedTagDisplay = computed(() => routeTag.value);

const channelTitle = computed(() => {
  if (!detail.value) return tr('feedDetailPage.feedChannel');
  const title = detail.value.title?.trim();
  if (title) return title;
  const site = detail.value.siteUrl?.trim();
  if (site) return site;
  return detail.value.url;
});

const channelInitial = computed(() => {
  const text = channelTitle.value?.trim();
  if (!text) return 'F';
  const chars = Array.from(text);
  return chars.slice(0, 2).join('').toUpperCase();
});

const latestUpdateText = computed(() => {
  const target = detail.value;
  if (!target) return tr('common.noUpdates');
  const timestamp = target.latestPublishedAt ?? target.lastUpdated ?? target.lastFetched;
  return timestamp ? formatRelativeTime(timestamp) : tr('common.noUpdates');
});

const lastFetchedText = computed(() => {
  const target = detail.value;
  if (!target || !target.lastFetched) return tr('common.noRecords');
  return formatRelativeTime(target.lastFetched);
});

const failureCount = computed(() => detail.value?.failureCount ?? 0);

const fetchErrorMessage = computed(() => {
  const message = detail.value?.fetchError;
  if (!message) return null;
  const trimmed = message.trim();
  return trimmed.length ? trimmed : null;
});

const hasFetchIssue = computed(() => failureCount.value > 0 || !!fetchErrorMessage.value);

const fetchIssueLabel = computed(() => {
  const parts: string[] = [];
  if (failureCount.value > 0) {
    parts.push(tr('feedDetailPage.consecutiveFailures', { value0: failureCount.value }));
  }
  if (fetchErrorMessage.value) {
    parts.push(tr('feedDetailPage.fetchError', { value0: fetchErrorMessage.value }));
  }
  return parts.join(' · ') || tr('feedDetailPage.fetchFailed');
});

const buildQuery = (overrides?: { tags?: string | null }) => {
  const query: Record<string, string> = {};
  const hasTagOverride = overrides && Object.prototype.hasOwnProperty.call(overrides, 'tags');
  const tag = hasTagOverride ? overrides?.tags ?? null : routeTag.value;
  if (tag) query.tags = tag;
  return query;
};

const refreshArticles = () => feedArticlesQuery.refetch();

const handleSelectTag = (tag: string) => {
  if (!tag || !currentFeedId.value) return;
  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ tags: tag.toLowerCase() })
  });
};

const clearTag = () => {
  if (!routeTag.value || !currentFeedId.value) return;
  router.push({
    name: 'feed',
    params: { feedId: currentFeedId.value },
    query: buildQuery({ tags: null })
  });
};

const subscriptionMutation = useSubscriptionMutation();
const subscriptionSubmitting = subscriptionMutation.isPending;
const subscriptionError = ref('');
const toggleSubscription = async () => {
  const feed = detail.value;
  if (!feed || !currentFeedId.value || subscriptionSubmitting.value) return;
  subscriptionError.value = '';
  try {
    await subscriptionMutation.mutateAsync({
      feedId: feed.feedId, feedUrl: feed.url, subscribed: feed.subscribed,
    });
  } catch (error) {
    subscriptionError.value = error instanceof Error ? error.message : tr('feedDetailPage.subscriptionFailed');
  }
};

const readFeedMutation = useMutation({
  mutationFn: (feedId: string) => readFeedStore.recordFeedRead(feedId),
  onSuccess: () => subscriptionsStore.fetchSubscriptions(),
});
const readFeedAttempted = new Set<string>();
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

watch([() => feedQuery.data.value, () => subscriptionsStore.items], () => {
  void readFeed();
}, { immediate: true });

const { sentinel: loadMoreSentinel, loadMore } = useInfiniteScroll({
  hasNextPage,
  isLoading: feedArticlesQuery.isFetching,
  hasError: feedArticlesQuery.isError,
  onLoadMore: () => feedArticlesQuery.fetchNextPage(),
});
</script>
