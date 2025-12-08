<template>
  <div class="min-h-screen ">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-3xl font-semibold text-gray-900 dark:text-gray-100 mb-2">
          发现订阅源
        </h1>
        <p class="text-gray-600 dark:text-gray-400">
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
          <svg class="w-5 h-5 text-gray-700 dark:text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" rx="1"/>
            <rect x="14" y="3" width="7" height="7" rx="1"/>
            <rect x="14" y="14" width="7" height="7" rx="1"/>
            <rect x="3" y="14" width="7" height="7" rx="1"/>
          </svg>
          <h2 class="text-lg font-semibold text-gray-900 dark:text-gray-100">
            分类
          </h2>
        </div>
        
        <div class="flex flex-wrap gap-2">
          <button
              v-for="category in categories"
              :key="category.id"
              @click="selectedCategory = category.id"
              :class="[
                selectedCategory === category.id
                  ? 'bg-secondary text-white shadow-md'
                  : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 border border-gray-200 dark:border-gray-700',
                'px-4 py-2 rounded-full text-sm font-medium transition-all duration-200'
              ]"
          >
            <span class="flex items-center gap-2">
              <span>{{ category.icon }}</span>
              <span>{{ category.name }}</span>
              <span v-if="category.feedCount" class="text-xs opacity-75">({{ category.feedCount }})</span>
            </span>
          </button>
        </div>
      </div>

      <!-- Feed Grid -->
      <div>
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-gray-100">
            {{ selectedCategoryName }}
          </h2>
          <div class="flex items-center gap-2">
            <button
                v-for="view in viewModes"
                :key="view.id"
                @click="viewMode = view.id"
                :class="[
                  viewMode === view.id
                    ? 'text-secondary bg-secondary/10'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300',
                  'p-2 rounded-lg transition-colors'
                ]"
                :title="view.name"
            >
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path v-if="view.id === 'grid'" d="M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z"/>
                <path v-else d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="grid" :class="viewMode === 'grid' ? 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6' : 'grid-cols-1 gap-4'">
          <div v-for="i in 6" :key="i" class="bg-white dark:bg-gray-800 rounded-2xl p-6 animate-pulse">
            <div class="flex items-start gap-4">
              <div class="w-16 h-16 bg-gray-200 dark:bg-gray-700 rounded-xl flex-shrink-0"></div>
              <div class="flex-1 space-y-3">
                <div class="h-5 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
                <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-full"></div>
                <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-5/6"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Grid View -->
        <div v-else-if="viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <article
              v-for="feed in filteredFeeds"
              :key="feed.id"
              class="group bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-200 dark:border-gray-700 hover:shadow-lg hover:border-secondary/50 transition-all duration-200"
          >
            <!-- Feed Header -->
            <div class="flex items-start gap-4 mb-4">
              <img
                  :src="feed.favicon"
                  :alt="feed.name"
                  class="w-16 h-16 rounded-full object-cover bg-gray-100 dark:bg-gray-700"
                  @error="handleImageError"
              />
              <div class="flex-1 min-w-0">
                <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1 line-clamp-1 group-hover:text-secondary transition-colors">
                  {{ feed.name }}
                </h3>
                <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-1">
                  {{ feed.url }}
                </p>
              </div>
            </div>

            <!-- Feed Description -->
            <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-3 mb-4 h-[60px]">
              {{ feed.description || '暂无描述' }}
            </p>

            <!-- Feed Stats -->
            <div class="flex items-center gap-4 mb-4 text-sm text-gray-500 dark:text-gray-400">
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                {{ formatNumber(feed.subscribers) }}
              </span>
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
                {{ feed.articleCount }} 篇
              </span>
              <span>{{ feed.updateFrequency }}</span>
            </div>

            <!-- Subscribe Button -->
            <button
                @click="toggleSubscribe(feed)"
                :disabled="subscribing === feed.id"
                :class="[
                  feed.subscribed
                    ? 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                    : 'bg-secondary text-white hover:bg-secondary/90',
                  'w-full py-2.5 rounded-full font-medium text-sm transition-colors disabled:opacity-50'
                ]"
            >
              <span v-if="subscribing === feed.id" class="flex items-center justify-center gap-2">
                <svg class="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                </svg>
                处理中...
              </span>
              <span v-else>
                {{ feed.subscribed ? '已订阅' : '订阅' }}
              </span>
            </button>
          </article>
        </div>

        <!-- List View -->
        <div v-else class="space-y-3">
          <article
              v-for="feed in filteredFeeds"
              :key="feed.id"
              class="group bg-white dark:bg-gray-800 rounded-xl p-5 border border-gray-200 dark:border-gray-700 hover:shadow-md hover:border-secondary/50 transition-all duration-200"
          >
            <div class="flex items-start gap-4">
              <!-- Feed Icon -->
              <img
                  :src="feed.favicon"
                  :alt="feed.name"
                  class="w-14 h-14 rounded-full object-cover bg-gray-100 dark:bg-gray-700 flex-shrink-0"
                  @error="handleImageError"
              />

              <!-- Feed Info -->
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-4 mb-2">
                  <div class="flex-1 min-w-0">
                    <h3 class="text-base font-semibold text-gray-900 dark:text-gray-100 mb-1 line-clamp-1 group-hover:text-secondary transition-colors">
                      {{ feed.name }}
                    </h3>
                    <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-1 mb-2">
                      {{ feed.url }}
                    </p>
                  </div>
                  
                  <!-- Subscribe Button -->
                  <button
                      @click="toggleSubscribe(feed)"
                      :disabled="subscribing === feed.id"
                      :class="[
                        feed.subscribed
                          ? 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                          : 'bg-secondary text-white hover:bg-secondary/90',
                        'px-6 py-2 rounded-full font-medium text-sm transition-colors disabled:opacity-50 flex-shrink-0'
                      ]"
                  >
                    <span v-if="subscribing === feed.id" class="flex items-center gap-2">
                      <svg class="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                      </svg>
                      处理中
                    </span>
                    <span v-else>
                      {{ feed.subscribed ? '已订阅' : '订阅' }}
                    </span>
                  </button>
                </div>

                <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-3 h-[40px]">
                  {{ feed.description || '暂无描述' }}
                </p>

                <!-- Feed Stats -->
                <div class="flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
                  <span class="flex items-center gap-1">
                    <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                      <circle cx="9" cy="7" r="4"/>
                      <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
                    </svg>
                    {{ formatNumber(feed.subscribers) }} 订阅者
                  </span>
                  <span>•</span>
                  <span class="flex items-center gap-1">
                    <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                      <polyline points="14 2 14 8 20 8"/>
                    </svg>
                    {{ feed.articleCount }} 篇文章
                  </span>
                  <span>•</span>
                  <span>{{ feed.updateFrequency }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>

        <!-- Empty State -->
        <div v-if="!loading && filteredFeeds.length === 0" class="text-center py-16">
          <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center">
            <svg class="w-10 h-10 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
          </div>
          <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">未找到订阅源</h3>
          <p class="text-gray-600 dark:text-gray-400">尝试选择其他分类或调整搜索关键词</p>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="filteredFeeds.length > 0" class="mt-8">
        <Pagination
            :current-page="currentPage + 1"
            :has-previous-page="hasPreviousPage"
            :has-next-page="hasNextPage"
            :disabled="loading"
            @prev-page="prevPage"
            @next-page="nextPage"
        />
      </div>
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
            <SubscriptionsAddManual @success="handleManualAddSuccess" />
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
            <SubscriptionsAddOpml @success="handleOpmlSuccess" />
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { request } from '../api/client';
import type { PageResponse } from '../types/api';
import type { Category, CategoriesResponse, DiscoveryFeed } from '../types/discovery';
import { useSubscriptionsStore } from '../stores/subscriptions';
import SubscriptionsAddManual from './components/SubscriptionsAddManual.vue';
import SubscriptionsAddOpml from './components/SubscriptionsAddOpml.vue';
import Pagination from '../components/Pagination.vue';

// Types
interface Feed {
  id: string;
  name: string;
  url: string;
  description: string;
  favicon: string;
  category: string;
  subscribers: number;
  articleCount: number;
  updateFrequency: string;
  subscribed: boolean;
}

// State
const searchQuery = ref('');
const selectedCategory = ref('all');
const viewMode = ref<'grid' | 'list'>('grid');
const loading = ref(false);
const subscribing = ref<string | null>(null);

// Dialog states
const showManualAddDialog = ref(false);
const showOpmlDialog = ref(false);

// Stores
const subscriptionsStore = useSubscriptionsStore();

// Categories
const categories = ref<Category[]>([]);

// View modes
const viewModes = [
  { id: 'grid', name: '网格视图' },
  { id: 'list', name: '列表视图' },
];

// Feeds data
const feeds = ref<Feed[]>([]);
const currentPage = ref(0);
const totalPages = ref(0);

// Computed
const selectedCategoryName = computed(() => {
  const category = categories.value.find(c => c.id === selectedCategory.value);
  return category ? category.name : '全部';
});

const filteredFeeds = computed(() => {
  return feeds.value;
});

const hasNextPage = computed(() => {
  return currentPage.value < totalPages.value - 1;
});

const hasPreviousPage = computed(() => {
  return currentPage.value > 0;
});

// Methods
const formatNumber = (num: number): string => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M';
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K';
  }
  return num.toString();
};

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement;
  img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="%239CA3AF" stroke-width="2"%3E%3Ccircle cx="12" cy="12" r="3"/%3E%3Cpath d="M12 2v4m0 12v4M4.22 4.22l2.83 2.83m9.9 9.9 2.83 2.83M2 12h4m12 0h4M4.22 19.78l2.83-2.83m9.9-9.9 2.83-2.83"/%3E%3C/svg%3E';
};

