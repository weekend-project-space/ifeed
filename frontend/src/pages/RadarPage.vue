<template>
  <div class="min-h-screen text-text">
    <div class="mx-auto max-w-screen-lg px-4 py-5 sm:px-6 sm:py-8">

      <!-- Header -->
      <div class="mb-6 flex items-start justify-between sm:mb-8">
        <div>
          <h1 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">今日热榜</h1>
          <p class="mt-1 text-xs text-text-secondary sm:text-sm">
            过去 {{ windowHoursText }}h
            <span v-if="generatedAtText" class="before:mx-1.5 before:content-['·']">{{ generatedAtText }}</span>
          </p>
        </div>

        <!-- MD3 Tonal 刷新药丸 -->
        <button
            @click="refresh"
            :disabled="loading"
            class="inline-flex h-9 items-center gap-1.5 rounded-full bg-surface-container/60 px-4 text-xs font-semibold text-text shadow-xs transition hover:bg-surface-container active:scale-[0.98] disabled:opacity-40"
            aria-label="刷新"
        >
          <svg
              class="h-3.5 w-3.5 text-text-secondary transition-transform"
              :class="{ 'animate-spin': loading }"
              viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          >
            <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
          </svg>
          <span>刷新</span>
        </button>
      </div>

      <!-- 错误横幅 -->
      <div v-if="error" class="mb-6 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400">
        {{ error }}
      </div>

      <!-- 骨架屏：M3 表面容器脉冲 -->
      <div v-if="loading" class="space-y-2">
        <div
            v-for="i in 8"
            :key="i"
            class="flex items-start gap-4 rounded-3xl bg-surface-container/25 p-4 animate-pulse"
        >
          <div class="mt-0.5 h-8 w-8 shrink-0 rounded-2xl bg-surface-container" />
          <div class="flex-1 space-y-2.5">
            <div class="h-4 w-3/5 rounded-full bg-surface-container" />
            <div class="h-3 w-4/5 rounded-full bg-surface-container/60" />
            <div class="mt-1 flex gap-1.5">
              <div class="h-5 w-12 rounded-full bg-surface-container/80" />
              <div class="h-5 w-16 rounded-full bg-surface-container/50" />
            </div>
          </div>
        </div>
      </div>

      <!-- 话题列表：MD3 柔和表面卡片 -->
      <div v-else-if="topics.length" class="space-y-2.5">
        <router-link
            v-for="(t, index) in topics"
            :key="t.topicId"
            :to="{ name: 'radarTopic', params: { topicId: t.topicId }, query: { snapshotId } }"
            class="group flex items-start gap-4 rounded-3xl bg-surface-container/25 p-4 transition hover:bg-surface-container/50 hover:shadow-xs"
        >
          <!-- 排名标识牌：M3 Tonal 柔和数字徽标 -->
          <div class="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-xs font-bold text-primary shadow-2xs">
            {{ index + 1 }}
          </div>

          <!-- 内容主体 -->
          <div class="min-w-0 flex-1">
            <div class="flex items-start justify-between gap-3">
              <h2 class="text-sm font-semibold leading-snug text-text line-clamp-2 transition group-hover:text-primary sm:text-base">
                {{ t.title || '未命名话题' }}
              </h2>
              <svg
                  class="mt-1 h-4 w-4 shrink-0 text-text-secondary opacity-0 transition-opacity group-hover:opacity-100"
                  viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </div>

            <p v-if="t.description" class="mt-1 text-xs leading-relaxed text-text-secondary line-clamp-2">
              {{ t.description }}
            </p>

            <!-- 篇数与标签指示条 -->
            <div class="mt-3 flex flex-wrap items-center gap-1.5">
              <span class="inline-flex items-center rounded-full bg-primary/15 px-2.5 py-0.5 text-[11px] font-semibold text-primary">
                {{ t.articleCount }} 篇
              </span>
              <span
                  v-for="kw in (t.topKeywords ?? []).slice(0, 5)"
                  :key="kw"
                  class="rounded-full bg-surface px-2.5 py-0.5 text-[11px] font-medium text-text-secondary shadow-2xs"
              >
                #{{ kw }}
              </span>
            </div>
          </div>
        </router-link>

        <!-- 分页 -->
        <div v-if="hasPreviousPage || hasNextPage" class="pt-4">
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

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center py-20 text-center">
        <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/60">
          <svg class="h-8 w-8 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.364-6.364l-1.414 1.414M7.05 16.95l-1.414 1.414m0-11.314L7.05 7.05m9.9 9.9l1.414 1.414" />
            <circle cx="12" cy="12" r="4" />
          </svg>
        </div>
        <p class="text-base font-bold text-text">暂无热点话题</p>
        <p class="mt-1 text-xs text-text-secondary">稍后再试，或调整时间窗口</p>
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

defineOptions({ name: 'RadarPage' });

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