<template>
  <div class="min-h-screen text-text">
    <div class="mx-auto max-w-screen-lg px-3 py-4 sm:px-4 sm:py-5">

      <!-- Header -->
      <div class="mb-4 flex items-start justify-between gap-3 sm:mb-5">
        <div class="min-w-0">
          <h1 class="text-lg font-bold tracking-tight text-text sm:text-xl">{{ tr('radarPage.title') }}</h1>
          <p class="mt-0.5 flex flex-wrap items-baseline gap-x-1.5 text-[11px] leading-5 text-text-secondary">
            <span>{{ tr('radarPage.timeWindow', { hours: windowHoursText }) }}</span>
            <template v-if="generatedAtText">
              <span aria-hidden="true">·</span>
              <span>{{ generatedAtText }}</span>
            </template>
          </p>
        </div>

        <!-- 轻量刷新按钮 -->
        <button
            type="button"
            @click="refresh"
            :disabled="loading"
            class="inline-flex h-8 shrink-0 items-center gap-1.5 rounded-md px-2 text-xs text-text-secondary transition-colors hover:bg-surface-container/40 hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 disabled:cursor-not-allowed disabled:opacity-40"
            :aria-label="tr('common.refresh')"
            :aria-busy="loading"
        >
          <svg
              class="h-3.5 w-3.5 transition-transform"
              :class="{ 'animate-spin': loading }"
              viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          >
            <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
          </svg>
          <span>{{ tr('common.refresh') }}</span>
        </button>
      </div>

      <!-- 错误横幅 -->
      <div v-if="error" class="mb-6 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400">
        {{ error }}
      </div>

      <!-- 骨架屏：与紧凑编号榜保持相同结构 -->
      <div v-if="loading" aria-hidden="true">
        <div
            v-for="i in 8"
            :key="i"
            class="flex animate-pulse items-start gap-2 py-1.5 sm:gap-2.5"
        >
          <div class="w-8 shrink-0 pt-1">
            <div class="ml-auto h-3 w-4 rounded bg-surface-container/60" />
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex h-5 items-center">
              <div class="h-3.5 w-3/5 rounded bg-surface-container" />
            </div>
            <div class="mt-0.5 flex h-4 items-center">
              <div class="h-2.5 w-2/5 rounded bg-surface-container/60" />
            </div>
          </div>
        </div>
      </div>

      <!-- 话题列表：紧凑编号、标题与次行元信息 -->
      <div v-else-if="topics.length">
        <ol :start="rankStart" class="list-none">
          <li v-for="(t, index) in topics" :key="t.topicId">
            <router-link
                :to="{ name: 'radarTopic', params: { topicId: t.topicId }, query: { snapshotId } }"
                class="flex items-start gap-2 rounded-sm py-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 sm:gap-2.5"
            >
              <span class="w-8 shrink-0 pt-0.5 text-right font-mono text-xs tabular-nums text-text-secondary/60" aria-hidden="true">
                {{ rankStart + index }}.
              </span>

              <div class="min-w-0 flex-1">
                <h2 class="break-words text-sm font-medium leading-5 text-text">
                  <span class="transition-colors hover:text-primary">{{ t.title || tr('radarPage.untitledTopic') }}</span>
                </h2>
                <p class="mt-0.5 break-words text-[11px] leading-4 text-text-secondary/70">
                  <span class="whitespace-nowrap">{{ tr('common.articleCount', { count: t.articleCount }) }}</span>
                  <template v-for="kw in (t.topKeywords ?? []).slice(0, 3)" :key="kw">
                    <span class="mx-1.5" aria-hidden="true">·</span>
                    <span>{{ kw }}</span>
                  </template>
                </p>
              </div>
            </router-link>
          </li>
        </ol>

        <!-- 分页 -->
        <pagination
              v-if="hasPreviousPage || hasNextPage"
              :current-page="page"
              :has-previous-page="hasPreviousPage"
              :has-next-page="hasNextPage"
              :disabled="loading"
              @prev-page="prevPage"
              @next-page="nextPage"
        />
      </div>

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center py-20 text-center">
        <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/60">
          <svg class="h-8 w-8 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.364-6.364l-1.414 1.414M7.05 16.95l-1.414 1.414m0-11.314L7.05 7.05m9.9 9.9l1.414 1.414" />
            <circle cx="12" cy="12" r="4" />
          </svg>
        </div>
        <p class="text-base font-bold text-text">{{ tr('radarPage.emptyTitle') }}</p>
        <p class="mt-1 text-xs text-text-secondary">{{ tr('radarPage.emptyDescription') }}</p>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { i18n, translate as tr } from '@/i18n';
import { useDocumentTitle } from '@/composables/documentTitle';
import { computed, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import Pagination from '@/components/Pagination.vue';
import { useRadarStore } from '@/stores/radar';

defineOptions({ name: 'RadarPage' });
useDocumentTitle(() => tr('radarPage.title'));

const router = useRouter();
const route = useRoute();
const radarStore = useRadarStore();
const {
  topics,
  loading,
  error,
  page,
  size,
  hasNextPage,
  hasPreviousPage,
  snapshotId,
  windowHours,
  generatedAt
} = storeToRefs(radarStore);

const rankStart = computed(() => (page.value - 1) * size.value + 1);

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
    return dt.toLocaleString(i18n.global.locale.value);
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
