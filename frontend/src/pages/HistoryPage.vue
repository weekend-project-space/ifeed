<template>
  <div class="space-y-5 sm:space-y-6">
    <section class="rounded-2xl border border-outline/30 bg-surface-container">
      <header class="flex flex-wrap items-center justify-between gap-3 border-b border-outline/30 px-4 py-3 sm:px-6 sm:py-4">
        <div>
          <h2 class="text-lg font-semibold text-text">阅读历史</h2>
          <p class="text-sm text-text-secondary">回顾你最近阅读过的文章。</p>
        </div>
        <button class="text-sm font-medium text-text-muted transition hover:text-text" @click="refresh">刷新</button>
      </header>

      <div v-if="historyStore.loading" class="py-9 text-center text-text-muted sm:py-12">加载中...</div>

      <ul v-else class="divide-y divide-outline/20">
        <li
          v-for="entry in items"
          :key="entry.articleId + entry.readAt"
          class="flex flex-wrap items-center justify-between gap-3 px-4 py-3 sm:gap-4 sm:px-6 sm:py-4"
        >
          <div class="min-w-0 space-y-2">
            <p class="truncate text-sm font-medium text-text">{{ entry.title || '未命名文章' }}</p>
            <p class="text-xs text-text-muted">{{ formatRelativeTime(entry.readAt) }}</p>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <button class="text-sm font-medium text-primary transition hover:opacity-80" @click="viewArticle(entry.articleId)">
              查看详情
            </button>
          </div>
        </li>
        <li v-if="!items.length" class="px-4 py-8 text-center text-text-muted sm:px-6 sm:py-10">还没有阅读记录。</li>
      </ul>

      <footer class="flex flex-wrap items-center justify-between gap-3 border-t border-outline/30 px-4 py-3 text-sm text-text-secondary sm:px-6 sm:py-4">
        <div>共 {{ totalText }}</div>
        <div class="flex items-center gap-3">
          <button
            class="rounded-full border border-outline/60 px-3 py-1.5 font-medium transition disabled:cursor-not-allowed disabled:border-outline/30 disabled:text-text-muted disabled:opacity-70"
            :disabled="!hasPreviousPage"
            @click="prevPage"
          >
            上一页
          </button>
          <span>第 {{ page }} 页</span>
          <button
            class="rounded-full border border-outline/60 px-3 py-1.5 font-medium transition disabled:cursor-not-allowed disabled:border-outline/30 disabled:text-text-muted disabled:opacity-70"
            :disabled="!hasNextPage"
            @click="nextPage"
          >
            下一页
          </button>
        </div>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useHistoryStore } from '../stores/history';
import { formatRelativeTime } from '../utils/datetime';

const router = useRouter();
const historyStore = useHistoryStore();
const { items, page, total, hasNextPage, hasPreviousPage } = storeToRefs(historyStore);

const refresh = async () => {
  try {
    await historyStore.fetchHistory();
  } catch (err) {
    console.warn('阅读历史刷新失败', err);
  }
};

const nextPage = async () => {
  if (!hasNextPage.value) {
    return;
  }
  await historyStore.fetchHistory({ page: page.value + 1 });
};

const prevPage = async () => {
  if (!hasPreviousPage.value) {
    return;
  }
  await historyStore.fetchHistory({ page: Math.max(1, page.value - 1) });
};

const viewArticle = (articleId: string) => {
  router.push({ name: 'article-detail', params: { id: articleId } });
};

const totalText = computed(() => {
  if (total.value === null) {
    return '0 条记录';
  }
  return `${total.value} 条记录`;
});

onMounted(() => {
  refresh();
});
</script>
