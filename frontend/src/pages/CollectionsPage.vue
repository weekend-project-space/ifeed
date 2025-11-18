<!--collections.vue-->
<template>
  <div class="min-h-screen">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 py-4 sm:py-6">
      <!-- Page Header -->
      <div class="mb-6 sm:mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold text-text mb-2">我的收藏</h1>
        <p class="text-xs sm:text-sm text-text-secondary">保存你想稍后阅读的文章</p>
      </div>

      <!-- Controls Bar -->
      <div class="flex items-center justify-between mb-4 sm:mb-6">
        <div class="text-xs sm:text-sm text-text-secondary">
          {{ totalText }}
        </div>
        <button
            @click="refresh"
            :disabled="loading"
            class="p-2 hover:bg-surface-container rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label="刷新收藏列表"
        >
          <svg
              class="w-5 h-5 text-text-secondary transition-transform"
              :class="{ 'animate-spin': loading }"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
          >
            <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
          </svg>
        </button>
      </div>

      <!-- Error State -->
      <div v-if="error" class="mb-4 p-3 sm:p-4 bg-red-50 border border-red-200 rounded-lg">
        <div class="flex items-center gap-2 text-xs sm:text-sm text-red-800">
          <svg class="w-4 h-4 sm:w-5 sm:h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <span>{{ error }}</span>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="loading && !items.length" role="status" aria-live="polite" class="space-y-3 sm:space-y-4">
        <span class="sr-only">正在加载收藏列表</span>
        <div v-for="i in 3" :key="i" class="flex flex-col sm:flex-row gap-3 sm:gap-4 animate-pulse">
          <div class="w-full sm:w-52 h-40 sm:h-32 bg-surface-container rounded-lg flex-shrink-0"></div>
          <div class="flex-1 space-y-2 sm:space-y-3 py-1 sm:py-2">
            <div class="h-4 sm:h-5 bg-surface-container rounded w-3/4"></div>
            <div class="h-3 sm:h-4 bg-surface-container rounded w-1/2"></div>
            <div class="h-3 sm:h-4 bg-surface-container rounded w-full"></div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!items.length && !loading" class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
        <div class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
          <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
          </svg>
        </div>
        <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">还没有收藏的文章</h2>
        <p class="text-xs sm:text-sm text-text-secondary px-4">收藏你喜欢的文章，它们将显示在这里</p>
      </div>

      <!-- Collection Items -->
      <div v-else class="space-y-4 sm:space-y-5">
        <article v-for="item in items" :key="`${item.articleId}-${item.collectedAt}`">
          <router-link
              :to="`/articles/${item.articleId}`"
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
                  <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
                </svg>
              </div>

              <!-- Remove Button (Mobile) -->
              <button
                  @click.stop.prevent="remove(item.articleId)"
                  class="sm:hidden absolute top-2 right-2 p-2 bg-black/60 backdrop-blur-sm hover:bg-black/80 rounded-full transition-all"
                  aria-label="取消收藏"
              >
                <svg class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
              </button>
            </div>

            <!-- Content -->
            <div class="flex-1 min-w-0 flex flex-col py-0 sm:py-0.5">
              <h3 class="text-sm sm:text-base font-normal text-text line-clamp-2 mb-1.5 sm:mb-2 leading-normal">
                {{ item.title || '未命名文章' }}
              </h3>
              <div class="flex items-center gap-1.5 text-xs sm:text-sm text-text-secondary mb-1.5 sm:mb-2">
                <span v-if="item.feedTitle" class="truncate">{{ item.feedTitle }}</span>
                <span v-if="item.feedTitle && item.collectedAt" class="flex-shrink-0">•</span>
                <span v-if="item.collectedAt" class="flex-shrink-0">收藏于 {{ formatRelativeTime(item.collectedAt) }}</span>
              </div>
              <p v-if="item.summary" class="text-xs sm:text-sm text-text-secondary line-clamp-2 leading-relaxed">
                {{ item.summary }}
              </p>
            </div>

            <!-- Remove Button (Desktop) -->
            <button
                @click.stop.prevent="remove(item.articleId)"
                class="hidden sm:block self-start p-2 opacity-0 group-hover:opacity-100 hover:bg-surface-container rounded-full transition-all"
                aria-label="取消收藏"
            >
              <svg class="w-5 h-5 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </router-link>
        </article>
      </div>

      <!-- Pagination -->
      <nav v-if="items.length && !loading" class="flex items-center justify-center gap-2 mt-6 sm:mt-8 pt-4 sm:pt-6 border-t border-outline/10" aria-label="分页导航">
        <button
            @click="prevPage"
            :disabled="!hasPreviousPage || loading"
            class="p-2 hover:bg-surface-container rounded-full disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
            aria-label="上一页"
        >
          <svg class="w-5 h-5 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <div class="px-3 sm:px-4 py-1 text-xs sm:text-sm text-text-secondary">
          第 {{ page }} 页
        </div>
        <button
            @click="nextPage"
            :disabled="!hasNextPage || loading"
            class="p-2 hover:bg-surface-container rounded-full disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
            aria-label="下一页"
        >
          <svg class="w-5 h-5 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useCollectionsStore } from '../stores/collections';
import { formatRelativeTime } from '../utils/datetime';

const collectionsStore = useCollectionsStore();
const { items, page, total, loading, hasNextPage, hasPreviousPage, error } = storeToRefs(collectionsStore);

const refresh = async () => {
  if (loading.value) return;
  try {
    await collectionsStore.fetchCollections({page:0});
  } catch (err) {
    console.error('收藏刷新失败:', err);
  }
};

const nextPage = async () => {
  if (!hasNextPage.value || loading.value) return;
  try {
    await collectionsStore.fetchCollections({ page: page.value + 1 });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } catch (err) {
    console.error('加载下一页失败:', err);
  }
};

const prevPage = async () => {
  if (!hasPreviousPage.value || loading.value) return;
  try {
    await collectionsStore.fetchCollections({ page: Math.max(1, page.value - 1) });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } catch (err) {
    console.error('加载上一页失败:', err);
  }
};

const remove = async (articleId: string) => {
  try {
    await collectionsStore.removeCollection(articleId);
    // 如果当前页没有内容了且有上一页，自动跳转到上一页
    if (!items.value.length && hasPreviousPage.value) {
      await collectionsStore.fetchCollections({ page: Math.max(1, page.value - 1) });
    }
  } catch (err) {
    console.error('取消收藏失败:', err);
  }
};

const handleImageError = (e: Event) => {
  (e.target as HTMLImageElement).style.display = 'none';
};

const totalText = computed(() =>
    (total.value === null || total.value === 0) ? '暂无收藏' : `共 ${total.value} 条收藏`
);

onMounted(refresh);
</script>