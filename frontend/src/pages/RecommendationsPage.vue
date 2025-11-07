<template>
  <div class="flex h-full flex-col gap-4 sm:gap-6">

    <section class="flex-1">
      <div v-if="articleError" class="rounded-xl border border-outline/30 bg-error/5 p-4 text-sm text-error sm:p-6">
        <p class="font-medium">推荐请求出错：{{ articleError }}</p>
        <button type="button"
          class="mt-3 inline-flex items-center gap-2 rounded-full bg-error text-error-foreground px-3 py-1.5 text-xs font-semibold transition hover:bg-error/90 sm:px-4"
          @click="refresh">
          重试一次
        </button>
      </div>
      <div v-else>
        <ArticleList title="智能推荐" subtitle="实时为你刷新阅读灵感" :items="articleItems" :loading="articlesLoading" empty-message="暂无推荐结果，尝试刷新或多收藏一些文章吧。"
          @select="handleSelect" @select-tag="handleSelectTag" @refresh="refresh" />
        <!-- 分页控件 -->
        <div
          class="mt-5 flex items-center justify-between border-t border-outline/20 pt-3 text-sm text-text-secondary sm:mt-6 sm:pt-4">
          <button type="button"
            class="rounded-full border border-outline/30 px-2.5 py-1.5 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/20 disabled:text-text-muted disabled:opacity-70 sm:px-3 sm:py-2"
            :disabled="!hasPrevious" @click="prevPage">
            上一页
          </button>
          <span>第 {{ currentPage }} 页</span>
          <button type="button"
            class="rounded-full border border-outline/30 px-2.5 py-1.5 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/20 disabled:text-text-muted disabled:opacity-70 sm:px-3 sm:py-2"
            :disabled="!hasNext" @click="nextPage">
            下一页
          </button>
        </div>
      </div>
    </section>

    <!-- debug panel removed -->
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import ArticleList from '../components/ArticleList.vue';
import { useArticlesStore } from '../stores/articles';

const SIZE = 20;

const route = useRoute();
const router = useRouter();
const articlesStore = useArticlesStore();

const { items, loading: articlesLoading, error: articleError, hasNextPage, hasPreviousPage, page } = storeToRefs(articlesStore);

const routePage = computed(() => {
  const raw = Array.isArray(route.query.page) ? route.query.page[0] : route.query.page;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
});

const recommendations = computed(() => items.value);
const currentPage = computed(() => page.value);
const hasNext = computed(() => hasNextPage.value);
const hasPrevious = computed(() => hasPreviousPage.value);

const loadRecommendations = async (targetPage = 1) => {
  if (!targetPage || targetPage < 1) {
    targetPage = 1;
  }
  try {
    const res = await articlesStore.fetchRecommendArticles({
      page: targetPage,
      size: SIZE
    });
    // fetched
    return res;
  } catch (err) {
    console.error('加载推荐文章失败:', err);
    throw err;
  }
};

const refresh = () => {
  loadRecommendations(routePage.value);
};

const articleItems = computed(() => recommendations.value);

const handleSelect = (articleId: string) => {
  router.push({ name: 'article-detail', params: { id: articleId } });
};

const handleSelectTag = (tag: string) => {
  if (!tag) {
    return;
  }
  router.push({ name: 'home', query: { tags: tag.toLowerCase() } });
};

onMounted(() => {
  // 监听路由参数变化
  watch(
    () => routePage.value,
    (page) => {
      loadRecommendations(page);
    },
    { immediate: true }
  );
});


function nextPage() {
  if (hasNextPage.value) {
    const nextPageNum = routePage.value + 1;
    router.push({
      query: {
        ...route.query,
        page: String(nextPageNum)
      }
    });
  }
}

function prevPage() {
  if (hasPreviousPage.value) {
    const prevPageNum = Math.max(1, routePage.value - 1);
    router.push({
      query: {
        ...route.query,
        page: String(prevPageNum)
      }
    });
  }
}
</script>
