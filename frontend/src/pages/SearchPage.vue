<template>
  <div class="text-text">
    <div class="mx-auto max-w-screen-lg px-4 py-4 sm:px-6 sm:py-6">
      <!-- 搜索头部 -->
      <div class="mb-6 sm:mb-8">
        <h1 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">
          {{ hasQuery ? t('searchPage.results') : t('searchPage.search') }}
        </h1>
        <p v-if="hasQuery" class="mt-1 text-xs text-text-secondary sm:text-sm">
          {{ t('searchPage.keyword', { query: searchQuery }) }}
          <span v-if="totalElements !== null" class="ml-2">{{ t('searchPage.resultCount', { count: totalElements }) }}</span>
        </p>
      </div>

      <!-- 控制栏：MD3 Filter Chips 与返回胶囊 -->
      <div class="mb-6 flex flex-wrap items-center justify-between gap-3 border-b border-outline/10 pb-4">
        <!-- 搜索类型切换：MD3 Filter Chips 规范 -->
        <div class="flex items-center gap-1.5 rounded-full bg-surface-container/50 p-1">
          <button type="button"
                  class="rounded-full px-4 py-1.5 text-xs font-medium transition select-none sm:text-xs"
                  :class="searchType === 'semantic'
                    ? 'bg-primary/15 font-semibold text-primary shadow-xs'
                    : 'text-text-secondary hover:bg-surface-container hover:text-text'"
                  @click="setSearchType('semantic')">
            {{ t('searchPage.semantic') }}
          </button>
          <button type="button"
                  class="rounded-full px-4 py-1.5 text-xs font-medium transition select-none sm:text-xs"
                  :class="searchType === 'keyword'
                    ? 'bg-primary/15 font-semibold text-primary shadow-xs'
                    : 'text-text-secondary hover:bg-surface-container hover:text-text'"
                  @click="setSearchType('keyword')">
            {{ t('searchPage.keywordMatch') }}
          </button>
        </div>

        <!-- 返回主页按钮：MD3 Tonal 药丸 -->
        <button
            class="inline-flex h-8 items-center gap-1.5 rounded-full bg-surface-container/60 px-4 text-xs font-medium text-text transition hover:bg-surface-container active:scale-[0.98]"
            @click="goBackToHome">
          <svg class="h-3.5 w-3.5 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          <span>{{ t('searchPage.backHome') }}</span>
        </button>
      </div>

      <!-- 空白引导态 -->
      <div v-if="!hasQuery" class="flex flex-col items-center justify-center py-20 text-center">
        <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/60">
          <svg class="h-8 w-8 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="11" cy="11" r="8" />
            <path stroke-linecap="round" d="m21 21-4.35-4.35" />
          </svg>
        </div>
        <h2 class="text-base font-bold text-text">{{ t('searchPage.start') }}</h2>
        <p class="mt-1 text-xs text-text-secondary">{{ t('searchPage.prompt') }}</p>
      </div>

      <!-- 错误横幅 -->
      <div v-else-if="searchError" role="alert" class="rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400">
        <div class="flex items-center justify-between gap-3">
          <span>{{ searchError }}</span>
          <button type="button" class="shrink-0 font-semibold underline" :disabled="searchFetching" @click="searchQueryResult.refetch()">{{ t('searchPage.retry') }}</button>
        </div>
      </div>

      <!-- 搜索结果列表 -->
      <div v-else-if="searchLoading || searchArticleItems.length > 0">
        <article-card-list :loading="searchLoading" :items="searchArticleItems" meta-field="timeAgo"
                           show-action :action-label="tr('common.articleOptions')">
          <template #empty-thumbnail>
            <svg class="h-8 w-8 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
            </svg>
          </template>
          <template #action-dropdown="{ item, close }">
            <router-link :to="`/articles/${item.articleId}`" target="_blank"
                         class="block w-full px-4 py-2 text-left text-xs font-medium text-text transition hover:bg-surface-container"
                         @click="close">{{ t('searchPage.openNewTab') }}</router-link>
          </template>
        </article-card-list>

        <!-- 分页 -->
        <pagination v-if="searchArticleItems.length > 0 && !searchLoading" :current-page="routePage"
                    :has-previous-page="hasPreviousPage" :has-next-page="hasNextPage" :disabled="searchFetching"
                    @prev-page="prevPage" @next-page="nextPage" />
      </div>

      <!-- 搜索无结果 -->
      <div v-else class="flex flex-col items-center justify-center py-20 text-center">
        <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/60">
          <svg class="h-8 w-8 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <h2 class="text-base font-bold text-text">{{ t('searchPage.notFound') }}</h2>
        <p class="mt-1 text-xs text-text-secondary">{{ t('searchPage.tryAgain') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useDocumentTitle } from '@/composables/documentTitle';
import { useRouter } from 'vue-router';
import { useSearchQuery, type SearchType, type Source } from '../queries/search';

defineOptions({ name: 'SearchPage' });
const { t } = useI18n();

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
useDocumentTitle(() => t(hasQuery.value ? 'searchPage.results' : 'searchPage.search'));

const searchQueryResult = useSearchQuery(searchQuery, searchType, source, routePage, filters);
const searchLoading = computed(() => searchQueryResult.isPending.value);
const searchFetching = computed(() => searchQueryResult.isFetching.value);
const searchError = computed(() => {
  const error = searchQueryResult.error.value;
  return error instanceof Error ? error.message : error ? t('searchPage.failed') : null;
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
      title: item.title ?? tr('common.untitledArticle'),
      summary: item.summary ?? tr('common.noSummary'),
      thumbnail: item.thumbnail,
      feedTitle: item.feedTitle,
      feedAvatar: item.feedAvatar,
      timeAgo: item.timeAgo,
    }))
);

const buildSearchQuery = (overrides?: { page?: number; type?: SearchType }) => {
  const query: Record<string, string> = {};
  if (searchQuery.value) query.q = searchQuery.value;
  const nextType = overrides?.type ?? searchType.value;
  if (nextType !== 'semantic') query.type = nextType;
  const sourceValue = source.value;
  if (sourceValue && sourceValue !== 'owner') query.source = sourceValue;
  const nextPage = overrides?.page ?? routePage.value;
  if (nextPage > 1) query.page = String(nextPage);
  if (props.feedId) query.feedId = props.feedId;
  if (props.tags) query.tags = props.tags;
  if (props.category) query.category = props.category;
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
  if (props.feedId) query.feedId = props.feedId;
  if (props.tags) query.tags = props.tags;
  if (props.category) query.category = props.category;
  router.push({ name: 'home', query });
};
</script>
