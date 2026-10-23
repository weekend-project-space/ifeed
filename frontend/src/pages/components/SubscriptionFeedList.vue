<template>
  <div>
    <!-- 工具栏 -->
    <div class="space-y-3 mb-6">
      <!-- 搜索框：MD3 药丸胶囊 -->
      <div class="relative">
        <svg class="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-text-secondary"
             viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
        <input
            v-model="searchQuery"
            type="text"
            :placeholder="tr('subscriptionFeedList.searchPlaceholder')"
            class="w-full rounded-full border border-outline/20 bg-surface-container/40 py-2.5 pl-10 pr-9 text-xs text-text outline-none transition placeholder:text-text-secondary/60 focus:border-primary focus:bg-surface focus:ring-2 focus:ring-primary/20 sm:text-sm"
        />
        <button
            v-if="searchQuery"
            @click="searchQuery = ''"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-text-secondary hover:text-text"
        >
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- 操作与排序栏 -->
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <!-- 左侧：排序与批量触发 -->
        <div class="flex items-center gap-2">
          <div class="relative shrink-0">
            <select
                v-model="sortBy"
                class="appearance-none cursor-pointer rounded-full bg-surface-container/60 py-2 pl-3.5 pr-8 text-xs font-medium text-text outline-none transition hover:bg-surface-container focus:ring-2 focus:ring-primary/20"
            >
              <option value="relevance">{{ tr('subscriptionFeedList.sortRelevance') }}</option>
              <option value="lastUpdated">{{ tr('subscriptionFeedList.lastUpdated') }}</option>
            </select>
            <svg class="pointer-events-none absolute right-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-text-secondary"
                 viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m6 9 6 6 6-6" />
            </svg>
          </div>

          <button
              v-if="!batchMode && filteredItems.length > 0"
              @click="enterBatchMode"
              class="inline-flex h-8 items-center rounded-full bg-surface-container/50 px-3.5 text-xs font-medium text-text-secondary transition hover:bg-surface-container hover:text-text"
          >
            {{ tr('subscriptionFeedList.batchActions') }}
          </button>
        </div>

        <!-- 右侧：常规动作按钮 -->
        <div v-if="!batchMode" class="flex items-center gap-2">
          <button
              @click="refresh"
              :disabled="subscriptionsStore.loading || isRefreshing"
              class="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text disabled:opacity-40"
              :aria-label="tr('common.refresh')"
          >
            <svg class="h-4 w-4" :class="{ 'animate-spin': subscriptionsStore.loading || isRefreshing }"
                 viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
            </svg>
          </button>
          <router-link
              to="/discover"
              class="inline-flex h-9 items-center gap-1.5 rounded-full bg-primary px-4 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98]"
          >
            <svg class="h-4 w-4 stroke-[2.2]" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path stroke-linecap="round" d="M12 5v14M5 12h14" />
            </svg>
            <span>{{ tr('subscriptionFeedList.addSubscription') }}</span>
          </router-link>
        </div>

        <!-- 批量操作激活栏 -->
        <div v-else class="flex flex-wrap items-center gap-2">
          <span class="text-xs text-text-secondary">
            {{ tr('common.selectedCount', { count: selectedItems.size }) }}
          </span>
          <button
              @click="selectAll"
              :disabled="isBatchProcessing"
              class="inline-flex h-8 items-center rounded-full bg-surface-container px-3 text-xs font-medium text-text hover:bg-surface-container-high transition"
          >
            {{ selectedItems.size === filteredItems.length ? tr('subscriptionFeedList.deselectAll') : tr('subscriptionFeedList.selectAll') }}
          </button>
          <button
              @click="confirmBatchRemove"
              :disabled="selectedItems.size === 0 || isBatchProcessing"
              class="inline-flex h-8 items-center rounded-full bg-red-500/10 px-3.5 text-xs font-semibold text-red-600 transition hover:bg-red-500/20 disabled:opacity-40 dark:text-red-400"
          >
            {{ tr('subscriptionFeedList.batchUnsubscribe') }}
          </button>
          <button
              @click="exitBatchMode"
              :disabled="isBatchProcessing"
              class="inline-flex h-8 items-center rounded-full px-3 text-xs font-medium text-text-secondary hover:bg-surface-container transition"
          >
            {{ tr('subscriptionFeedList.exit') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 错误反馈提示 -->
    <div v-if="subscriptionError"
         class="mb-4 rounded-2xl bg-red-500/10 p-3.5 text-xs text-red-600 dark:text-red-400"
         role="alert">
      {{ subscriptionError }}
    </div>

    <div v-if="subscriptionsStore.error && !subscriptionsStore.loading"
         class="mb-6 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400">
      <p class="font-semibold">{{ tr('subscriptionFeedList.listFailed') }}</p>
      <p class="mt-0.5 opacity-90">{{ subscriptionsStore.error }}</p>
      <button type="button" class="mt-2 font-semibold underline underline-offset-2 hover:opacity-80" @click="refresh">{{ tr('common.retry') }}</button>
    </div>

    <!-- 骨架屏 -->
    <div v-if="subscriptionsStore.loading && !items.length" class="space-y-3">
      <div v-for="i in 3" :key="i" class="flex items-start gap-4 rounded-2xl bg-surface-container/30 p-4 animate-pulse">
        <div class="h-10 w-10 shrink-0 rounded-full bg-surface-container"></div>
        <div class="flex-1 space-y-2 py-1">
          <div class="h-4 w-1/3 rounded bg-surface-container"></div>
          <div class="h-3 w-1/2 rounded bg-surface-container"></div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!feedItems.length"
         class="rounded-3xl border-2 border-dashed border-outline/20 bg-surface-container/20 py-16 text-center">
      <div class="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/80">
        <svg class="h-6 w-6 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      </div>
      <h2 class="text-base font-bold text-text">{{ tr('subscriptionFeedList.noSubscriptions') }}</h2>
      <p class="mt-1 text-xs text-text-secondary mb-5">{{ tr('subscriptionFeedList.addFeedsHint') }}</p>
      <router-link
          to="/discover"
          class="inline-flex h-9 items-center gap-1.5 rounded-full bg-primary px-5 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98]"
      >
        {{ tr('subscriptionFeedList.addSubscription') }}
      </router-link>
    </div>

    <!-- 搜索无结果 -->
    <div v-else-if="searchQuery && filteredItems.length === 0" class="py-16 text-center">
      <div class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/80">
        <svg class="h-6 w-6 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
      </div>
      <h2 class="text-sm font-bold text-text">{{ tr('subscriptionFeedList.noMatchingFeeds') }}</h2>
      <p class="mt-1 text-xs text-text-secondary mb-4">{{ tr('subscriptionFeedList.tryOtherKeywords') }}</p>
      <button
          @click="searchQuery = ''"
          class="inline-flex h-8 items-center rounded-full bg-surface-container px-4 text-xs font-semibold text-text hover:bg-surface-container-high transition"
      >
        {{ tr('subscriptionFeedList.clearSearch') }}
      </button>
    </div>

    <!-- 订阅列表：MD3 表面卡片列表 -->
    <div v-else class="space-y-2.5">
      <article
          v-for="item in filteredItems"
          :key="item.feedId"
          class="group flex items-start gap-3.5 rounded-2xl p-3.5 transition sm:p-4"
          :class="batchMode && selectedItems.has(item.feedId)
            ? 'bg-primary/10 shadow-xs'
            : 'bg-surface-container/30 hover:bg-surface-container/60 hover:shadow-xs'"
      >
        <!-- 批量复选框 -->
        <div v-if="batchMode" class="shrink-0 pt-2.5">
          <input
              type="checkbox"
              :checked="selectedItems.has(item.feedId)"
              :disabled="isBatchProcessing"
              @change="toggleSelection(item.feedId)"
              class="h-4 w-4 cursor-pointer rounded-sm border-outline text-primary focus:ring-primary/20"
          />
        </div>

        <!-- 头像 -->
        <router-link :to="`/feed/${item.feedId}`" class="shrink-0" :class="{ 'pointer-events-none': batchMode }">
          <img
              :src="getFaviconUrl(item)"
              :alt="displayTitle(item)"
              class="h-10 w-10 rounded-full bg-surface object-cover p-0.5 shadow-xs"
              @error="handleImageError"
          />
        </router-link>

        <!-- 内容区域 -->
        <div class="flex-1 min-w-0">
          <div class="flex items-start justify-between gap-3 mb-1">
            <router-link
                :to="`/feed/${item.feedId}`"
                class="flex-1 min-w-0"
                :class="{ 'pointer-events-none': batchMode }"
            >
              <h3 class="truncate text-xs font-bold text-text transition group-hover:text-primary sm:text-sm">
                {{ displayTitle(item) }}
              </h3>
            </router-link>

            <!-- 操作下拉 -->
            <div v-if="!batchMode" class="relative flex shrink-0 items-center">
              <button
                  type="button"
                  class="flex h-7 w-7 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface hover:text-text"
                  @click.stop="toggleDropdown(item.feedId)"
                  :aria-expanded="activeDropdown === item.feedId"
                  :aria-label="tr('common.moreActions')"
              >
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="1" fill="currentColor" />
                  <circle cx="12" cy="5" r="1" fill="currentColor" />
                  <circle cx="12" cy="19" r="1" fill="currentColor" />
                </svg>
              </button>
              <div
                  v-if="activeDropdown === item.feedId"
                  class="absolute right-0 top-full z-20 mt-1 w-32 overflow-hidden rounded-2xl border border-outline/10 bg-surface py-1 shadow-xl"
              >
                <router-link
                    :to="`/feed/${item.feedId}`"
                    class="block w-full px-4 py-2 text-left text-xs font-medium text-text transition hover:bg-surface-container"
                    @click="activeDropdown = null"
                >
                  {{ tr('subscriptionFeedList.viewDetails') }}
                </router-link>
                <button
                    @click.stop="confirmRemove(item)"
                    :disabled="subscriptionSubmitting"
                    class="w-full px-4 py-2 text-left text-xs font-medium text-red-600 transition hover:bg-red-500/10 disabled:opacity-40 dark:text-red-400"
                >
                  {{ tr('common.unsubscribe') }}
                </button>
              </div>
            </div>
          </div>

          <router-link :to="`/feed/${item.feedId}`" class="block" :class="{ 'pointer-events-none': batchMode }">
            <p class="mb-2 line-clamp-1 text-xs text-text-secondary">
              {{ getDescription(item) || tr('subscriptionFeedList.noDescription') }}
            </p>
            <div class="flex flex-wrap items-center gap-2 text-xs text-text-secondary">
              <span class="truncate max-w-[180px]">@{{ getUsername(item) }}</span>
              <span>•</span>
              <span class="truncate">{{ getSubscriberCount(item) }}</span>
            </div>

            <!-- 异常状态指示标签 -->
            <div
                v-if="hasIssue(item)"
                class="mt-2 inline-flex items-center gap-1.5 rounded-full bg-red-500/10 px-2.5 py-0.5 text-[10px] font-semibold text-red-600 dark:text-red-400"
            >
              <svg class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd" />
              </svg>
              <span>{{ tr('subscriptionFeedList.fetchFailed') }}</span>
            </div>
          </router-link>
        </div>
      </article>
    </div>

    <!-- 底部统计 -->
    <div v-if="feedItems.length" class="mt-6 text-center text-xs text-text-secondary/60">
      {{ tr('subscriptionFeedList.showingFeeds', { visible: filteredItems.length, total: feedItems.length }) }}
    </div>

    <!-- 点击遮罩收起下拉 -->
    <div v-if="activeDropdown" @click="activeDropdown = null" class="fixed inset-0 z-10"></div>

    <!-- 单项取消订阅确认弹窗：MD3 28px 大圆角 -->
    <div
        v-if="itemToRemove"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
        @click.self="cancelRemove"
    >
      <div class="absolute inset-0 bg-black/45" />
      <div class="relative z-10 w-full max-w-sm rounded-[28px] bg-surface p-6 shadow-2xl">
        <h3 class="text-base font-bold text-text mb-2">{{ tr('subscriptionFeedList.confirmUnsubscribe') }}</h3>
        <p class="text-xs text-text-secondary mb-5 leading-relaxed">
          <i18n-t keypath="subscriptionFeedList.unsubscribeQuestion" scope="global" tag="span"><template #name><strong>{{ displayTitle(itemToRemove) }}</strong></template></i18n-t>
        </p>
        <div v-if="subscriptionError" class="mb-4 rounded-xl bg-red-500/10 p-3 text-xs text-red-600 dark:text-red-400" role="alert">
          {{ subscriptionError }}
        </div>
        <div class="flex gap-2.5 justify-end">
          <button
              @click="cancelRemove"
              :disabled="subscriptionSubmitting"
              class="inline-flex h-9 items-center justify-center rounded-full px-4 text-xs font-semibold text-text-secondary hover:bg-surface-container transition"
          >
            {{ tr('common.cancel') }}
          </button>
          <button
              @click="remove(itemToRemove.feedId)"
              :disabled="subscriptionSubmitting"
              class="inline-flex h-9 items-center justify-center rounded-full bg-red-600 px-4 text-xs font-semibold text-white shadow-xs transition hover:bg-red-700 disabled:opacity-40"
          >
            {{ tr('subscriptionFeedList.confirmUnsubscribe') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 批量取消订阅确认弹窗 -->
    <div
        v-if="batchItemsToRemove.length > 0"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
        @click.self="cancelBatchRemove"
    >
      <div class="absolute inset-0 bg-black/45" />
      <div class="relative z-10 w-full max-w-md rounded-[28px] bg-surface p-6 shadow-2xl">
        <h3 class="text-base font-bold text-text mb-2">{{ tr('subscriptionFeedList.confirmBatchUnsubscribe') }}</h3>
        <p class="text-xs text-text-secondary mb-3 leading-relaxed">
          <i18n-t keypath="subscriptionFeedList.batchUnsubscribeQuestion" scope="global" :plural="batchItemsToRemove.length" tag="span"><template #count><strong>{{ batchItemsToRemove.length }}</strong></template></i18n-t>
        </p>
        <div class="max-h-48 overflow-y-auto mb-5 rounded-2xl bg-surface-container/40 p-3">
          <ul class="space-y-1 text-xs text-text">
            <li v-for="item in batchItemsToRemove" :key="item.feedId" class="truncate">
              {{ displayTitle(item) }}
            </li>
          </ul>
        </div>
        <div v-if="subscriptionError" class="mb-4 rounded-xl bg-red-500/10 p-3 text-xs text-red-600 dark:text-red-400" role="alert">
          {{ subscriptionError }}
        </div>
        <div class="flex gap-2.5 justify-end">
          <button
              @click="cancelBatchRemove"
              :disabled="isBatchProcessing"
              class="inline-flex h-9 items-center justify-center rounded-full px-4 text-xs font-semibold text-text-secondary hover:bg-surface-container transition disabled:opacity-40"
          >
            {{ tr('common.cancel') }}
          </button>
          <button
              @click="batchRemove"
              :disabled="isBatchProcessing"
              class="inline-flex h-9 items-center justify-center rounded-full bg-red-600 px-4 text-xs font-semibold text-white shadow-xs transition hover:bg-red-700 disabled:opacity-40"
          >
            {{ isBatchProcessing ? tr('common.processing') : tr('subscriptionFeedList.confirmUnsubscribe') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { computed, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useSubscriptionsStore, type SubscriptionListItemDto } from '../../stores/subscriptions';
import { formatRelativeTime } from '../../utils/datetime';
import { useBatchRemoveSubscriptionsMutation, useSubscriptionMutation } from '../../queries/subscriptions';

defineOptions({ name: 'SubscriptionFeedList' });

const subscriptionsStore = useSubscriptionsStore();
const { items } = storeToRefs(subscriptionsStore);
const subscriptionMutation = useSubscriptionMutation();
const batchRemoveMutation = useBatchRemoveSubscriptionsMutation();
const subscriptionSubmitting = subscriptionMutation.isPending;
const subscriptionError = ref('');
const activeDropdown = ref<string | null>(null);
const sortBy = ref<'relevance' | 'lastUpdated'>('relevance');
const isRefreshing = ref(false);
const itemToRemove = ref<SubscriptionListItemDto | null>(null);

// 搜索与过滤
const searchQuery = ref('');

// 批量模式
const batchMode = ref(false);
const selectedItems = ref<Set<string>>(new Set());
const isBatchProcessing = batchRemoveMutation.isPending;
const batchItemsToRemove = ref<SubscriptionListItemDto[]>([]);
const feedItems = computed(() => items.value.filter((item) => !item.type || item.type === 'FEED'));

const sortedItems = computed(() => {
  const list = [...feedItems.value];
  switch (sortBy.value) {
    case 'relevance':
      return list.sort((a, b) => {
        const aHasIssue = hasIssue(a);
        const bHasIssue = hasIssue(b);
        if (aHasIssue !== bHasIssue) return aHasIssue ? 1 : -1;
        return getTime(b.lastUpdated || b.lastFetched) - getTime(a.lastUpdated || a.lastFetched);
      });
    case 'lastUpdated':
      return list.sort((a, b) => getTime(b.lastUpdated || b.lastFetched) - getTime(a.lastUpdated || a.lastFetched));
    default:
      return list;
  }
});

const filteredItems = computed(() => {
  if (!searchQuery.value.trim()) return sortedItems.value;
  const query = searchQuery.value.toLowerCase().trim();
  return sortedItems.value.filter(item => {
    const title = (item.title || '').toLowerCase();
    const url = (item.url || '').toLowerCase();
    const siteUrl = (item.siteUrl || '').toLowerCase();
    const description = (item.description || '').toLowerCase();
    return title.includes(query) || url.includes(query) || siteUrl.includes(query) || description.includes(query);
  });
});

const hasIssue = (item: SubscriptionListItemDto) => (item.failureCount ?? 0) > 0 || !!item.fetchError;
const getTime = (date?: string | null) => date ? new Date(date).getTime() : 0;

const toggleDropdown = (feedId: string) => {
  activeDropdown.value = activeDropdown.value === feedId ? null : feedId;
};

const refresh = async () => {
  if (isRefreshing.value || subscriptionsStore.loading) return;
  isRefreshing.value = true;
  try {
    await subscriptionsStore.fetchSubscriptions();
  } catch (error) {
    subscriptionError.value = error instanceof Error ? error.message : tr('subscriptionFeedList.listLoadFailed');
  } finally {
    setTimeout(() => isRefreshing.value = false, 500);
  }
};

const confirmRemove = (item: SubscriptionListItemDto) => {
  activeDropdown.value = null;
  subscriptionError.value = '';
  itemToRemove.value = item;
};

const cancelRemove = () => {
  if (subscriptionSubmitting.value) return;
  itemToRemove.value = null;
};

const remove = async (feedId: string) => {
  const item = items.value.find((entry) => entry.feedId === feedId);
  if (!item || subscriptionSubmitting.value) return;
  subscriptionError.value = '';
  try {
    await subscriptionMutation.mutateAsync({
      feedId: item.feedId,
      feedUrl: item.url ?? '',
      subscribed: true,
    });
    itemToRemove.value = null;
  } catch (error) {
    subscriptionError.value = error instanceof Error ? error.message : tr('subscriptionFeedList.unsubscribeFailed');
  }
};

const enterBatchMode = () => {
  batchMode.value = true;
  selectedItems.value.clear();
};

const exitBatchMode = () => {
  if (isBatchProcessing.value) return;
  batchMode.value = false;
  selectedItems.value.clear();
};

const toggleSelection = (feedId: string) => {
  if (selectedItems.value.has(feedId)) {
    selectedItems.value.delete(feedId);
  } else {
    selectedItems.value.add(feedId);
  }
  selectedItems.value = new Set(selectedItems.value);
};

const selectAll = () => {
  if (selectedItems.value.size === filteredItems.value.length) {
    selectedItems.value.clear();
  } else {
    selectedItems.value = new Set(filteredItems.value.map(item => item.feedId));
  }
};

const confirmBatchRemove = () => {
  if (selectedItems.value.size === 0 || isBatchProcessing.value) return;
  subscriptionError.value = '';
  batchItemsToRemove.value = filteredItems.value.filter(item =>
      selectedItems.value.has(item.feedId)
  );
};

const cancelBatchRemove = () => {
  if (isBatchProcessing.value) return;
  batchItemsToRemove.value = [];
};

const batchRemove = async () => {
  if (batchItemsToRemove.value.length === 0 || isBatchProcessing.value || subscriptionSubmitting.value) return;
  subscriptionError.value = '';
  try {
    const result = await batchRemoveMutation.mutateAsync(
        batchItemsToRemove.value.map((item) => item.feedId),
    );
    for (const feedId of result.removedFeedIds) {
      selectedItems.value.delete(feedId);
    }
    const failedIds = new Set(result.failedFeedIds);
    batchItemsToRemove.value = batchItemsToRemove.value.filter((item) => failedIds.has(item.feedId));
    if (failedIds.size) {
      subscriptionError.value = tr('subscriptionFeedList.batchPartialFailure', { value0: failedIds.size });
    } else {
      exitBatchMode();
    }
  } catch (error) {
    subscriptionError.value = error instanceof Error ? error.message : tr('subscriptionFeedList.batchUnsubscribeFailed');
  }
};

const displayTitle = (item: SubscriptionListItemDto) => item.title || item.siteUrl || item.url || tr('common.untitledFeed');

const getUsername = (item: SubscriptionListItemDto) => {
  const url = item.siteUrl || item.url;
  if (!url) return tr('common.unknownSource');
  try {
    const hostname = new URL(url).hostname.replace('www.', '');
    return hostname.length > 30 ? hostname.substring(0, 30) + '...' : hostname;
  } catch {
    return url.length > 30 ? url.substring(0, 30) + '...' : url;
  }
};

const getSubscriberCount = (item: SubscriptionListItemDto) => {
  const timeStr = item.lastUpdated || item.lastFetched;
  return timeStr ? formatRelativeTime(timeStr) : tr('common.noUpdates');
};

const getDescription = (item: SubscriptionListItemDto) => item.description;

const getFaviconUrl = (item: SubscriptionListItemDto) => {
  const url = item.siteUrl || item.url;
  if (!url) return '';
  try {
    const hostname = new URL(url).hostname;
    return `https://favicon.im/${hostname}`;
  } catch {
    return '';
  }
};

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement;
  img.style.display = 'none';
  const placeholder = document.createElement('div');
  placeholder.className = 'w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary';
  placeholder.innerHTML = '<svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 2v4m0 12v4M4.22 4.22l2.83 2.83m9.9 9.9 2.83 2.83M2 12h4m12 0h4M4.22 19.78l2.83-2.83m9.9-9.9 2.83-2.83"/></svg>';
  img.parentElement?.appendChild(placeholder);
};

onMounted(() => {
  if (!items.value.length) void refresh();
});
</script>
