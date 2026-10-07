<!--search.vue-->
<template>
  <div class="">
    <div class="max-w-screen-lg mx-auto px-4 sm:px-6 py-4 sm:py-6">
      <!-- Page Header -->
      <div class="mb-6 sm:mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold text-text mb-2">
          {{ hasQuery ? '搜索结果' : '搜索' }}
        </h1>
        <p v-if="hasQuery" class="text-xs sm:text-sm text-text-secondary">
          关键词: "{{ searchQuery }}"
          <span v-if="totalElements !== null" class="ml-2">· {{ totalElements }} 条结果</span>
        </p>
      </div>

      <!-- Controls Bar -->
      <div class="flex items-center justify-between gap-4 mb-4 sm:mb-6 pb-4 border-b border-outline/20 flex-wrap">
        <!-- Search Type Toggle -->
        <div class="flex items-center gap-1 bg-surface-container rounded-full p-1">
          <button type="button"
            class="px-3 sm:px-4 py-1.5 sm:py-2 rounded-full text-xs sm:text-sm font-medium transition-colors"
            :class="searchType === 'semantic' ? 'bg-secondary text-secondary-foreground' : 'text-text'"
            @click="setSearchType('semantic')">
            语义匹配
          </button>
          <button type="button"
            class="px-3 sm:px-4 py-1.5 sm:py-2 rounded-full text-xs sm:text-sm font-medium transition-colors"
            :class="searchType === 'keyword' ? 'bg-secondary text-secondary-foreground' : 'text-text'"
            @click="setSearchType('keyword')">
            关键词匹配
          </button>
        </div>

        <!-- Back Button -->
        <button
          class="flex items-center gap-2 px-3 sm:px-4 py-1.5 sm:py-2 hover:bg-surface-container rounded-lg transition-colors text-xs sm:text-sm font-medium text-text"
          @click="goBackToHome">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          <span>返回推荐</span>
        </button>
      </div>

      <!-- Empty State (No Query) -->
      <div v-if="!hasQuery" class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
        <div
          class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
          <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="1.5">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
        </div>
        <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">开始搜索</h2>
        <p class="text-xs sm:text-sm text-text-secondary px-4">输入关键词查找你感兴趣的文章</p>
      </div>

      <!-- Error State -->
      <div v-else-if="searchError" role="alert" class="rounded-lg border border-red-200 bg-red-50 p-3 text-xs text-red-800 sm:p-4 sm:text-sm">
        <div class="flex items-center justify-between gap-3">
          <span>{{ searchError }}</span>
          <button type="button" class="shrink-0 underline" :disabled="searchFetching" @click="searchQueryResult.refetch()">重试</button>
        </div>
      </div>

      <!-- Loading or Results List -->
      <div v-else-if="searchLoading || searchArticleItems.length > 0">
        <article-card-list :loading="searchLoading" :items="searchArticleItems" meta-field="timeAgo"
          show-action action-label="文章选项菜单">
          <template #empty-thumbnail>
            <svg class="w-8 h-8 sm:w-10 sm:h-10 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5">
              <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
              <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
            </svg>
          </template>
          <template #action-dropdown="{ item, close }">
            <router-link :to="`/articles/${item.articleId}`" target="_blank" class="menu-action" @click="close">在新标签页打开</router-link>
          </template>
        </article-card-list>

        <!-- Pagination -->
        <pagination v-if="searchArticleItems.length > 0 && !searchLoading" :current-page="routePage"
          :has-previous-page="hasPreviousPage" :has-next-page="hasNextPage" :disabled="searchFetching"
          @prev-page="prevPage" @next-page="nextPage" />
      </div>

      <!-- Error or Empty State -->
      <div v-else>
        <!-- Empty Results -->
        <div class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
          <div
            class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
            <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </div>
          <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">未找到相关结果</h2>
          <p class="text-xs sm:text-sm text-text-secondary px-4">换个关键词试试</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useSearchQuery, type SearchType, type Source } from '../queries/search';

defineOptions({name: 'SearchPage'});

const props = defineProps<{
  query: string;
  type: SearchType;
  source?: Source;
  page: number;
  feedId: string | null;
  tags: string | null;
  category: string | null;
}>();
const router = useRouter();
const searchQuery = computed(() => props.query);
const searchType = computed(() => props.type);
const source = computed(() => props.source);
const routePage = computed(() => props.page);
const filters = {
  feedId: computed(() => props.feedId),
  tags: computed(() => props.tags),
  category: computed(() => props.category),
};

const hasQuery = computed(() => Boolean(searchQuery.value));

const searchQueryResult = useSearchQuery(searchQuery, searchType, source, routePage, filters);
const searchLoading = computed(() => searchQueryResult.isPending.value);
const searchFetching = computed(() => searchQueryResult.isFetching.value);
const searchError = computed(() => {
  const error = searchQueryResult.error.value;
  return error instanceof Error ? error.message : error ? '搜索失败' : null;
});
const results = computed(() => searchQueryResult.data.value?.content ?? []);
const totalPages = computed(() => searchQueryResult.data.value?.totalPages ?? 0);
const totalElements = computed(() => searchQueryResult.data.value?.totalElements ?? null);
const hasNextPage = computed(() => routePage.value < totalPages.value);
const hasPreviousPage = computed(() => routePage.value > 1);

const searchArticleItems = computed(() =>
  results.value.map((item) => ({
    articleId: item.id,
    feedId: item.feedId,
    title: item.title ?? '未命名文章',
    summary: item.summary ?? '暂无摘要',
    thumbnail: item.thumbnail,
    feedTitle: item.feedTitle,
    feedAvatar: item.feedAvatar,
    timeAgo: item.timeAgo,
  }))
);

const buildSearchQuery = (overrides?: { page?: number; type?: SearchType }) => {
  const query: Record<string, string> = {};

  if (searchQuery.value) {
    query.q = searchQuery.value;
  }

  const nextType = overrides?.type ?? searchType.value;
  if (nextType !== 'semantic') {
    query.type = nextType;
  }

  const sourceValue = source.value;
  if (sourceValue && sourceValue !== 'owner') {
    query.source = sourceValue;
  }

  const nextPage = overrides?.page ?? routePage.value;
  if (nextPage > 1) {
    query.page = String(nextPage);
  }

  const feedId = props.feedId;
  if (feedId) query.feedId = feedId;

  const tag = props.tags;
  if (tag) query.tags = tag;

  const category = props.category;
  if (category) query.category = category;

  return query;
};

const navigateToPage = (target: number) => {
  if (target < 1 || !hasQuery.value) return;
  router.push({ name: 'search', query: buildSearchQuery({ page: target }) });
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const nextPage = () => {
  if (!hasNextPage.value || searchFetching.value) return;
  navigateToPage(routePage.value + 1);
};

const prevPage = () => {
  if (!hasPreviousPage.value || searchFetching.value) return;
  navigateToPage(Math.max(1, routePage.value - 1));
};

const setSearchType = (type: SearchType) => {
  if (type === searchType.value) return;
  router.push({ name: 'search', query: buildSearchQuery({ page: 1, type }) });
};

const goBackToHome = () => {
  const query: Record<string, string> = {};

  const feedId = props.feedId;
  if (feedId) query.feedId = feedId;

  const tag = props.tags;
  if (tag) query.tags = tag;

  const category = props.category;
  if (category) query.category = category;

  router.push({ name: 'home', query });
};

</script>

<style scoped>
.menu-action { @apply block w-full px-4 py-2 text-left text-sm hover:bg-surface-container; }
</style>
