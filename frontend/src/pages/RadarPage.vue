<template>
  <div class="min-h-screen">
    <div class="max-w-screen-lg mx-auto px-3 sm:px-6 py-4 sm:py-6">

      <!-- Header -->
      <div class="flex items-start justify-between mb-6 sm:mb-8">
        <div>
          <h1 class="text-2xl sm:text-3xl font-bold text-text">热点雷达</h1>
          <p class="text-sm text-text-secondary mt-3">
            过去 {{ windowHoursText }}h
            <span v-if="generatedAtText" class="before:content-['·'] before:mx-1.5">{{ generatedAtText }}</span>
          </p>
        </div>

        <button
          @click="refresh"
          :disabled="loading"
          class="flex items-center gap-2 h-9 px-4 rounded-full text-sm font-medium bg-surface-container text-text-secondary hover:bg-surface-container/70 disabled:opacity-40 transition-colors"
          aria-label="刷新"
        >
          <svg
            class="w-4 h-4 transition-transform"
            :class="{ 'animate-spin': loading }"
            viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          >
            <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
          </svg>
          刷新
        </button>
      </div>

      <!-- Error -->
      <div v-if="error" class="flex items-center gap-3 mb-6 p-4 rounded-2xl bg-red-50 dark:bg-red-950/30 text-red-700 dark:text-red-400 text-sm">
        <svg class="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        {{ error }}
      </div>

      <!-- Loading Skeleton -->
      <div v-if="loading" class="space-y-1">
        <div
          v-for="i in 8"
          :key="i"
          class="flex items-start gap-4 px-4 py-4 rounded-2xl animate-pulse"
        >
          <div class="w-8 h-8 rounded-xl bg-gray-200/80 dark:bg-gray-700/60 flex-shrink-0 mt-0.5" />
          <div class="flex-1 space-y-2.5">
            <div class="h-4 bg-gray-200/80 dark:bg-gray-700/60 rounded-full w-3/5" />
            <div class="h-3 bg-gray-200/60 dark:bg-gray-700/40 rounded-full w-4/5" />
            <div class="flex gap-1.5 mt-1">
              <div class="h-5 w-12 bg-gray-200/80 dark:bg-gray-700/60 rounded-full" />
              <div class="h-5 w-16 bg-gray-200/60 dark:bg-gray-700/40 rounded-full" />
              <div class="h-5 w-14 bg-gray-200/60 dark:bg-gray-700/40 rounded-full" />
            </div>
          </div>
        </div>
      </div>

      <!-- Topic List -->
      <div v-else-if="topics.length" class="space-y-1">
        <router-link
          v-for="(t, index) in topics"
          :key="t.topicId"
          :to="{ name: 'radarTopic', params: { topicId: t.topicId }, query: { snapshotId } }"
          class="flex items-start gap-4 px-4 py-4 rounded-2xl hover:bg-surface-container/60 transition-colors group"
        >
          <!-- Rank badge -->
          <div class="flex-shrink-0 w-8 h-8 mt-0.5 rounded-xl bg-primary/8 flex items-center justify-center">
            <span class="text-xs font-bold text-primary/70">{{ index + 1 }}</span>
          </div>

          <!-- Content -->
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-3">
              <h2 class="text-[15px] font-semibold text-text leading-snug line-clamp-2 group-hover:text-primary transition-colors">
                {{ t.title || '未命名话题' }}
              </h2>
              <svg
                class="w-4 h-4 text-text-muted flex-shrink-0 mt-0.5 opacity-0 group-hover:opacity-100 transition-opacity"
                viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              >
                <path d="M9 5l7 7-7 7" />
              </svg>
            </div>

            <p v-if="t.description" class="mt-1 text-sm text-text-secondary line-clamp-2 leading-relaxed">
              {{ t.description }}
            </p>

            <div class="mt-2.5 flex flex-wrap items-center gap-1.5">
              <span class="inline-flex items-center text-xs font-semibold px-2 py-0.5 rounded-full bg-primary/10 text-primary">
                {{ t.articleCount }} 篇
              </span>
              <span
                v-for="kw in (t.topKeywords ?? []).slice(0, 5)"
                :key="kw"
                class="text-xs px-2 py-0.5 rounded-full bg-surface-container text-text-secondary"
              >
                {{ kw }}
              </span>
            </div>
          </div>
        </router-link>

        <div v-if="hasPreviousPage || hasNextPage" class="pt-3">
          <pagination
            :current-page="page"
            :has-previous-page="hasPreviousPage"
            :has-next-page="hasNextPage"
            :disabled="loading"
            @prev-page="prevPage"
            @next-page="nextPage"
          />
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="flex flex-col items-center justify-center py-20 text-center">
        <div class="w-16 h-16 mb-5 rounded-2xl bg-surface-container flex items-center justify-center">
          <svg class="w-8 h-8 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.364-6.364l-1.414 1.414M7.05 16.95l-1.414 1.414m0-11.314L7.05 7.05m9.9 9.9l1.414 1.414" />
            <circle cx="12" cy="12" r="4" />
          </svg>
        </div>
        <p class="text-base font-semibold text-text mb-1">暂无热点话题</p>
        <p class="text-sm text-text-secondary">稍后再试，或调整时间窗口</p>
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

const currentSnapshotId = computed(() => {
  const raw = Array.isArray(route.query.snapshotId) ? route.query.snapshotId[0] : route.query.snapshotId;
  return typeof raw === 'string' ? raw : '';
});

const currentWindowHours = computed(() => {
  const raw = Array.isArray(route.query.windowHours)
    ? route.query.windowHours[0]
    : route.query.windowHours;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 24;
});

const buildQuery = (overrides?: { page?: number; windowHours?: number; snapshotId?: string | null }) => {
  const query: Record<string, string> = {};
  const nextPage = overrides?.page ?? currentPage.value;
  const nextWindow = overrides?.windowHours ?? currentWindowHours.value;

  if (nextPage > 1) query.page = String(nextPage);
  if (nextWindow !== 24) query.windowHours = String(nextWindow);
  // 允许调用方显式传 null 来清除 snapshotId（如切换时间窗口、刷新时）
  const sid = overrides && 'snapshotId' in overrides ? overrides.snapshotId : snapshotId.value;
  if (sid) query.snapshotId = sid;

  return query;
};

const loadData = async () => {
  await radarStore.fetchDigest({
    page: currentPage.value,
    size: 12,
    snapshotId: currentSnapshotId.value || null,
    windowHours: currentWindowHours.value,
  });
};

const refresh = async () => {
  // 刷新时清除 snapshotId，始终获取最新快照，而不是重新加载旧快照
  await radarStore.fetchDigest({
    page: 1,
    size: 12,
    snapshotId: null,
    windowHours: currentWindowHours.value,
  });
};

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
  // 切换时间窗口时清除旧 snapshotId，后端根据新 windowHours 返回最新快照
  router.push({ name: 'radar', query: buildQuery({ page: 1, windowHours: hours, snapshotId: null }) });
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