const formatUpdateFrequency = (freq: string): string => {
  const map: Record<string, string> = {
    'REALTIME': '实时更新',
    'HOURLY': '每小时更新',
    'DAILY': '每日更新',
    'WEEKLY': '每周更新',
    'MONTHLY': '每月更新',
    'UNKNOWN': '更新频率未知'
  };
  return map[freq] || freq;
};

const mapDiscoveryFeedToFeed = (discoveryFeed: DiscoveryFeed): Feed => {
  return {
    id: discoveryFeed.feedId,
    name: discoveryFeed.name,
    url: discoveryFeed.url,
    description: discoveryFeed.description || '',
    favicon: discoveryFeed.favicon,
    category: discoveryFeed.category,
    subscribers: discoveryFeed.subscriberCount,
    articleCount: discoveryFeed.articleCount,
    updateFrequency: formatUpdateFrequency(discoveryFeed.updateFrequency),
    subscribed: discoveryFeed.subscribed,
  };
};

const loadCategories = async () => {
  try {
    const response = await request<CategoriesResponse>('/api/discovery/categories');
    categories.value = response.categories;
  } catch (error) {
    console.error('Failed to load categories:', error);
  }
};

const loadFeeds = async () => {
  loading.value = true;
  try {
    // Both browse and search now return Page<DiscoveryFeed>
    const endpoint = searchQuery.value.trim() 
      ? '/api/discovery/feeds/search' 
      : '/api/discovery/feeds';
    
    const params: Record<string, any> = {
      page: currentPage.value,
      size: 30
    };
    
    if (searchQuery.value.trim()) {
      params.q = searchQuery.value.trim();
    } else {
      params.sort = 'popular';
    }
    
    if (selectedCategory.value !== 'all') {
      params.category = selectedCategory.value;
    }
    
    const response = await request<PageResponse<DiscoveryFeed>>(endpoint, {
      query: params
    });
    
    feeds.value = response.content.map(mapDiscoveryFeedToFeed);
    totalPages.value = response.totalPages;
  } catch (error) {
    console.error('Failed to load feeds:', error);
    feeds.value = [];
  } finally {
    loading.value = false;
  }
};

