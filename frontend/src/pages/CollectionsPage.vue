<template>
  <div class="space-y-6">
    <section class="rounded-3xl border border-slate-200 bg-white shadow-sm">
      <header class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-6 py-4">
        <div>
          <h2 class="text-lg font-semibold text-slate-900">我的收藏</h2>
          <p class="text-sm text-slate-500">保存你想稍后阅读的文章。</p>
        </div>
        <button class="text-sm text-slate-400 transition hover:text-slate-600" @click="refresh">刷新</button>
      </header>

      <div v-if="loading" class="py-12 text-center text-slate-400">加载中...</div>

      <ul v-else class="divide-y divide-slate-100">
        <li
          v-for="item in items"
          :key="item.articleId + (item.collectedAt ?? '')"
          class="flex flex-wrap items-center justify-between gap-4 px-6 py-4"
        >
          <div class="min-w-0">
            <p class="truncate text-sm font-medium text-slate-800">{{ item.title || '未命名文章' }}</p>
            <p v-if="item.collectedAt" class="text-xs text-slate-400">收藏于 {{ formatRelativeTime(item.collectedAt) }}</p>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <button class="text-sm text-blue-500 hover:text-blue-600" @click="viewArticle(item.articleId)">
              查看详情
            </button>
            <button class="text-sm text-red-500 hover:text-red-600" @click="remove(item.articleId)">
              取消收藏
            </button>
          </div>
        </li>
        <li v-if="!items.length" class="px-6 py-10 text-center text-slate-400">暂时还没有收藏的文章。</li>
      </ul>

      <p v-if="errorMessage" class="border-t border-slate-100 px-6 pb-0 pt-4 text-sm text-red-500">{{ errorMessage }}</p>

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
import { useCollectionsStore } from '../stores/collections';
import { formatRelativeTime } from '../utils/datetime';

const router = useRouter();
const collectionsStore = useCollectionsStore();
const { items, page, total, loading, hasNextPage, hasPreviousPage, error } = storeToRefs(collectionsStore);

const refresh = async () => {
  try {
    await collectionsStore.fetchCollections();
  } catch (err) {
    console.warn('收藏刷新失败', err);
  }
};

const nextPage = async () => {
  if (!hasNextPage.value) {
    return;
  }
  await collectionsStore.fetchCollections({ page: page.value + 1 });
};

const prevPage = async () => {
  if (!hasPreviousPage.value) {
    return;
  }
  await collectionsStore.fetchCollections({ page: Math.max(1, page.value - 1) });
};

const viewArticle = (articleId: string) => {
  router.push({ name: 'article-detail', params: { id: articleId } });
};

const remove = async (articleId: string) => {
  try {
    await collectionsStore.removeCollection(articleId);
    if (!items.value.length && hasPreviousPage.value) {
      await collectionsStore.fetchCollections({ page: Math.max(1, page.value - 1) });
    }
  } catch (err) {
    // 错误由 store 维护
  }
};

const totalText = computed(() => {
  if (total.value === null) {
    return `${items.value.length} 条记录`;
  }
  return `${total.value} 条记录`;
});

const errorMessage = computed(() => error.value);

onMounted(() => {
  refresh();
});
</script>
