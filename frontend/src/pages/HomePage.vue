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
        :disabled="articlesLoading"
        @prev-page="prevPage"
        @next-page="nextPage"
    />
  </div>
</template>

<script setup>
import {onMounted, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {storeToRefs} from 'pinia';
import {useRecommendArticlesStore} from '../stores/articles/recommendArticles';

defineOptions({ name: 'HomePage' });

const props = defineProps({
  page: { type: Number, default: 1 }
});

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

const loadData = async (targetPage = 1) => {
  try {
    await recommendStore.fetchArticles({
      page: Math.max(1, targetPage),
      size: SIZE
    });
  } catch (err) {
    console.error('加载推荐文章失败:', err);
  }
};

const goToPage = (page) => {
  router.push({
    query: { ...route.query, page: Math.max(1, page) }
  });
};

const refresh = () => loadData(props.page);
const nextPage = () => hasNextPage.value && goToPage(props.page + 1);
const prevPage = () => hasPreviousPage.value && goToPage(props.page - 1);

// 仅在 props.page 变化且是用户手动触发时监听
watch(() => props.page, (newPage) => {
  loadData(newPage);
});

// 组件首次挂载时加载，被 keep-alive 缓存后再次后退回来不会重复触发 onMounted
onMounted(() => {
  if (items.value.length === 0) {
    loadData(props.page);
  }
});
</script>
