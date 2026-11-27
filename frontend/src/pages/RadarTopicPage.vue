<template>
  <div class="min-h-screen text-text">
    <div class="mx-auto max-w-screen-lg px-3 py-4 sm:px-4 sm:py-5">
      <!-- 轻量话题头部 -->
      <header class="mb-4 sm:mb-5">
        <div class="flex items-start gap-2">
          <button
              type="button"
              class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-text-secondary transition-colors hover:bg-surface-container/40 hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
              @click="goBack"
              :aria-label="tr('radarTopicPage.back')"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
            </svg>
          </button>

          <div class="min-w-0 flex-1">
            <h1 class="break-words text-lg font-bold leading-snug tracking-tight text-text sm:text-xl">
              {{ topic?.title || tr('radarTopicPage.title') }}
            </h1>
            <p v-if="topicSummary" class="mt-1 break-words text-xs leading-relaxed text-text-secondary line-clamp-2 sm:text-[13px]">
              {{ topicSummary }}
            </p>
            <p v-if="topic" class="mt-0.5 break-words text-[11px] leading-5 text-text-secondary/70">
              <span class="whitespace-nowrap">{{ tr('common.articleCount', { count: topic.articleCount }) }}</span>
              <template v-for="kw in (topic.topKeywords ?? []).slice(0, 3)" :key="kw">
                <span class="mx-1.5" aria-hidden="true">·</span>
                <span>{{ kw }}</span>
              </template>
            </p>
          </div>

          <!-- 刷新按钮 -->
          <button
              type="button"
              @click="refresh"
              :disabled="topicLoading"
              class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-text-secondary transition-colors hover:bg-surface-container/40 hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 disabled:cursor-not-allowed disabled:opacity-40"
              :aria-label="tr('radarTopicPage.refreshTopic')"
              :aria-busy="topicLoading"
          >
            <svg
                class="h-4 w-4 transition-transform"
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

      </header>

      <!-- 错误提示 -->
      <div v-if="topicError" role="alert" class="mb-4 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400">
        {{ topicError }}
      </div>

      <!-- 与编号列表相同结构的骨架屏 -->
      <div v-if="topicLoading" role="status" aria-live="polite">
        <span class="sr-only">{{ tr('common.loadingList') }}</span>
        <div v-for="i in 8" :key="i" class="flex animate-pulse items-start gap-2 py-1.5 sm:gap-2.5" aria-hidden="true">
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

      <!-- 紧凑编号文章列表 -->
      <ol v-else-if="items.length" :start="rankStart" class="list-none">
        <li v-for="(item, index) in items" :key="item.articleId">
          <router-link :to="{ name: 'article', params: { id: item.articleId } }"
                       class="flex items-start gap-2 rounded-sm py-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 sm:gap-2.5">
            <span class="w-8 shrink-0 pt-0.5 text-right font-mono text-xs tabular-nums text-text-secondary/60" aria-hidden="true">{{ rankStart + index }}.</span>
            <div class="min-w-0 flex-1">
              <h2 class="break-words text-sm font-medium leading-5 text-text">
                <span class="transition-colors hover:text-primary">{{ item.title || tr('common.untitledArticle') }}</span>
              </h2>
              <p class="mt-0.5 flex min-w-0 items-center gap-1.5 text-[11px] leading-4 text-text-secondary/70">
                <span class="min-w-0 truncate">{{ item.feedTitle || tr('common.unknownSource') }}</span>
                <span class="shrink-0" aria-hidden="true">·</span>
                <span class="shrink-0 whitespace-nowrap">{{ item.relativeTime || formatRelativeTime(item.publishedAt) }}</span>
              </p>
            </div>
          </router-link>
        </li>
      </ol>

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center py-20 text-center">
        <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/60">
          <svg class="h-8 w-8 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h10" />
          </svg>
        </div>
        <h2 class="text-base font-bold text-text">{{ tr('common.noArticles') }}</h2>
        <p class="mt-1 text-xs text-text-secondary">{{ tr('radarTopicPage.emptyDescription') }}</p>
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
import { translate as tr } from '@/i18n';
import { useDocumentTitle } from '@/composables/documentTitle';
import { computed, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import Pagination from '@/components/Pagination.vue';
import { useRadarStore } from '@/stores/radar';
import { formatRelativeTime } from '@/utils/datetime';

defineOptions({ name: 'RadarTopicPage' });

const router = useRouter();
const route = useRoute();
const radarStore = useRadarStore();
const {
  topic,
  items,
  topicLoading,
  topicError,
  topicPage,
  topicSize,
  topicHasNextPage,
  topicHasPreviousPage
} = storeToRefs(radarStore);
const rankStart = computed(() => (topicPage.value - 1) * topicSize.value + 1);
const topicSummary = computed(() => {
  const description = topic.value?.description?.trim();
  if (!description) return '';
  const normalize = (value: string) => value.replace(/[\s\p{P}\p{S}]/gu, '').toLowerCase();
  const normalized = normalize(description);
  if (!normalized || normalized === normalize(topic.value?.title ?? '')) return '';
  const keywords = new Set((topic.value?.topKeywords ?? []).map(normalize));
  const terms = description.split(/[\s,，、;；|·#]+/u).map(normalize).filter(Boolean);
  if (terms.length && terms.every(term => keywords.has(term))) return '';
  return description;
});
useDocumentTitle(() => topic.value?.title || tr('radarTopicPage.title'));

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
