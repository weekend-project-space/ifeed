<!--search.vue-->
<template>
  <div class="min-h-screen">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 py-4 sm:py-6">
      <!-- Page Header -->
      <div class="mb-6 sm:mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold text-text mb-2">
          {{ hasQuery ? '搜索结果' : '搜索' }}
        </h1>
        <p v-if="hasQuery" class="text-xs sm:text-sm text-text-secondary">
          关键词: "{{ searchQuery }}" · {{ searchTotalText }}
        </p>
      </div>

      <!-- Controls Bar -->
      <div class="flex items-center justify-between gap-4 mb-4 sm:mb-6 pb-4 border-b border-outline/20 flex-wrap">
        <!-- Search Type Toggle -->
        <div class="flex items-center gap-1 bg-surface-container rounded-full p-1">
          <button
              type="button"
              class="px-3 sm:px-4 py-1.5 sm:py-2 rounded-full text-xs sm:text-sm font-medium transition-colors"
              :class="searchType === 'keyword' ? 'bg-secondary text-secondary-foreground' : 'text-text '"
              @click="setSearchType('keyword')"
          >
            关键词匹配
          </button>
          <button
              type="button"
              class="px-3 sm:px-4 py-1.5 sm:py-2 rounded-full text-xs sm:text-sm font-medium transition-colors"
              :class="searchType === 'semantic' ? 'bg-secondary text-secondary-foreground' : 'text-text '"
              @click="setSearchType('semantic')"
          >
            语义匹配
          </button>
        </div>

        <!-- Back Button -->
        <button
            class="flex items-center gap-2 px-3 sm:px-4 py-1.5 sm:py-2 hover:bg-surface-container rounded-lg transition-colors text-xs sm:text-sm font-medium text-text"
            @click="goBackToHome"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          <span>返回推荐</span>
        </button>
      </div>

      <!-- Empty State (No Query) -->
      <div v-if="!hasQuery" class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
        <div class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
          <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.35-4.35"/>
          </svg>
        </div>
        <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">开始搜索</h2>
        <p class="text-xs sm:text-sm text-text-secondary px-4">输入关键词查找你感兴趣的文章</p>
      </div>

      <!-- Loading State -->
      <div v-else-if="searchLoading" role="status" aria-live="polite" class="space-y-3 sm:space-y-4">
        <span class="sr-only">正在搜索</span>
        <div v-for="i in 3" :key="i" class="flex flex-col sm:flex-row gap-3 sm:gap-4 animate-pulse">
          <div class="w-full sm:w-52 h-40 sm:h-32 bg-surface-container rounded-lg flex-shrink-0"></div>
          <div class="flex-1 space-y-2 sm:space-y-3 py-1 sm:py-2">
            <div class="h-4 sm:h-5 bg-surface-container rounded w-3/4"></div>
            <div class="h-3 sm:h-4 bg-surface-container rounded w-1/2"></div>
            <div class="h-3 sm:h-4 bg-surface-container rounded w-full"></div>
          </div>
        </div>
      </div>

      <!-- Search Results -->
      <div v-else>
        <!-- Error State -->
        <div v-if="searchError" class="mb-4 p-3 sm:p-4 bg-red-50 border border-red-200 rounded-lg">
          <div class="flex items-center gap-2 text-xs sm:text-sm text-red-800">
            <svg class="w-4 h-4 sm:w-5 sm:h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <span>{{ searchError }}</span>
          </div>
        </div>

        <!-- Empty Results -->
        <div v-if="searchArticleItems.length === 0" class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
          <div class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
            <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
          </div>
          <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">未找到相关结果</h2>
          <p class="text-xs sm:text-sm text-text-secondary px-4">换个关键词试试</p>
        </div>

        <!-- Results List -->
        <div v-else class="space-y-4 sm:space-y-5">
          <article v-for="item in searchArticleItems" :key="item.id">
            <router-link
                :to="`/articles/${item.id}`"
                @click="recordHistory(item.id)"
                class="group flex flex-col sm:flex-row gap-3 sm:gap-4 hover:bg-surface-container/50 -mx-2 px-2 py-2 rounded-lg transition-colors"
            >
              <!-- Thumbnail -->
              <div class="relative flex-shrink-0 w-full sm:w-52 h-40 sm:h-32 rounded-lg overflow-hidden bg-surface-container">
                <img
                    v-if="item.thumbnail"
                    :src="item.thumbnail"
                    :alt="item.title || '文章缩略图'"
                    class="w-full h-full object-cover"
                    loading="lazy"
                    referrerpolicy="no-referrer"
                    @error="handleImageError"
                />
                <div v-else class="w-full h-full flex items-center justify-center">
                  <svg class="w-8 h-8 sm:w-10 sm:h-10 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
                  </svg>
                </div>

                <!-- Menu Button (Mobile) -->
                <button
                    @click.stop.prevent="handleMenuClick(item)"
                    class="sm:hidden absolute top-2 right-2 p-2 bg-black/60 backdrop-blur-sm hover:bg-black/80 rounded-full transition-all"
                    aria-label="文章选项菜单"
                >
                  <svg class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                    <circle cx="12" cy="12" r="1.5"/>
                    <circle cx="12" cy="5" r="1.5"/>
                    <circle cx="12" cy="19" r="1.5"/>
                  </svg>
                </button>
              </div>

              <!-- Content -->
              <div class="flex-1 min-w-0 flex flex-col py-0 sm:py-0.5">
                <h3 class="text-sm sm:text-base font-normal text-text line-clamp-2 mb-1.5 sm:mb-2 leading-normal">
                  {{ item.title }}
                </h3>
                <div class="flex items-center gap-1.5 text-xs sm:text-sm text-text-secondary mb-1.5 sm:mb-2">
                  <span v-if="item.feedTitle" class="truncate">{{ item.feedTitle }}</span>
                  <span v-if="item.feedTitle && item.timeAgo" class="flex-shrink-0">•</span>
                  <span v-if="item.timeAgo" class="flex-shrink-0">{{ item.timeAgo }}</span>
                </div>
                <p v-if="item.summary" class="text-xs sm:text-sm text-text-secondary line-clamp-2 leading-relaxed">
                  {{ item.summary }}
                </p>
              </div>

              <!-- Menu Button (Desktop) -->
              <button
                  @click.stop.prevent="handleMenuClick(item)"
                  class="hidden sm:block self-start p-2 opacity-0 group-hover:opacity-100 hover:bg-surface-container rounded-full transition-all"
                  aria-label="文章选项菜单"
              >
                <svg class="w-5 h-5 text-text-secondary" viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="12" cy="12" r="1.5"/>
                  <circle cx="12" cy="5" r="1.5"/>
                  <circle cx="12" cy="19" r="1.5"/>
                </svg>
              </button>
            </router-link>
          </article>
        </div>

        <!-- Pagination -->
        <pagination
            v-if="searchArticleItems.length && !loading"
            :current-page="page"
            :has-previous-page="hasPreviousPage"
            :has-next-page="hasNextPage"
            :disabled="loading"
            @prev-page="prevPage"
            @next-page="nextPage"
        />

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import { useArticlesStore } from '../stores/articles';
import { useSearchStore, type SearchType } from '../stores/search';

