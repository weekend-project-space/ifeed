<template>
  <div>
    <!-- Toolbar -->
    <div class="space-y-3 mb-6">
      <!-- Search Bar -->
      <div class="relative">
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none"
             viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <path d="m21 21-4.35-4.35"/>
        </svg>
        <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索订阅源..."
            class="w-full pl-10 pr-4 py-2 bg-white dark:bg-gray-800 text-sm text-gray-900 dark:text-gray-100 rounded-lg border border-gray-300 dark:border-gray-600 focus:outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        />
        <button
            v-if="searchQuery"
            @click="searchQuery = ''"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- Action Bar -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <!-- Left: Sort & Batch Mode -->
        <div class="flex items-center gap-2">
          <div class="relative flex-shrink-0">
            <select
                v-model="sortBy"
                class="appearance-none bg-white dark:bg-gray-800 text-sm text-gray-900 dark:text-gray-100 pl-3 pr-10 py-2 rounded-lg border border-gray-300 dark:border-gray-600 focus:outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20 cursor-pointer">
              <option value="relevance">相关度(从高到低)</option>
              <option value="lastUpdated">最近更新</option>
              <option value="subscribeTime">订阅时间</option>
            </select>
            <svg class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none"
                 viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m6 9 6 6 6-6"/>
            </svg>
          </div>
          
          <button
              v-if="!batchMode && filteredItems.length > 0"
              @click="enterBatchMode"
              class="px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
          >
            批量操作
          </button>
        </div>

        <!-- Right: Action Buttons -->
        <div v-if="!batchMode" class="flex items-center gap-2">
          <button
              @click="refresh"
              :disabled="subscriptionsStore.loading || isRefreshing"
              class="flex-1 sm:flex-none p-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors disabled:opacity-50"
              aria-label="刷新"
          >
            <svg
                class="w-5 h-5 mx-auto"
                :class="{ 'animate-spin': subscriptionsStore.loading || isRefreshing }"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
            >
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
            </svg>
          </button>
          <router-link
              to="/discover"
              class="flex-1 sm:flex-none inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium text-white bg-secondary hover:bg-secondary/90 rounded-full transition-colors"
          >
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"/>
            </svg>
            <span>添加订阅</span>
          </router-link>
        </div>

        <!-- Batch Mode Actions -->
        <div v-else class="flex items-center gap-2">
          <span class="text-sm text-gray-600 dark:text-gray-400">
            已选择 {{ selectedItems.size }} 项
          </span>
          <button
              @click="selectAll"
              class="px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
          >
            {{ selectedItems.size === filteredItems.length ? '取消全选' : '全选' }}
          </button>
          <button
              @click="confirmBatchRemove"
              :disabled="selectedItems.size === 0 || isBatchProcessing"
              class="px-3 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors disabled:opacity-50"
          >
            批量取消订阅
          </button>
          <button
              @click="exitBatchMode"
              class="px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
          >
            取消
          </button>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="subscriptionsStore.loading && !items.length" class="space-y-3">
      <div v-for="i in 3" :key="i" class="flex items-start gap-4 p-4 rounded-lg animate-pulse">
        <div class="w-14 h-14 bg-gray-200 dark:bg-gray-700 rounded-full"></div>
        <div class="flex-1 space-y-2 py-1">
          <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded w-2/3"></div>
          <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
          <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!items.length" class="text-center py-16">
      <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center">
        <svg class="w-8 h-8 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
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
    </div>

    <!-- No Search Results -->
    <div v-else-if="searchQuery && filteredItems.length === 0" class="text-center py-16">
      <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center">
        <svg class="w-8 h-8 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <path d="m21 21-4.35-4.35"/>
        </svg>
      </div>
      <h2 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">未找到匹配的订阅源</h2>
      <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">尝试使用其他关键词搜索</p>
      <button
          @click="searchQuery = ''"
          class="inline-flex items-center gap-2 px-5 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full transition-colors"
      >
        清除搜索
      </button>
    </div>

    <!-- Subscription List -->
    <div v-else class="space-y-0">
      <article
          v-for="item in filteredItems"
          :key="item.feedId"
          class="group flex items-start gap-4 px-4 py-4 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
          :class="{ 'bg-secondary/5': batchMode && selectedItems.has(item.feedId) }"
      >
        <!-- Checkbox (Batch Mode) -->
        <div v-if="batchMode" class="flex-shrink-0 pt-2">
          <input
              type="checkbox"
              :checked="selectedItems.has(item.feedId)"
              @change="toggleSelection(item.feedId)"
              class="w-5 h-5 text-secondary border-gray-300 dark:border-gray-600 rounded focus:ring-secondary cursor-pointer"
          />
        </div>

        <!-- Avatar -->
        <router-link :to="`/feeds/${item.feedId}`" class="flex-shrink-0" :class="{ 'pointer-events-none': batchMode }">
          <img
              :src="getFaviconUrl(item)"
              :alt="displayTitle(item)"
              class="w-10 h-10 rounded-full bg-gray-100 dark:bg-gray-800 object-cover"
              @error="handleImageError"
          />
        </router-link>

        <!-- Content -->
        <div class="flex-1 min-w-0">
          <div class="flex items-start justify-between gap-3 mb-1.5">
            <router-link :to="`/feeds/${item.feedId}`" class="flex-1 min-w-0" :class="{ 'pointer-events-none': batchMode }">
              <h3 class="text-base font-medium text-gray-900 dark:text-gray-100 line-clamp-1 group-hover:text-secondary transition-colors">
                {{ displayTitle(item) }}
              </h3>
            </router-link>

            <!-- Action Buttons -->
            <div v-if="!batchMode" class="flex items-center gap-1.5 flex-shrink-0">
              <button
                  class="relative p-1"
                  @click.stop="toggleDropdown(item.feedId)"
                  :aria-expanded="activeDropdown === item.feedId"
              >
                <svg class="w-5 h-5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="1" fill="currentColor"/>
                  <circle cx="12" cy="5" r="1" fill="currentColor"/>
                  <circle cx="12" cy="19" r="1" fill="currentColor"/>
                </svg>
                <!-- Dropdown Menu -->
                <div
                    v-if="activeDropdown === item.feedId"
                    class="absolute right-0 top-full mt-1 w-36 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-10"
                >
                  <router-link :to="`/feeds/${item.feedId}`" class="sm:hidden block w-full px-4 py-2 text-left text-sm text-gray-900 dark:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-700" @click="activeDropdown = null">
                    查看详情
                  </router-link>
                  <button
                      @click.stop="confirmRemove(item)"
                      :disabled="subscriptionsStore.submitting"
                      class="w-full px-4 py-2 text-left text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 disabled:opacity-50"
                  >
                    取消订阅
                  </button>
                </div>
              </button>
            </div>
          </div>

          <router-link :to="`/feeds/${item.feedId}`" class="block" :class="{ 'pointer-events-none': batchMode }">
            <div class="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 mb-1.5">
              <span class="truncate max-w-[200px]">@{{ getUsername(item) }}</span>
              <span>•</span>
              <span class="truncate">{{ getSubscriberCount(item) }}</span>
            </div>

            <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-2">
              {{ getDescription(item) }}
            </p>

            <!-- Status -->
            <div v-if="hasIssue(item)" class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-red-50 dark:bg-red-900/20 text-xs text-red-700 dark:text-red-400">
              <svg class="w-3.5 h-3.5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z"/>
              </svg>
              <span>抓取异常</span>
            </div>
            <span v-else class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-green-50 dark:bg-green-900/20 text-xs text-green-700 dark:text-green-400">
              <svg class="w-3.5 h-3.5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z"/>
              </svg>
              <span>已订阅</span>
            </span>
          </router-link>
        </div>
      </article>
    </div>

    <!-- Footer -->
    <div v-if="items.length" class="mt-6 text-center text-sm text-gray-600 dark:text-gray-400">
      共 {{ items.length }} 个订阅源
    </div>

    <!-- Click outside to close dropdown -->
    <div v-if="activeDropdown" @click="activeDropdown = null" class="fixed inset-0 z-0"></div>

    <!-- Single Item Confirmation Dialog -->
    <div
        v-if="itemToRemove"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40"
        @click.self="itemToRemove = null"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-sm w-full p-5">
        <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">确认取消订阅</h3>
        <p class="text-sm text-gray-600 dark:text-gray-400 mb-5">
          确定要取消订阅 <strong>{{ displayTitle(itemToRemove) }}</strong> 吗?
        </p>
        <div class="flex gap-3 justify-end">
          <button @click="itemToRemove = null" class="px-4 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
            取消
          </button>
          <button
              @click="remove(itemToRemove.feedId)"
              :disabled="subscriptionsStore.submitting"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg disabled:opacity-50 transition-colors"
          >
            确认取消订阅
          </button>
        </div>
      </div>
    </div>

    <!-- Batch Removal Confirmation Dialog -->
    <div
        v-if="batchItemsToRemove.length > 0"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40"
        @click.self="cancelBatchRemove"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full p-5">
        <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">确认批量取消订阅</h3>
        <p class="text-sm text-gray-600 dark:text-gray-400 mb-3">
          确定要取消订阅以下 <strong>{{ batchItemsToRemove.length }}</strong> 个订阅源吗?
        </p>
        <div class="max-h-48 overflow-y-auto mb-5 p-3 bg-gray-50 dark:bg-gray-900 rounded-lg">
          <ul class="space-y-1 text-sm text-gray-700 dark:text-gray-300">
            <li v-for="item in batchItemsToRemove" :key="item.feedId" class="truncate">
              • {{ displayTitle(item) }}
            </li>
          </ul>
        </div>
        <div class="flex gap-3 justify-end">
          <button 
              @click="cancelBatchRemove" 
              :disabled="isBatchProcessing"
              class="px-4 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors disabled:opacity-50"
          >
            取消
          </button>
          <button
              @click="batchRemove"
              :disabled="isBatchProcessing"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg disabled:opacity-50 transition-colors"
          >
            {{ isBatchProcessing ? '处理中...' : '确认取消订阅' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useSubscriptionsStore, type SubscriptionListItemDto } from '../../stores/subscriptions';
import { formatRelativeTime } from '../../utils/datetime';

const subscriptionsStore = useSubscriptionsStore();
const { items } = storeToRefs(subscriptionsStore);
const activeDropdown = ref<string | null>(null);
const sortBy = ref<'relevance' | 'lastUpdated' | 'subscribeTime'>('relevance');
const isRefreshing = ref(false);
const itemToRemove = ref<SubscriptionListItemDto | null>(null);

// Search & Filter
const searchQuery = ref('');

// Batch operations
const batchMode = ref(false);
const selectedItems = ref<Set<string>>(new Set());
const isBatchProcessing = ref(false);
const batchItemsToRemove = ref<SubscriptionListItemDto[]>([]);

const sortedItems = computed(() => {
  const list = [...items.value];

  switch (sortBy.value) {
    case 'relevance':
      return list.sort((a, b) => {
        const aHasIssue = hasIssue(a);
        const bHasIssue = hasIssue(b);
        if (aHasIssue !== bHasIssue) return aHasIssue ? 1 : -1;
        return getTime(b.subscribedAt) - getTime(a.subscribedAt);
      });
    case 'lastUpdated':
      return list.sort((a, b) => getTime(b.lastUpdated || b.lastFetched) - getTime(a.lastUpdated || a.lastFetched));
    case 'subscribeTime':
      return list.sort((a, b) => getTime(b.subscribedAt) - getTime(a.subscribedAt));
    default:
      return list;
  }
});

// Filter items based on search query
const filteredItems = computed(() => {
  if (!searchQuery.value.trim()) {
    return sortedItems.value;
  }

  const query = searchQuery.value.toLowerCase().trim();
  return sortedItems.value.filter(item => {
    const title = (item.title || '').toLowerCase();
    const url = (item.url || '').toLowerCase();
    const siteUrl = (item.siteUrl || '').toLowerCase();
    const description = getDescription(item).toLowerCase();
    
    return title.includes(query) || 
           url.includes(query) || 
           siteUrl.includes(query) ||
           description.includes(query);
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
  } finally {
    setTimeout(() => isRefreshing.value = false, 500);
  }
};

const confirmRemove = (item: SubscriptionListItemDto) => {
  activeDropdown.value = null;
  itemToRemove.value = item;
};

const remove = async (feedId: string) => {
  try {
    await subscriptionsStore.removeSubscription(feedId);
    itemToRemove.value = null;
  } catch (err) {
    console.error('取消订阅失败:', err);
  }
};

// Batch operations
const enterBatchMode = () => {
  batchMode.value = true;
  selectedItems.value.clear();
};

const exitBatchMode = () => {
  batchMode.value = false;
  selectedItems.value.clear();
  isBatchProcessing.value = false;
};

const toggleSelection = (feedId: string) => {
  if (selectedItems.value.has(feedId)) {
    selectedItems.value.delete(feedId);
  } else {
    selectedItems.value.add(feedId);
  }
  // Trigger reactivity
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
  if (selectedItems.value.size === 0) return;
  
  batchItemsToRemove.value = filteredItems.value.filter(item => 
    selectedItems.value.has(item.feedId)
  );
};

const cancelBatchRemove = () => {
  batchItemsToRemove.value = [];
};

const batchRemove = async () => {
  if (batchItemsToRemove.value.length === 0 || isBatchProcessing.value) return;
  
  isBatchProcessing.value = true;
  try {
    // Remove items one by one using existing API
    for (const item of batchItemsToRemove.value) {
      await subscriptionsStore.removeSubscription(item.feedId);
    }
    batchItemsToRemove.value = [];
    exitBatchMode();
  } catch (err) {
    console.error('批量取消订阅失败:', err);
  } finally {
    isBatchProcessing.value = false;
  }
};

const displayTitle = (item: SubscriptionListItemDto) => item.title || item.siteUrl || item.url || '未命名订阅';

const getUsername = (item: SubscriptionListItemDto) => {
  const url = item.siteUrl || item.url;
  if (!url) return '未知来源';
  try {
    const hostname = new URL(url).hostname.replace('www.', '');
    return hostname.length > 30 ? hostname.substring(0, 30) + '...' : hostname;
  } catch {
    return url.length > 30 ? url.substring(0, 30) + '...' : url;
  }
};

const getSubscriberCount = (item: SubscriptionListItemDto) => {
  const timeStr = item.lastUpdated || item.lastFetched;
  return timeStr ? formatRelativeTime(timeStr) : '暂无更新';
};

const getDescription = (item: SubscriptionListItemDto) => {
  const parts: string[] = [];
  if (item.lastFetched) parts.push(`上次抓取 ${formatRelativeTime(item.lastFetched)}`);

  const url = item.siteUrl && item.siteUrl !== item.url ? item.siteUrl : item.url;
  if (url) parts.push(url.length > 50 ? url.substring(0, 50) + '...' : url);

  return parts.length > 0 ? parts.join(' · ') : '暂无描述';
};

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
  placeholder.className = 'w-10 h-10 rounded-full bg-secondary/10 flex items-center justify-center';
  placeholder.innerHTML = '<svg class="w-5 h-5 text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 2v4m0 12v4M4.22 4.22l2.83 2.83m9.9 9.9 2.83 2.83M2 12h4m12 0h4M4.22 19.78l2.83-2.83m9.9-9.9 2.83-2.83"/></svg>';
  img.parentElement?.appendChild(placeholder);
};

onMounted(() => {
  if (!items.value.length) refresh();
});
</script>
