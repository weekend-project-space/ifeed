<!--collections.vue-->
<template>
  <div class="min-h-screen">
    <div class="max-w-screen-lg mx-auto px-4 sm:px-6 py-4 sm:py-6">
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


      <!-- Collection Items -->
      <article-card-list
          v-if="loading || items.length"
          :loading="loading"
          :items="items"
          meta-field="collectedAt"
          meta-prefix="收藏于 "
          action-label="取消收藏"
          @action="item=>remove(item.articleId)"
      >
        <template #action-icon-mobile>
          <svg class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
        </template>
        <template #action-icon-desktop>
          <svg class="w-5 h-5 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
        </template>
      </article-card-list>
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



      <!-- Pagination -->
      <pagination
          v-if="items.length && !loading"
          :current-page="page"
          :has-previous-page="hasPreviousPage"
          :has-next-page="hasNextPage"
          :disabled="loading"
          @prev-page="prevPage"
          @next-page="nextPage"
      />
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