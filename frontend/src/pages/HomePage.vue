<template>
  <div class="mx-auto">
    <!-- 错误提示 -->
    <div v-if="articleError" class="mb-6 p-4 rounded-lg bg-red-50 border border-red-200">
      <p class="text-sm text-red-800 font-medium mb-2">推荐请求出错：{{ articleError }}</p>
      <button
          type="button"
          class="px-4 py-2 text-sm font-medium rounded-md bg-red-600 text-white hover:bg-red-700 transition-colors"
          @click="refresh"
      >
        重试
      </button>
    </div>

    <!-- 文章列表 -->
    <article-list
        subtitle="实时为你刷新阅读灵感"
        :items="items"
        :loading="articlesLoading"
        empty-message="暂无推荐结果，尝试刷新或多收藏一些文章吧。"
        @refresh="refresh"
    />

    <div v-if="items.length" class="mt-6 flex flex-col items-center gap-3" aria-live="polite">
      <div v-if="isFetchingNextPage" class="text-sm text-text-secondary">正在加载更多推荐...</div>
      <template v-else-if="nextPageError">
        <p class="text-sm text-red-700">加载更多失败，请重试。</p>
        <button
            type="button"
            class="px-4 py-2 text-sm font-medium rounded-md bg-red-600 text-white hover:bg-red-700 transition-colors"
            @click="loadMore"
        >
          重试
        </button>
      </template>
      <p v-else-if="!hasNextPage" class="text-sm text-text-muted">已加载全部推荐</p>
    </div>

    <div ref="loadMoreSentinel" class="h-1" aria-hidden="true"></div>
  </div>
</template>

<script setup lang="ts">
import {computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch} from 'vue';
import {normalizeArticle} from '../stores/articles/types';
import {useInfiniteRecommendationsQuery} from '../queries/recommendations';

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
const loadMoreSentinel = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

const loadMore = async () => {
  if (!hasNextPage.value || isFetchingNextPage.value) return;
  try {
    await recommendationsQuery.fetchNextPage();
  } catch {
    // 错误状态由 Query 暴露，页面提供重试入口。
  }
};

const refresh = () => recommendationsQuery.refetch();

const observeSentinel = () => {
  observer?.disconnect();
  observer = null;
  if (!loadMoreSentinel.value) return;
  observer = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting) {
      void loadMore();
    }
  }, {rootMargin: '600px 0px'});
  observer.observe(loadMoreSentinel.value);
};

const disconnectObserver = () => {
  observer?.disconnect();
  observer = null;
};

const activateObserver = () => {
  if (!observer) observeSentinel();
};

watch(loadMoreSentinel, observeSentinel);
onMounted(activateObserver);
onActivated(activateObserver);
onDeactivated(disconnectObserver);
onBeforeUnmount(disconnectObserver);
</script>
