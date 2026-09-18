<template>
  <div class="min-h-screen">
    <div class="max-w-screen-lg mx-auto px-3 sm:px-6 py-4 sm:py-6">
      <!-- Page Header -->
      <div class="mb-6 sm:mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold text-text mb-2">我的收藏</h1>
      </div>

      <!-- Unauthenticated State -->
      <div v-if="!authStore.isAuthenticated"
        class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
        <div
          class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
          <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="1.5">
            <path
              d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z" />
          </svg>
        </div>
        <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">请先登录</h2>
        <p class="text-xs sm:text-sm text-text-secondary px-4 mb-6">登录后即可查看和管理你的收藏</p>
        <router-link to="/auth"
          class="inline-block px-6 py-2.5 bg-secondary text-white rounded-full hover:bg-secondary-hover transition-colors text-sm font-medium">
          前往登录
        </router-link>
      </div>

      <!-- Authenticated Content -->
      <template v-else>
        <!-- Controls Bar -->
        <div class="flex items-center justify-between mb-4 sm:mb-6">
          <div class="text-xs sm:text-sm text-text-secondary">
            {{ totalText }}
          </div>
          <button @click="refresh" :disabled="isFetching"
            class="p-2 hover:bg-surface-container rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label="刷新收藏列表">
            <svg class="w-5 h-5 text-text-secondary transition-transform" :class="{ 'animate-spin': isFetching }"
              viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
            </svg>
          </button>
        </div>

        <!-- Error State -->
        <div v-if="errorMessage" class="mb-4 p-3 sm:p-4 bg-red-50 border border-red-200 rounded-lg">
          <div class="flex items-center gap-2 text-xs sm:text-sm text-red-800">
            <svg class="w-4 h-4 sm:w-5 sm:h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <span>{{ errorMessage }}</span>
          </div>
        </div>

        <!-- Collection Items -->
        <article-card-list v-if="isPending || items.length" :loading="isPending" show-action :items="items" meta-field="collectedAt"
          meta-prefix="收藏于 " action-label="取消收藏">

          <template #action-dropdown="{ item, close }">
            <button @click.stop.prevent="handleOpenInNewTab(item); close()"
              class="w-full px-4 py-2 text-left text-sm text-text hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors flex items-center gap-2">
              在新标签页打开
            </button>

            <div class="border-t border-gray-200 dark:border-gray-700 my-1"></div>

            <button @click.stop.prevent="handleRemove(item); close()"
              class="w-full px-4 py-2 text-left text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors flex items-center gap-2">
              取消收藏
            </button>
          </template>
        </article-card-list>

        <!-- Empty State -->
        <div v-else-if="!items.length && !isPending"
          class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
          <div
            class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
            <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
            </svg>
          </div>
          <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">还没有收藏的文章</h2>
          <p class="text-xs sm:text-sm text-text-secondary px-4">收藏你喜欢的文章，它们将显示在这里</p>
        </div>

        <!-- Pagination -->
        <pagination v-if="items.length && !isPending" :current-page="page" :has-previous-page="hasPreviousPage"
          :has-next-page="hasNextPage" :disabled="isFetching" @prev-page="prevPage" @next-page="nextPage" />
      </template>
    </div>
  </div>

</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useMutation, useQueryClient } from '@tanstack/vue-query';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { confirm } from '../composables/confirm';
import { removeCollection } from '../api/collections';
import type { PageResponse } from '../types/api';
import { COLLECTIONS_PAGE_SIZE, type CollectionDto, collectionsQueryKey, useCollectionsQuery } from '../queries/collections';

const props = defineProps<{ page: number }>();
const router = useRouter();
const authStore = useAuthStore();
const queryClient = useQueryClient();
const userId = computed(() => authStore.user?.userId ?? 'anonymous');
const page = computed(() => props.page);
const collectionsQuery = useCollectionsQuery(page);
const items = computed(() => collectionsQuery.data.value?.content ?? []);
const total = computed(() => collectionsQuery.data.value?.totalElements ?? 0);
const isPending = computed(() => collectionsQuery.isPending.value);
const isFetching = computed(() => collectionsQuery.isFetching.value);
const errorMessage = computed(() => {
  const error = collectionsQuery.error.value;
  return error instanceof Error ? error.message : error ? '收藏列表加载失败' : '';
});
const totalPages = computed(() => collectionsQuery.data.value?.totalPages ?? 0);
const hasNextPage = computed(() => page.value < totalPages.value);
const hasPreviousPage = computed(() => page.value > 1);

const removeMutation = useMutation({
  mutationFn: (articleId: string) => removeCollection(articleId),
  onSuccess: (_, articleId) => {
    queryClient.setQueryData<PageResponse<CollectionDto>>(collectionsQueryKey(userId.value, page.value), (previous) => {
      if (!previous) return previous;
      return {
        ...previous,
        content: previous.content.filter(item => item.articleId !== articleId),
        totalElements: Math.max(0, previous.totalElements - 1),
        totalPages: Math.max(0, Math.ceil(Math.max(0, previous.totalElements - 1) / COLLECTIONS_PAGE_SIZE)),
      };
    });
    void Promise.all([
      queryClient.invalidateQueries({ queryKey: ['article'] }),
      queryClient.invalidateQueries({ queryKey: ['recommendations'] }),
      queryClient.invalidateQueries({ queryKey: ['search'] }),
      queryClient.invalidateQueries({ queryKey: ['feedArticles'] }),
      queryClient.invalidateQueries({ queryKey: ['subscriptionArticles'] }),
    ]);
  },
});

const refresh = () => collectionsQuery.refetch();

const navigateToPage = (target: number) => {
  if (target < 1 || isFetching.value) return;
  router.push({ name: 'collections', query: target > 1 ? { page: String(target) } : {} });
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const nextPage = () => hasNextPage.value && navigateToPage(page.value + 1);
const prevPage = () => hasPreviousPage.value && navigateToPage(Math.max(1, page.value - 1));

const handleOpenInNewTab = (item: any) => {
  if (item?.articleId) {
    const url = `/articles/${item.articleId}`;
    window.open(url, '_blank');
  }
};

const handleRemove = async  (item: CollectionDto) => {
  if (!item?.articleId) return;
  try {
    const confirmed = await confirm({
      title: '取消收藏',
      description: '确定要取消收藏这篇文章吗？'
    });
    if (confirmed) {
      await removeMutation.mutateAsync(item.articleId);
      if (items.value.length <= 1 && hasPreviousPage.value) {
        navigateToPage(Math.max(1, page.value - 1));
      }
    }
  } catch {
    // 用户取消，无需操作
  }
};

const totalText = computed(() =>
  total.value === 0 ? '暂无收藏' : `共 ${total.value} 条收藏`
);
</script>
