<template>
  <div class="min-h-screen text-text">
    <div class="mx-auto max-w-screen-lg px-4 py-5 sm:px-6 sm:py-7">
      <!-- 头部：MD3 动态表面包裹 -->
      <div class="mb-6 rounded-[28px] bg-surface-container/40 p-5 sm:p-6">
        <div class="mb-3 flex items-start gap-2.5">
          <button
              type="button"
              class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface hover:text-text active:scale-95"
              @click="goBack"
              aria-label="返回"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
            </svg>
          </button>

          <div class="min-w-0 flex-1">
            <h1 class="text-xl font-bold tracking-tight text-text line-clamp-2 sm:text-2xl">
              {{ topic?.title || '话题详情' }}
            </h1>
            <p v-if="topic?.description" class="mt-1 text-xs leading-relaxed text-text-secondary line-clamp-2 sm:text-sm">
              {{ topic.description }}
            </p>
          </div>

          <!-- 刷新按钮 -->
          <button
              @click="refresh"
              :disabled="topicLoading"
              class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface hover:text-text disabled:cursor-not-allowed disabled:opacity-40"
              aria-label="刷新话题"
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

        <div class="flex items-center justify-between border-t border-outline/10 pt-3 text-xs text-text-secondary">
          <span v-if="topic">{{ topic.articleCount }} 篇文章</span>
          <span v-if="snapshotId" class="opacity-75">快照: {{ snapshotId }}</span>
        </div>

        <!-- 关键词胶囊群 -->
        <div v-if="(topic?.topKeywords ?? []).length" class="mt-3.5 flex flex-wrap gap-1.5">
          <span
              v-for="kw in (topic?.topKeywords ?? []).slice(0, 10)"
              :key="kw"
              class="rounded-full bg-surface px-3 py-1 text-[11px] font-medium text-text-secondary shadow-2xs"
          >
            #{{ kw }}
          </span>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="topicError" class="mb-4 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400">
        {{ topicError }}
      </div>

      <!-- 列表与分页 -->
      <article-card-list
          v-if="topicLoading || items.length"
          :loading="topicLoading"
          :items="items"
          meta-field="relativeTime"
          :show-action="false"
      />

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center py-20 text-center">
        <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/60">
          <svg class="h-8 w-8 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h10" />
          </svg>
        </div>
        <h2 class="text-base font-bold text-text">暂无文章</h2>
        <p class="mt-1 text-xs text-text-secondary">该话题当前没有可展示的文章</p>
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