const toggleSubscribe = async (feed: Feed) => {
  subscribing.value = feed.id;
  
  try {
    if (feed.subscribed) {
      // Unsubscribe
      await subscriptionsStore.removeSubscription(feed.id);
      feed.subscribed = false;
      feed.subscribers--;
    } else {
      // Subscribe
      await subscriptionsStore.addSubscription(feed.url, feed.id);
      feed.subscribed = true;
      feed.subscribers++;
    }
  } catch (error) {
    console.error('Failed to toggle subscription:', error);
  } finally {
    subscribing.value = null;
  }
};

const nextPage = () => {
  if (hasNextPage.value && !loading.value) {
    currentPage.value++;
    loadFeeds();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const prevPage = () => {
  if (hasPreviousPage.value && !loading.value) {
    currentPage.value--;
    loadFeeds();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

// Success handlers for dialogs
const handleManualAddSuccess = () => {
  showManualAddDialog.value = false;
  loadFeeds(); // Refresh feeds list
};

const handleOpmlSuccess = () => {
  showOpmlDialog.value = false;
  loadFeeds(); // Refresh feeds list
};

// Watchers
watch(selectedCategory, () => {
  currentPage.value = 0;
  loadFeeds();
});

watch(searchQuery, () => {
  currentPage.value = 0;
  loadFeeds();
});

// Lifecycle
onMounted(async () => {
  await loadCategories();
  await loadFeeds();
});
</script>

<style scoped>
.modal-enter-active, .modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from, .modal-leave-to {
  opacity: 0;
}
</style>
