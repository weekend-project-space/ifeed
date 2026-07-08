<template>
  <div class="min-h-screen">
    <div class="max-w-screen-lg mx-auto px-3 sm:px-6 py-4 sm:py-6">
      <!-- Header -->
      <div class="mb-5 sm:mb-7">
        <div class="flex items-center gap-2 mb-2">
          <button
            type="button"
            class="p-2 -ml-2 hover:bg-surface-container rounded-full transition-colors"
            @click="goBack"
            aria-label="返回"
          >
            <svg class="w-5 h-5 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 18l-6-6 6-6" />
            </svg>
          </button>

          <div class="min-w-0">
            <h1 class="text-xl sm:text-2xl font-bold text-text line-clamp-2">
              {{ topic?.title || '话题详情' }}
            </h1>
            <p v-if="topic?.description" class="text-xs sm:text-sm text-text-secondary mt-1 line-clamp-2">
              {{ topic.description }}
            </p>
          </div>
        </div>

        <div class="flex items-center justify-between gap-3">
          <div class="text-xs sm:text-sm text-text-secondary">
            <span v-if="topic">{{ topic.articleCount }} 篇文章</span>
            <span v-if="snapshotId" class="ml-2">· snapshot: {{ snapshotId }}</span>
          </div>

          <button
            @click="refresh"
            :disabled="topicLoading"
            class="p-2 hover:bg-surface-container rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label="刷新话题"
          >
            <svg
              class="w-5 h-5 text-text-secondary transition-transform"
              :class="{ 'animate-spin': topicLoading }"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
            </svg>
          </button>
        </div>

        <div v-if="(topic?.topKeywords ?? []).length" class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="kw in (topic?.topKeywords ?? []).slice(0, 10)"
            :key="kw"
            class="text-[11px] sm:text-xs px-2 py-1 rounded-full bg-surface-container text-text-secondary"
          >
            {{ kw }}
          </span>
        </div>
      </div>

      <!-- Error -->
      <div v-if="topicError" class="mb-4 p-3 sm:p-4 bg-red-50 border border-red-200 rounded-lg">
        <div class="flex items-center gap-2 text-xs sm:text-sm text-red-800">
          <svg class="w-4 h-4 sm:w-5 sm:h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span>{{ topicError }}</span>
        </div>
      </div>

      <article-card-list
        v-if="topicLoading || items.length"
        :loading="topicLoading"
        :items="items"
        meta-field="relativeTime"
        :show-action="false"
      />

      <div v-else class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
        <div class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
          <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M4 6h16M4 12h16M4 18h10" />
          </svg>
        </div>
        <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">暂无文章</h2>
        <p class="text-xs sm:text-sm text-text-secondary px-4">这个话题当前没有可展示的文章</p>
      </div>

      <pagination
        v-if="items.length && !topicLoading"
        :current-page="topicPage"
        :has-previous-page="topicHasPreviousPage"
        :has-next-page="topicHasNextPage"
        :disabled="topicLoading"
        @prev-page="prevPage"
        @next-page="nextPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import ArticleCardList from '@/components/ArticleCardList.vue';
import Pagination from '@/components/Pagination.vue';
import { useRadarStore } from '@/stores/radar';

const router = useRouter();
const route = useRoute();

const radarStore = useRadarStore();
const {
  topic,
  items,
  topicLoading,
  topicError,
  topicPage,
  topicHasNextPage,
  topicHasPreviousPage
} = storeToRefs(radarStore);

const topicId = computed(() => {
  const raw = route.params.topicId;
  return typeof raw === 'string' ? raw : '';
});

const snapshotId = computed(() => {
  const raw = Array.isArray(route.query.snapshotId) ? route.query.snapshotId[0] : route.query.snapshotId;
  return typeof raw === 'string' ? raw : '';
});

const currentPage = computed(() => {
  const raw = Array.isArray(route.query.page) ? route.query.page[0] : route.query.page;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
});

const buildQuery = (overrides?: { page?: number }) => {
  const query: Record<string, string> = {};
  const nextPage = overrides?.page ?? currentPage.value;
  if (snapshotId.value) query.snapshotId = snapshotId.value;
  if (nextPage > 1) query.page = String(nextPage);
  return query;
};

const loadData = async () => {
  if (!topicId.value || !snapshotId.value) {
    await router.replace({ name: 'radar' });
    return;
  }
  await radarStore.fetchTopicDetail({
    topicId: topicId.value,
    snapshotId: snapshotId.value,
    page: currentPage.value,
    size: 20
  });
};

const refresh = () => loadData();

const navigateToPage = (target: number) => {
  if (target < 1) return;
  router.push({ name: 'radarTopic', params: { topicId: topicId.value }, query: buildQuery({ page: target }) });
};

const nextPage = () => {
  if (topicHasNextPage.value) navigateToPage(currentPage.value + 1);
};

const prevPage = () => {
  if (topicHasPreviousPage.value) navigateToPage(Math.max(1, currentPage.value - 1));
};

const goBack = () => {
  router.back();
};

watch(
  () => [route.params.topicId, route.query.snapshotId, route.query.page],
  async () => {
    await loadData();
  }
);

onMounted(async () => {
  await loadData();
});
</script>
