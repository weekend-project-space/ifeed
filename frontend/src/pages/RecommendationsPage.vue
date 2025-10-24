<template>
  <div class="flex h-full flex-col gap-6 px-4 py-4 sm:px-6">
    <header class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-xl font-semibold text-text">智能推荐</h1>
        <p class="text-sm text-text-secondary">
<!--          聚合协同过滤、语义向量与流行度信号的候选集，-->
          实时为你刷新阅读灵感。
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <button type="button"
          class="inline-flex items-center gap-1 rounded-full border border-outline/40 bg-surface-container px-3 py-1.5 text-xs text-text-secondary transition hover:border-primary/50 hover:text-primary"
          @click="refresh" :disabled="articlesLoading">
          <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M4 10a6 6 0 0 1 10-4.24M16 10a6 6 0 0 1-10 4.24M4 6V3.5M4 3.5h2.5M4 3.5 6.5 6M16 14v2.5M16 16.5h-2.5M16 16.5 13.5 14" />
          </svg>
          <span>{{ articlesLoading ? '刷新中…' : '刷新' }}</span>
        </button>
      </div>
    </header>

    <section class="flex-1">
      <div v-if="articleError" class="rounded-xl border border-outline/30 bg-error/5 p-6 text-sm text-error">
        <p class="font-medium">推荐请求出错：{{ articleError }}</p>
        <button type="button"
          class="mt-3 inline-flex items-center gap-2 rounded-full bg-error text-error-foreground px-4 py-1.5 text-xs font-semibold transition hover:bg-error/90"
          @click="refresh">
          重试一次
        </button>
      </div>
      <div v-else-if="articlesLoading" class="space-y-4">
        <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
          <div v-for="i in 4" :key="`recommend-skeleton-${i}`"
            class="animate-pulse rounded-xl border border-outline/15 bg-surface-container px-5 py-6 shadow-sm shadow-black/0 transition">
            <div class="flex items-center justify-between gap-3">
              <div class="flex flex-1 items-center gap-2">
                <div class="h-6 w-20 rounded-full bg-outline/15" />
                <div class="h-3 w-10 rounded-full bg-outline/10" />
              </div>
              <div class="h-6 w-16 rounded-full bg-outline/15" />
            </div>
            <div class="mt-4 space-y-3">
              <div class="h-4 w-3/4 rounded-full bg-outline/15" />
              <div class="h-4 w-full rounded-full bg-outline/10" />
              <div class="h-4 w-5/6 rounded-full bg-outline/10" />
              <div class="h-4 w-1/2 rounded-full bg-outline/10" />
            </div>
            <div class="mt-6 flex flex-wrap gap-2">
              <div class="h-6 w-16 rounded-full bg-outline/10" />
              <div class="h-6 w-20 rounded-full bg-outline/10" />
              <div class="h-6 w-12 rounded-full bg-outline/10" />
            </div>
          </div>
        </div>
        <div
          class="flex items-center justify-center gap-2 rounded-xl border border-outline/20 bg-surface py-3 text-xs text-text-muted">
          <svg class="h-4 w-4 animate-spin text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="1.5">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"></circle>
            <path class="opacity-75" d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-linecap="round"></path>
          </svg>
          正在加载最新推荐…
        </div>
      </div>
      <div v-else-if="recommendations.length === 0"
        class="rounded-xl border border-outline/20 bg-surface-container p-8 text-center text-sm text-text-secondary">
        暂无推荐结果，尝试刷新或多收藏一些感兴趣的文章吧。
      </div>
      <div v-else>
        <ArticleList :items="articleItems" empty-message="暂无推荐结果，尝试刷新或多收藏一些文章吧。" @select="handleSelect"
          @toggle-favorite="handleToggleFavorite" @select-tag="handleSelectTag" />
        <!-- 分页控件 -->
        <div class="mt-6 flex items-center justify-between border-t border-outline/20 pt-4 text-sm text-text-secondary">
          <button type="button"
            class="rounded-full border border-outline/30 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/20 disabled:text-text-muted disabled:opacity-70"
            :disabled="!hasPrevious" @click="prevPage">
            上一页
          </button>
          <span>第 {{ currentPage }} 页</span>
          <button type="button"
            class="rounded-full border border-outline/30 px-3 py-2 font-medium text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-outline/20 disabled:text-text-muted disabled:opacity-70"
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
import { useCollectionsStore } from '../stores/collections';
import { useArticlesStore } from '../stores/articles';

const SIZE = 20;

const route = useRoute();
const router = useRouter();
const collectionsStore = useCollectionsStore();
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

const articleItems = computed(() =>
  recommendations.value.map((item) => ({
    ...item,
    collected: collectionsStore.isCollected(item.id)
  }))
);

const handleSelect = (articleId: string) => {
  router.push({ name: 'article-detail', params: { id: articleId } });
};

const handleToggleFavorite = async (articleId: string) => {
  const target = articleItems.value.find((item) => item.id === articleId);
  try {
    await collectionsStore.toggleCollection(articleId, { title: target?.title });
  } catch (err) {
    console.warn('收藏操作失败', err);
  }
};

const handleSelectTag = (tag: string) => {
  if (!tag) {
    return;
  }
  router.push({ name: 'home', query: { tags: tag.toLowerCase() } });
};

// 监听路由参数变化
watch(
  () => routePage.value,
  (page) => {
    loadRecommendations(page);
  },
  { immediate: true }
);

onMounted(async () => {
  if (!collectionsStore.items.length) {
    try {
      await collectionsStore.fetchCollections();
    } catch (err) {
      console.warn('收藏列表加载失败', err);
    }
  }
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
