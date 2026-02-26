<template>
  <div class="min-h-screen">
    <div class="max-w-screen-lg mx-auto px-3 sm:px-6 py-4 sm:py-6">
      <!-- Page Header -->
      <div class="mb-6 sm:mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold text-text mb-2">热点雷达</h1>
        <p class="text-xs sm:text-sm text-text-secondary">
          聚合最近 {{ windowHoursText }} 小时的热点话题
          <span v-if="generatedAtText" class="ml-1">· 更新于 {{ generatedAtText }}</span>
        </p>
      </div>

      <!-- Controls -->
      <div class="flex items-center justify-between mb-4 sm:mb-6 gap-3">
        <div></div>
<!--        <div class="flex items-center gap-2">
          <button
            type="button"
            class="px-3 py-1.5 text-sm font-medium rounded-lg transition-colors whitespace-nowrap"
            :class="windowHours === 6 ? 'bg-secondary text-secondary-foreground' : 'bg-secondary/5 text-secondary hover:bg-secondary/20'"
            @click="setWindowHours(6)"
          >
            6h
          </button>
          <button
            type="button"
            class="px-3 py-1.5 text-sm font-medium rounded-lg transition-colors whitespace-nowrap"
            :class="windowHours === 12 ? 'bg-secondary text-secondary-foreground' : 'bg-secondary/5 text-secondary hover:bg-secondary/20'"
            @click="setWindowHours(12)"
          >
            12h
          </button>
          <button
            type="button"
            class="px-3 py-1.5 text-sm font-medium rounded-lg transition-colors whitespace-nowrap"
            :class="windowHours === 24 ? 'bg-secondary text-secondary-foreground' : 'bg-secondary/5 text-secondary hover:bg-secondary/20'"
            @click="setWindowHours(24)"
          >
            24h
          </button>
        </div>-->

        <div class="flex items-center gap-2">

          <button
            @click="refresh"
            :disabled="loading"
            class="p-2 hover:bg-surface-container rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label="刷新热点雷达"
          >
            <svg
              class="w-5 h-5 text-text-secondary transition-transform"
              :class="{ 'animate-spin': loading }"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Error -->
      <div v-if="error" class="mb-4 p-3 sm:p-4 bg-red-50 border border-red-200 rounded-lg">
        <div class="flex items-center gap-2 text-xs sm:text-sm text-red-800">
          <svg class="w-4 h-4 sm:w-5 sm:h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span>{{ error }}</span>
        </div>
      </div>

      <!-- List -->
      <div v-if="loading" class="space-y-3 sm:space-y-4" role="status" aria-live="polite">
        <div v-for="i in 6" :key="i" class="p-4 sm:p-5 rounded-xl bg-surface-container animate-pulse">
          <div class="h-4 bg-gray-200/70 dark:bg-gray-700/60 rounded w-2/3 mb-2"></div>
          <div class="h-3 bg-gray-200/70 dark:bg-gray-700/60 rounded w-1/2 mb-3"></div>
          <div class="flex gap-2">
            <div class="h-6 bg-gray-200/70 dark:bg-gray-700/60 rounded w-16"></div>
            <div class="h-6 bg-gray-200/70 dark:bg-gray-700/60 rounded w-12"></div>
            <div class="h-6 bg-gray-200/70 dark:bg-gray-700/60 rounded w-20"></div>
          </div>
        </div>
      </div>

      <div v-else-if="topics.length" class="space-y-3 sm:space-y-4">
        <router-link
          v-for="t in topics"
          :key="t.topicId"
          :to="{ name: 'radarTopic', params: { topicId: t.topicId }, query: { snapshotId } }"
          class="block p-4 sm:p-5 rounded-xl border border-outline/20 hover:bg-surface-container/50 transition-colors"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <h2 class="text-base sm:text-lg font-medium text-text line-clamp-2">
                {{ t.title || '未命名话题' }}
              </h2>
              <p v-if="t.description" class="mt-1 text-xs sm:text-sm text-text-secondary line-clamp-2">
                {{ t.description }}
              </p>

              <div class="mt-3 flex flex-wrap items-center gap-2">
                <span class="text-[11px] sm:text-xs px-2 py-1 rounded-full bg-secondary/10 text-secondary border border-secondary/20">
                  {{ t.articleCount }} 篇
                </span>
                <span
                  v-for="kw in (t.topKeywords ?? []).slice(0, 6)"
                  :key="kw"
                  class="text-[11px] sm:text-xs px-2 py-1 rounded-full bg-surface-container text-text-secondary"
                >
                  {{ kw }}
                </span>
              </div>
            </div>

            <div class="flex-shrink-0 text-text-muted">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>
        </router-link>

        <pagination
          v-if="topics.length"
          :current-page="page"
          :has-previous-page="hasPreviousPage"
          :has-next-page="hasNextPage"
          :disabled="loading"
          @prev-page="prevPage"
          @next-page="nextPage"
        />
      </div>

      <div v-else class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
        <div class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
          <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.364-6.364l-1.414 1.414M7.05 16.95l-1.414 1.414m0-11.314L7.05 7.05m9.9 9.9l1.414 1.414" />
            <circle cx="12" cy="12" r="4" />
          </svg>
        </div>
        <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">暂时没有热点话题</h2>
        <p class="text-xs sm:text-sm text-text-secondary px-4">稍后再试，或调整时间窗口</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import Pagination from '@/components/Pagination.vue';
