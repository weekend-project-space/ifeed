<template>
  <div class="mx-auto">

    <!-- 文章列表 -->
    <main>
      <!-- 错误提示 -->
      <div
          v-if="articleError"
          class="mt-6 p-4 rounded-lg bg-red-50 border border-red-200"
      >
        <p class="text-sm text-red-800 font-medium mb-2">请求出错：{{ articleError }}</p>
        <button
            type="button"
            class="px-4 py-2 text-sm font-medium rounded-md bg-red-600 text-white hover:bg-red-700 transition-colors"
            @click="refresh"
        >
          重试
        </button>
      </div>
      <article-list
          title="最新"
          :items="items"
          :loading="articlesLoading"
          @select-tag="handleSelectTag"
          @refresh="refresh"
      >
        <template #header>

          <!-- 分类筛选 -->
          <header class="mb-6">
            <div class="flex items-center gap-1.5 overflow-x-auto pb-2">
              <button
                  class="px-3 py-1 text-sm font-medium rounded-lg transition-colors whitespace-nowrap"
                  :class="!currentCategory ? 'bg-secondary text-secondary-foreground' : 'bg-secondary/5 text-secondary hover:bg-secondary/20'"
                  @click="clearCategoryFilter"
              >
                全部
              </button>

              <template v-if="insightsLoading">
                <span class="text-xs text-gray-500 px-3">加载中...</span>
              </template>
              <template v-else>
                <button
                    v-for="c in topCategories"
                    :key="c.category"
                    class="px-3 py-1 text-sm font-medium rounded-lg transition-colors whitespace-nowrap"
                    :class="currentCategory === c.category.toLowerCase() ? 'bg-secondary text-secondary-foreground' : 'bg-secondary/5 text-secondary hover:bg-secondary/20'"
                    @click="handleSelectCategory(c.category)"
                >
                  {{ c.category }}
                </button>
                <span v-if="!topCategories.length" class="text-sm text-gray-500 px-3">暂无分类</span>
              </template>
            </div>

            <!-- 标签筛选提示 -->
            <div v-if="activeTag" class="mt-3">
              <div class="inline-flex items-center gap-2 px-3 py-1 text-xs bg-secondary/10 rounded-lg border border-secondary/20">
                <span class="text-secondary">#{{ activeTag }}</span>
                <button
                    class="text-secondary hover:text-secondary/80 font-medium"
                    @click="clearTagFilter"
                >
                  ✕
                </button>
              </div>
            </div>
          </header>
        </template>
        <template #action>
          <router-link class="p-2 text-sm text-primary font-medium rounded-lg transition-colors  hover:bg-surface-container" to="/feeds/channels">管理</router-link>
        </template>
        <template #empty>
            <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center">
              <svg class="w-8 h-8 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
              </svg>
            </div>
            <h2 class="text-lg font-medium text-gray-900 dark:text-gray-100 mb-2">还没有订阅</h2>
            <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">添加你感兴趣的订阅源开始使用</p>
          <router-link
              to="/discover"
              class="inline-flex items-center gap-2 px-5 py-2 text-sm font-medium text-white bg-secondary hover:bg-secondary/90 rounded-full transition-colors"
          >
            添加订阅
          </router-link>
        </template>
      </article-list>

      <!-- 分页 -->
      <pagination
          v-if="items.length && !articlesLoading"
          :current-page="currentPage"
          :has-previous-page="hasPreviousPage"
          :has-next-page="hasNextPage"
          :disabled="articlesLoading"
          @prev-page="prevPage"
          @next-page="nextPage"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, watch} from 'vue';
import {storeToRefs} from 'pinia';
import {useRoute, useRouter} from 'vue-router';
import {useSubscriptionArticlesStore} from '../stores/articles/subscriptionArticles';

const router = useRouter();
const route = useRoute();
const subscriptionStore = useSubscriptionArticlesStore();

const {
  items,
  hasNextPage,
  hasPreviousPage,
  loading: articlesLoading,
  error: articleError
} = storeToRefs(subscriptionStore);

const { insights, insightsLoading } = storeToRefs(subscriptionStore);

const topCategories = computed(() => insights.value.categories ?? []);

// 当前页码
const currentPage = computed(() => {
  const raw = Array.isArray(route.query.page) ? route.query.page[0] : route.query.page;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
});

// 当前标签
const activeTag = computed(() => {
  const raw = Array.isArray(route.query.tags) ? route.query.tags[0] : route.query.tags;
  return typeof raw === 'string' && raw.trim() ? raw.trim() : null;
});

// 当前分类
const currentCategory = computed(() => {
  const raw = route.query.category as string | undefined;
  return raw?.toLowerCase() ?? '';
});

// 构建查询参数
const buildQuery = (overrides?: { page?: number; tags?: string | null; category?: string | null }) => {
  const query: Record<string, string> = {};

  const tag = overrides?.hasOwnProperty('tags') ? overrides.tags : activeTag.value;
  if (tag) query.tags = tag;

  const category = overrides?.hasOwnProperty('category') ? overrides.category : currentCategory.value;
  if (category) query.category = category;

  const page = overrides?.page ?? currentPage.value;
  if (page > 1) query.page = String(page);

  return query;
};

// 加载数据
const loadData = async () => {
  try {
    await subscriptionStore.fetchArticles({
      size: 20,
      page: currentPage.value,
      tags: activeTag.value,
      category: currentCategory.value || undefined
    });
  } catch (err) {
    console.warn('订阅数据加载失败', err);
  }
};

const refresh = () => loadData();

// 页面跳转
const navigateToPage = (target: number) => {
  if (target < 1) return;
  router.push({ name: 'feedsSubscriptions', query: buildQuery({ page: target }) });
};

const nextPage = () => {
  if (hasNextPage.value) navigateToPage(currentPage.value + 1);
};

const prevPage = () => {
  if (hasPreviousPage.value) navigateToPage(Math.max(1, currentPage.value - 1));
};


// 标签筛选
const handleSelectTag = (tag: string) => {
  if (!tag) return;
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({ page: 1, tags: tag.toLowerCase() })
  });
};

const clearTagFilter = () => {
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({ tags: null, page: 1 })
  });
};

// 分类筛选
const handleSelectCategory = (category: string) => {
  if (!category) return;
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({ page: 1, category: category.toLowerCase(), tags: null })
  });
};

const clearCategoryFilter = () => {
  router.push({
    name: 'feedsSubscriptions',
    query: buildQuery({ page: 1, category: null })
  });
};

onMounted(() => {
  if(sessionStorage.getItem('origin-list')!=route.path){
    loadData();
    subscriptionStore.fetchInsights();
  }
  sessionStorage.removeItem('origin-list')
});

watch(
    () => [currentPage.value, activeTag.value, currentCategory.value],
    () => loadData()
);
</script>