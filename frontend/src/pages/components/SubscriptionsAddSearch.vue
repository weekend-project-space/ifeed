<template>
  <div>
    <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
      输入关键词或网址进行搜索
    </p>
    <!-- Search Header -->
    <div class="mb-6">
      <form @submit.prevent="handleSearch" class="relative">
        <div class="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
          <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
          </svg>
        </div>
        <input
            v-model.trim="searchQuery"
            type="search"
            placeholder="输入关键词或网址..."
            class="w-full pl-11 pr-4 py-3 text-base text-gray-900 dark:text-gray-100 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-full shadow-sm focus:outline-none focus:border-secondary focus:ring-1 focus:ring-secondary transition-shadow"
        />
        <button
            type="submit"
            class="absolute right-2 top-1.5 bottom-1.5 px-4 text-sm font-medium text-white bg-secondary dark:bg-secondary/10 rounded-full hover:bg-secondary/90 transition-colors disabled:opacity-50"
            :disabled="searchLoading">
          {{ searchLoading ? '搜索中...' : '搜索' }}
        </button>
      </form>

      <p v-if="searchError" class="mt-2 text-sm text-red-600 dark:text-red-400">
        {{ searchError }}
      </p>
    </div>

    <!-- Results List -->
    <div v-if="searchResults.length" class="space-y-6">
      <div class="text-sm text-gray-500 dark:text-gray-400">
        找到约 {{ searchResults.length }} 个结果
      </div>

      <div
          v-for="feed in searchResults"
          :key="feed.feedId"
          class="group"
      >
        <div class="flex gap-4">
          <!-- Avatar/Icon -->
          <div class="flex-shrink-0 pt-1">
            <img
                v-if="feed.avatar"
                :src="feed.avatar"
                :alt="displayTitle(feed)"
                class="w-8 h-8 rounded-full "
                @error="handleImageError"
            />
            <div
                v-else
                class="w-8 h-8 rounded-full bg-secondary/10 flex items-center justify-center text-secondary text-sm font-bold"
            >
              {{ displayTitle(feed).charAt(0).toUpperCase() }}
            </div>
          </div>

          <div class="flex-1 min-w-0 space-y-2">
            <router-link :to="'/feeds/'+feed.feedId" class="space-y-1 ">
              <!-- Site Info -->
              <div class="flex items-center gap-2 text-sm text-gray-800 dark:text-gray-300 ">
                <span class="font-medium truncate">{{ displayTitle(feed) }}</span>
                <span class="text-gray-400 text-xs">•</span>
                <span class="text-gray-500 dark:text-gray-400 text-xs truncate max-w-[15rem]">{{ displaySiteUrl(feed) }}</span>
              </div>
              <!-- Title Link -->
              <h3 class="text-base font-medium text-gray-900 dark:text-gray-100 hover:underline">
                {{ displayTitle(feed) }}
              </h3>
            </router-link>
            <!-- Description -->
            <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 custom-cursor-on-hover ">
              <span v-if="feed.lastUpdated" class=" mr-2">{{ formatRecentText(feed.lastUpdated) }} —</span>
              {{ feed.description || `${feed.title} 的订阅源。包含 ${feed.subscriberCount ?? 0} 位订阅者。` }}
            </p>
            <!-- Actions -->
            <div>
              <button
                  v-if="isSubscribed(feed)"
                  type="button"
                  class="text-sm font-medium cursor-default"
              >
                已订阅
              </button>
              <button
                  v-else
                  type="button"
                  class="px-4 py-1.5 text-sm font-medium text-secondary bg-secondary/10 rounded-full hover:bg-secondary/20 transition-colors disabled:opacity-50"
                  :disabled="subscriptionsStore.submitting"
                  @click="subscribeFromSearch(feed)">
                订阅
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div
        v-else-if="hasSearched && !searchLoading && !searchError"
        class="py-12 text-center"
    >
      <p class="text-gray-500 dark:text-gray-400">
        未找到相关结果，请尝试其他关键词
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onUnmounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {storeToRefs} from 'pinia';
import {
  useSubscriptionsStore,
  type SubscriptionListItemDto,
  type SubscriptionSearchResultDto
} from '../../stores/subscriptions';
import {formatRelativeTime} from '../../utils/datetime';

const subscriptionsStore = useSubscriptionsStore();
const {items, searchResults, searchLoading, searchError} = storeToRefs(subscriptionsStore);
const router = useRouter();

const searchQuery = ref('');
const hasSearched = ref(false);

const subscribedFeedIds = computed(() => new Set(items.value.map((item) => item.feedId)));

const isSubscribed = (feed: SubscriptionSearchResultDto | SubscriptionListItemDto) => {
  if ('subscribed' in feed) return feed.subscribed;
  return subscribedFeedIds.value.has(feed.feedId);
};

const resetSearchState = () => {
  searchQuery.value = '';
  hasSearched.value = false;
  subscriptionsStore.clearSearchResults();
};

watch(searchQuery, (value) => {
  if (!value || !value.trim()) {
    hasSearched.value = false;
    subscriptionsStore.clearSearchResults();
  } else if (searchError.value) {
    searchError.value = null;
  }
});

const handleSearch = async () => {
  try {
    if (!searchQuery.value.trim()) {
      hasSearched.value = false;
      await subscriptionsStore.searchSubscriptions(searchQuery.value);
      return;
    }
    await subscriptionsStore.searchSubscriptions(searchQuery.value);
    hasSearched.value = true;
  } catch (err) {
    // 错误信息由 store 维护
  }
};

const viewArticles = (feedId: string) => {
  router.push({name: 'feed', params: {feedId}});
};

const subscribeFromSearch = async (feed: SubscriptionSearchResultDto) => {
  try {
    if (isSubscribed(feed)) return;
    await subscriptionsStore.addSubscription(feed.url, feed.feedId);
    feed.subscribed = true;
    feed.subscriberCount = (feed.subscriberCount ?? 0) + 1;
  } catch (err) {
    // 错误信息由 store 维护
  }
};

const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement;
  target.style.display = 'none';
};

const displayTitle = (item: SubscriptionListItemDto | SubscriptionSearchResultDto) =>
    item.title || item.siteUrl || item.url;

const displaySiteUrl = (item: SubscriptionListItemDto | SubscriptionSearchResultDto) => item.siteUrl || item.url;

const formatRecentText = (value?: string | null) => (value ? formatRelativeTime(value) : '');

onUnmounted(() => {
  resetSearchState();
});
</script>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}

.animate-fade-in-up {
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
