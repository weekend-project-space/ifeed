<template>
  <div class="space-y-6">
    <section class="rounded-3xl border border-slate-200 bg-white shadow-sm">
      <header class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-6 py-4">
        <div>
          <h2 class="text-lg font-semibold text-slate-900">阅读历史</h2>
          <p class="text-sm text-slate-500">回顾你最近阅读过的文章。</p>
        </div>
        <button class="text-sm text-slate-400 transition hover:text-slate-600" @click="refresh">刷新</button>
      </header>

      <div v-if="historyStore.loading" class="py-12 text-center text-slate-400">加载中...</div>

      <ul v-else class="divide-y divide-slate-100">
        <li
          v-for="entry in items"
          :key="entry.articleId + entry.readAt"
          class="flex flex-wrap items-center justify-between gap-4 px-6 py-4"
        >
          <div class="min-w-0">
            <p class="truncate text-sm font-medium text-slate-800">{{ entry.title || '未命名文章' }}</p>
            <p class="text-xs text-slate-400">{{ formatRelativeTime(entry.readAt) }}</p>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <button class="text-sm text-blue-500 hover:text-blue-600" @click="viewArticle(entry.articleId)">
              查看详情
            </button>
          </div>
        </li>
        <li v-if="!items.length" class="px-6 py-10 text-center text-slate-400">还没有阅读记录。</li>
      </ul>

      <footer class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-6 py-4 text-sm text-slate-500">
        <div>共 {{ totalText }}</div>
        <div class="flex items-center gap-3">
          <button
            class="rounded-lg border border-slate-200 px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="!hasPreviousPage"
            @click="prevPage"
          >
            上一页
          </button>
          <span>第 {{ page }} 页</span>
          <button
            class="rounded-lg border border-slate-200 px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-50"
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
