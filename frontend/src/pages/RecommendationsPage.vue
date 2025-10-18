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
      <div v-else-if="loading" class="grid gap-4 lg:grid-cols-2">
        <div v-for="i in 6" :key="i" class="animate-pulse rounded-2xl border border-outline/20 bg-surface-container p-5">
          <div class="flex items-center justify-between">
            <div class="h-5 w-32 rounded-full bg-outline/20" />
            <div class="h-5 w-16 rounded-full bg-outline/20" />
          </div>
          <div class="mt-4 h-4 w-3/4 rounded-full bg-outline/20" />
          <div class="mt-2 h-4 w-2/3 rounded-full bg-outline/10" />
          <div class="mt-5 flex gap-2">
            <div class="h-6 w-16 rounded-full bg-outline/15" />
            <div class="h-6 w-20 rounded-full bg-outline/15" />
          </div>
        </div>
      </div>
      <div v-else-if="recommendations.length === 0" class="rounded-xl border border-outline/20 bg-surface-container p-8 text-center text-sm text-text-secondary">
        暂无推荐结果，尝试刷新或多收藏一些感兴趣的文章吧。
      </div>
      <div v-else class="grid gap-4 lg:grid-cols-2">
        <article
          v-for="item in recommendations"
          :key="item.id"
          class="group flex h-full cursor-pointer flex-col justify-between rounded-2xl border border-outline/20 bg-surface-container p-5 transition hover:border-primary/40 hover:shadow-sm"
          @click="openArticle(item.id)"
        >
          <header class="flex items-start justify-between gap-3">
            <div class="flex flex-1 flex-col gap-1">
              <div class="flex items-center gap-2 text-xs text-text-secondary">
                <span class="rounded-full bg-primary/10 px-2 py-0.5 font-semibold text-primary">
                  {{ reasonLabel(item.reason) }}
                </span>
                <span class="text-text-muted">·</span>
                <span>{{ item.feedTitle }}</span>
                <span v-if="item.timeAgo" class="text-text-muted">· {{ item.timeAgo }}</span>
              </div>
              <h3 class="text-base font-semibold leading-snug text-text transition group-hover:text-primary">
                {{ item.title }}
              </h3>
            </div>
            <button
              type="button"
              class="inline-flex shrink-0 items-center gap-1 rounded-full border border-outline/30 bg-surface px-2.5 py-1 text-[11px] text-text-secondary transition hover:border-outline/50 hover:text-text"
              @click.stop="viewOriginal(item.link)"
            >
              原文
              <svg class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 5H15v7.5m-9.5 2L15 5" />
              </svg>
            </button>
          </header>
          <p
            class="mt-3 text-sm leading-relaxed text-text-secondary"
            style="display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 3; overflow: hidden;"
          >
            {{ item.summary || '暂无摘要' }}
          </p>
          <footer class="mt-5 flex flex-wrap gap-2">
            <span
              v-for="tag in item.tags"
              :key="tag"
              class="rounded-full border border-outline/20 bg-surface px-3 py-1 text-xs font-medium text-text-secondary transition"
            >
              #{{ tag }}
            </span>
          </footer>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { fetchRecommendations, type ArticleRecommendation, type RecommendationSource } from '../api/articles';

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

const openArticle = (articleId: string) => {
  router.push({ name: 'article-detail', params: { id: articleId } });
};

const viewOriginal = (link?: string) => {
  if (!link) {
    return;
  }
  window.open(link, '_blank', 'noopener');
};

const reasonLabel = (reason: ArticleRecommendation['reason']) => reasonMap[reason] ?? '推荐';

watch(source, () => {
  loadRecommendations();
});

onMounted(() => {
  loadRecommendations();
});
</script>
