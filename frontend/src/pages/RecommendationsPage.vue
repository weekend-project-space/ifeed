<template>
  <div class="flex h-full flex-col gap-6 px-4 py-4 sm:px-6">
    <header class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-xl font-semibold text-text">智能推荐</h1>
        <p class="text-sm text-text-secondary">
          聚合协同过滤、语义向量与流行度信号的候选集，实时为你刷新阅读灵感。
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <div class="inline-flex rounded-full border border-outline/40 bg-surface-container p-1">
          <button
            type="button"
            class="rounded-full px-3 py-1.5 text-xs font-medium transition"
            :class="source === 'owner'
              ? 'bg-primary text-primary-foreground shadow'
              : 'text-text-secondary hover:text-text'"
            @click="setSource('owner')"
          >
            为你推荐
          </button>
          <button
            type="button"
            class="rounded-full px-3 py-1.5 text-xs font-medium transition"
            :class="source === 'global'
              ? 'bg-primary text-primary-foreground shadow'
              : 'text-text-secondary hover:text-text'"
            @click="setSource('global')"
          >
            全局热门
          </button>
        </div>
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-full border border-outline/40 bg-surface-container px-3 py-1.5 text-xs text-text-secondary transition hover:border-primary/50 hover:text-primary"
          @click="refresh"
          :disabled="loading"
        >
          <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 10a6 6 0 0 1 10-4.24M16 10a6 6 0 0 1-10 4.24M4 6V3.5M4 3.5h2.5M4 3.5 6.5 6M16 14v2.5M16 16.5h-2.5M16 16.5 13.5 14" />
          </svg>
          <span>{{ loading ? '刷新中…' : '刷新' }}</span>
        </button>
      </div>
    </header>

    <section class="flex-1">
      <div v-if="error" class="rounded-xl border border-outline/30 bg-error/5 p-6 text-sm text-error">
        <p class="font-medium">推荐请求出错：{{ error }}</p>
        <button
          type="button"
          class="mt-3 inline-flex items-center gap-2 rounded-full bg-error text-error-foreground px-4 py-1.5 text-xs font-semibold transition hover:bg-error/90"
          @click="refresh"
        >
          重试一次
        </button>
      </div>
      <div v-else-if="loading" class="space-y-4">
        <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
          <div
            v-for="i in 4"
            :key="`recommend-skeleton-${i}`"
            class="animate-pulse rounded-xl border border-outline/15 bg-surface-container px-5 py-6 shadow-sm shadow-black/0 transition"
          >
            <div class="flex items-center justify-between gap-3">
              <div class="flex flex-1 items-center gap-2">
                <div class="h-6 w-20 rounded-full bg-outline/15" />
                <div class="h-3 w-10 rounded-full bg-outline/10" />
              </div>
              <div class="h-6 w-16 rounded-full bg-outline/15" />
            </div>
            <div class="mt-4 space-y-3">
              <div class="h-4 w-3/4 rounded-full bg-outline/15" />
              <div class="h-4 w-full rounded-full bg-outline/10" />
              <div class="h-4 w-5/6 rounded-full bg-outline/10" />
              <div class="h-4 w-1/2 rounded-full bg-outline/10" />
            </div>
            <div class="mt-6 flex flex-wrap gap-2">
              <div class="h-6 w-16 rounded-full bg-outline/10" />
              <div class="h-6 w-20 rounded-full bg-outline/10" />
              <div class="h-6 w-12 rounded-full bg-outline/10" />
            </div>
          </div>
        </div>
        <div class="flex items-center justify-center gap-2 rounded-xl border border-outline/20 bg-surface py-3 text-xs text-text-muted">
          <svg class="h-4 w-4 animate-spin text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"></circle>
            <path class="opacity-75" d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-linecap="round"></path>
          </svg>
          正在加载最新推荐…
        </div>
      </div>
      <div v-else-if="recommendations.length === 0" class="rounded-xl border border-outline/20 bg-surface-container p-8 text-center text-sm text-text-secondary">
        暂无推荐结果，尝试刷新或多收藏一些感兴趣的文章吧。
      </div>
      <div v-else>
        <ArticleList
          :items="articleItems"
          empty-message="暂无推荐结果，尝试刷新或多收藏一些文章吧。"
          @select="handleSelect"
          @toggle-favorite="handleToggleFavorite"
          @select-tag="handleSelectTag"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { fetchRecommendations, type ArticleRecommendation, type RecommendationSource } from '../api/articles';
import ArticleList from '../components/ArticleList.vue';
import { useCollectionsStore } from '../stores/collections';

const SIZE = 20;

const router = useRouter();
const source = ref<RecommendationSource>('owner');
const recommendations = ref<ArticleRecommendation[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

const reasonMap: Record<ArticleRecommendation['reason'], string> = {
  collaborative: '协同推荐',
  content: '语义相似',
  popular: '热门趋势'
};

const collectionsStore = useCollectionsStore();

const loadRecommendations = async () => {
  loading.value = true;
  error.value = null;
  try {
    recommendations.value = await fetchRecommendations({ size: SIZE, source: source.value });
  } catch (err: any) {
    error.value = err?.message ?? '未知错误';
  } finally {
    loading.value = false;
  }
};

const setSource = (value: RecommendationSource) => {
  if (source.value === value) {
    return;
  }
  source.value = value;
};

const refresh = () => {
  loadRecommendations();
};

const articleItems = computed(() =>
  recommendations.value.map((item) => {
    const baseTags = Array.isArray(item.tags) ? [...item.tags] : [];
    const reason = reasonLabel(item.reason);
    const tags = reason ? [`推荐·${reason}`, ...baseTags] : baseTags;
    return {
      id: item.id,
      title: item.title,
      summary: item.summary || '暂无摘要',
      feedTitle: item.feedTitle || '未知来源',
      timeAgo: item.timeAgo || '',
      tags,
      collected: collectionsStore.isCollected(item.id),
      thumbnail: item.thumbnail,
      enclosure: item.enclosure
    };
  })
);

const handleSelect = (articleId: string) => {
  router.push({ name: 'article-detail', params: { id: articleId } });
};

const handleToggleFavorite = async (articleId: string) => {
  const target = articleItems.value.find((item) => item.id === articleId);
  try {
    await collectionsStore.toggleCollection(articleId, { title: target?.title });
  } catch (err) {
    console.warn('收藏操作失败', err);
  }
};

const handleSelectTag = (tag: string) => {
  if (!tag || tag.startsWith('推荐·')) {
    return;
  }
  router.push({ name: 'home', query: { tags: tag.toLowerCase() } });
};

const reasonLabel = (reason: ArticleRecommendation['reason']) => reasonMap[reason] ?? '推荐';

watch(source, () => {
  loadRecommendations();
});

onMounted(async () => {
  if (!collectionsStore.items.length) {
    try {
      await collectionsStore.fetchCollections();
    } catch (err) {
      console.warn('收藏列表加载失败', err);
    }
  }
  await loadRecommendations();
});
</script>