import { useRadarStore } from '@/stores/radar';

const router = useRouter();
const route = useRoute();

const radarStore = useRadarStore();
const {
  topics,
  loading,
  error,
  page,
  hasNextPage,
  hasPreviousPage,
  snapshotId,
  windowHours,
  generatedAt
} = storeToRefs(radarStore);

const windowHoursText = computed(() => {
  if (windowHours.value && Number.isFinite(windowHours.value)) {
    return String(windowHours.value);
  }
  return '24';
});

const generatedAtText = computed(() => {
  if (!generatedAt.value) return '';
  try {
    const dt = new Date(generatedAt.value);
    if (Number.isNaN(dt.getTime())) return '';
    return dt.toLocaleString();
  } catch {
    return '';
  }
});

const currentPage = computed(() => {
  const raw = Array.isArray(route.query.page) ? route.query.page[0] : route.query.page;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
});

const currentWindowHours = computed(() => {
  const raw = Array.isArray(route.query.windowHours)
    ? route.query.windowHours[0]
    : route.query.windowHours;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 24;
});


const buildQuery = (overrides?: { page?: number; windowHours?: number }) => {
  const query: Record<string, string> = {};
  const nextPage = overrides?.page ?? currentPage.value;
  const nextWindow = overrides?.windowHours ?? currentWindowHours.value;

  if (nextPage > 1) query.page = String(nextPage);
  if (nextWindow !== 24) query.windowHours = String(nextWindow);

  return query;
};

const loadData = async () => {
  await radarStore.fetchDigest({
    page: currentPage.value,
    size: 12,
    windowHours: currentWindowHours.value,
  });
};

const refresh = () => loadData();

const navigateToPage = (target: number) => {
  if (target < 1) return;
  router.push({ name: 'radar', query: buildQuery({ page: target }) });
};

const nextPage = () => {
  if (hasNextPage.value) navigateToPage(currentPage.value + 1);
};

const prevPage = () => {
  if (hasPreviousPage.value) navigateToPage(Math.max(1, currentPage.value - 1));
};

const setWindowHours = (hours: number) => {
  router.push({ name: 'radar', query: buildQuery({ page: 1, windowHours: hours }) });
};

watch(
  () => [route.query.page, route.query.windowHours],
  async () => {
    await loadData();
  }
);

onMounted(async () => {
  await loadData();
});
</script>
