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
        title="智能推荐"
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
        :disabled="articlesLoading"
        @prev-page="prevPage"
        @next-page="nextPage"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, watch} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import {storeToRefs} from 'pinia';
import {useRecommendArticlesStore} from '../stores/articles/recommendArticles';

const SIZE = 60;

const route = useRoute();
const router = useRouter();
const recommendStore = useRecommendArticlesStore();

const {
  items,
  loading: articlesLoading,
  error: articleError,
  hasNextPage,
  hasPreviousPage
} = storeToRefs(recommendStore);

const currentPage = computed(() => {
  const raw = Array.isArray(route.query.page) ? route.query.page[0] : route.query.page;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
});

const loadRecommendations = async (targetPage = 1) => {
  if (targetPage < 1) targetPage = 1;

  try {
    await recommendStore.fetchArticles({
      page: targetPage,
      size: SIZE
    });
  } catch (err) {
    console.error('加载推荐文章失败:', err);
  }
};

const refresh = () => loadRecommendations(currentPage.value);


const nextPage = () => {
  if (hasNextPage.value) {
    router.push({
      query: {...route.query, page: String(currentPage.value + 1)}
    });
  }
};

const prevPage = () => {
  if (hasPreviousPage.value) {
    router.push({
      query: {...route.query, page: String(Math.max(1, currentPage.value - 1))}
    });
  }
};

onMounted(() => {
  if (sessionStorage.getItem('origin-list') != route.path) {
    loadRecommendations(currentPage.value);
  }
  sessionStorage.removeItem('origin-list')
});

watch(
    () => currentPage.value,
    (page) => loadRecommendations(page)
);
</script>