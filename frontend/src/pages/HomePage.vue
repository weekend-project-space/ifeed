<template>
  <div class="mx-auto text-text">
    <!-- 错误提示：MD3 柔和警示卡片 -->
    <div v-if="articleError" class="mb-6 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400" role="alert">
      <p class="font-semibold mb-2">推荐请求出错：{{ articleError }}</p>
      <button
          type="button"
          class="inline-flex h-8 items-center justify-center rounded-full bg-red-600 px-4 text-xs font-semibold text-white shadow-xs transition hover:bg-red-700 active:scale-95"
          @click="refresh"
      >
        重试
      </button>
    </div>

    <!-- 文章列表组件 -->
    <article-list
        subtitle="实时为你刷新阅读灵感"
        :items="items"
        :loading="articlesLoading"
        empty-message="暂无推荐结果，尝试刷新或多收藏一些文章吧。"
        @refresh="refresh"
    />

    <!-- 分页加载指示 -->
    <div v-if="items.length" class="mt-8 flex flex-col items-center gap-2.5" aria-live="polite">
      <div v-if="isFetchingNextPage" class="text-xs text-text-secondary">正在加载更多推荐...</div>
      <template v-else-if="nextPageError">
        <p class="text-xs text-red-600 dark:text-red-400">加载更多失败，请重试。</p>
        <button
            type="button"
            class="inline-flex h-8 items-center justify-center rounded-full bg-surface-container px-4 text-xs font-semibold text-text shadow-xs hover:bg-surface-container-high transition"
            @click="loadMore"
        >
          重试
        </button>
      </template>
      <p v-else-if="!hasNextPage" class="text-xs text-text-secondary/60">已加载全部推荐</p>
    </div>

    <div ref="loadMoreSentinel" class="h-1" aria-hidden="true"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { normalizeArticle } from '../stores/articles/types';
import { useInfiniteRecommendationsQuery } from '../queries/recommendations';
import { useInfiniteScroll } from '../composables/infiniteScroll';
import ArticleList from '../components/ArticleList.vue';

defineOptions({ name: 'HomePage' });

const recommendationsQuery = useInfiniteRecommendationsQuery();
const items = computed(() => recommendationsQuery.data.value?.pages
    .flatMap((page) => page.content)
    .map(normalizeArticle) ?? []);
const articlesLoading = computed(() => recommendationsQuery.isPending.value);
const isFetchingNextPage = computed(() => recommendationsQuery.isFetchingNextPage.value);
const hasNextPage = computed(() => Boolean(recommendationsQuery.hasNextPage.value));
const articleError = computed(() => {
  const error = recommendationsQuery.error.value;
  if (items.value.length > 0) return null;
  return error instanceof Error ? error.message : error ? '推荐文章加载失败' : null;
});
const nextPageError = computed(() => recommendationsQuery.isFetchNextPageError.value);
const { sentinel: loadMoreSentinel, loadMore } = useInfiniteScroll({
  hasNextPage,
  isLoading: recommendationsQuery.isFetching,
  hasError: recommendationsQuery.isError,
  onLoadMore: () => recommendationsQuery.fetchNextPage(),
});

const refresh = () => recommendationsQuery.refetch();
</script>