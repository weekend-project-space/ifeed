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

    <!-- 分页控件 -->
    <pagination
        v-if="items.length && !articlesLoading"
        :current-page="currentPage"
        :has-previous-page="hasPreviousPage"
        :has-next-page="hasNextPage"
        :disabled="articlesFetching"
        @prev-page="prevPage"
        @next-page="nextPage"
    />
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {normalizeArticle} from '../stores/articles/types';
import {useRecommendationsQuery} from '../queries/recommendations';

defineOptions({ name: 'HomePage' });

const props = defineProps<{page: number}>();

const route = useRoute();
const router = useRouter();
const currentPage = computed(() => props.page);
const recommendationsQuery = useRecommendationsQuery(currentPage);
const items = computed(() => (
  recommendationsQuery.data.value?.content ?? []
).map(normalizeArticle));
const articlesLoading = computed(() => recommendationsQuery.isPending.value);
const articlesFetching = computed(() => recommendationsQuery.isFetching.value);
const articleError = computed(() => {
  const error = recommendationsQuery.error.value;
  return error instanceof Error ? error.message : error ? '推荐文章加载失败' : null;
});
const totalPages = computed(() => recommendationsQuery.data.value?.totalPages ?? 0);
const hasNextPage = computed(() => currentPage.value < totalPages.value);
const hasPreviousPage = computed(() => currentPage.value > 1);

const goToPage = (page: number) => {
  router.push({
    query: { ...route.query, page: Math.max(1, page) }
  });
};

const refresh = () => recommendationsQuery.refetch();
const nextPage = () => hasNextPage.value && goToPage(currentPage.value + 1);
const prevPage = () => hasPreviousPage.value && goToPage(currentPage.value - 1);
</script>