const router = useRouter();
const route = useRoute();
const articlesStore = useArticlesStore();
const searchStore = useSearchStore();

const {
  results,
  page,
  hasNextPage,
  hasPreviousPage,
  total,
  loading: searchLoading,
  error: searchError
} = storeToRefs(searchStore);

const searchQuery = computed(() => {
  const q = route.query.q;
  return typeof q === 'string' ? q.trim() : '';
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

const hasQuery = computed(() => Boolean(searchQuery.value));

const searchArticleItems = computed(() =>
    results.value.map((item) => ({
      id: item.id,
      title: item.title ?? '未命名文章',
      summary: item.summary ?? '暂无摘要',
      thumbnail: item.thumbnail,
      feedTitle: item.feedTitle,
      timeAgo: item.timeAgo,
    }))
);

const currentPage = computed(() => page.value);
const hasNext = computed(() => hasNextPage.value);
const hasPrevious = computed(() => hasPreviousPage.value);
const searchTotalText = computed(() => `${total.value ?? 0} 条结果`);

const buildSearchQuery = (overrides?: { page?: number; type?: SearchType }) => {
  const query: Record<string, string> = {};
  if (searchQuery.value) {
    query.q = searchQuery.value;
  }
  const nextType = overrides?.type ?? searchType.value;
  if (nextType !== 'keyword') {
    query.type = nextType;
  }
  const nextPage = overrides?.page ?? routePage.value;
  if (nextPage > 1) {
    query.page = String(nextPage);
  }
  const feedId = typeof route.query.feedId === 'string' ? route.query.feedId : undefined;
  if (feedId) query.feedId = feedId;

  const tag = typeof route.query.tags === 'string' ? route.query.tags : undefined;
  if (tag) query.tags = tag;

  const category = typeof route.query.category === 'string' ? route.query.category : undefined;
  if (category) query.category = category;

  return query;
};

const loadData = async () => {
  if (!hasQuery.value) {
    searchStore.clear();
    return;
  }

  try {
    await searchStore.searchArticles({
      query: searchQuery.value,
      page: routePage.value,
      type: searchType.value
    });
  } catch (err) {
    console.error('搜索数据加载失败:', err);
  }
};

const navigateToPage = (target: number) => {
  if (target < 1 || !hasQuery.value) return;
  router.push({ name: 'search', query: buildSearchQuery({ page: target }) });
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const nextPage = () => {
  if (!hasNext.value) return;
  navigateToPage(routePage.value + 1);
};

const prevPage = () => {
  if (!hasPrevious.value) return;
  navigateToPage(Math.max(1, routePage.value - 1));
};

const setSearchType = (type: SearchType) => {
  if (type === searchType.value) return;
  router.push({ name: 'search', query: buildSearchQuery({ page: 1, type }) });
};

const goBackToHome = () => {
  const query: Record<string, string> = {};
  const feedId = typeof route.query.feedId === 'string' ? route.query.feedId : undefined;
  if (feedId) query.feedId = feedId;

  const tag = typeof route.query.tags === 'string' ? route.query.tags : undefined;
  if (tag) query.tags = tag;

  const category = typeof route.query.category === 'string' ? route.query.category : undefined;
  if (category) query.category = category;

  router.push({ name: 'home', query });
};

const recordHistory = (articleId: string) => {
  articlesStore.recordHistory(articleId);
};

const handleMenuClick = (item: any) => {
  console.log('菜单点击:', item);
};

const handleImageError = (e: Event) => {
  (e.target as HTMLImageElement).style.display = 'none';
};

watch(
    () => [searchQuery.value, searchType.value, routePage.value],
    () => {
      loadData();
    }
);

onMounted(() => {
  loadData();
});
</script>