<template>
  <div class="">
    <div class="max-w-screen-lg mx-auto px-3 sm:px-6 py-4 sm:py-6">
      <!-- Page Header -->
      <div class="mb-6 sm:mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold text-text">阅读记录</h1>
      </div>

      <!-- Unauthenticated State -->
      <div v-if="!authStore.isAuthenticated" class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
        <div class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
          <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"/>
          </svg>
        </div>
        <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">请先登录</h2>
        <p class="text-xs sm:text-sm text-text-secondary px-4 mb-6">登录后即可查看你的阅读记录</p>
        <router-link
            to="/auth"
            class="inline-block px-6 py-2.5 bg-secondary text-white rounded-full hover:bg-secondary-hover transition-colors text-sm font-medium"
        >
          前往登录
        </router-link>
      </div>

      <!-- Authenticated Content -->
      <template v-else>
        <!-- Controls Bar -->
        <div class="flex items-center justify-between mb-4 sm:mb-6">
          <div class="text-xs sm:text-sm text-text-secondary">
            {{ totalText }}
          </div>
          <div class="flex items-center gap-2">

            <button
                @click="refresh"
                :disabled="loading"
                class="p-2 hover:bg-surface-container rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                aria-label="刷新阅读历史"
            >
              <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="w-5 h-5 text-text-secondary transition-transform"
                  :class="{ 'animate-spin': loading }"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
              >
                <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Error State -->
        <div v-if="error" class="mb-4 p-3 sm:p-4 bg-red-50 border border-red-200 rounded-lg">
          <div class="flex items-center gap-2 text-xs sm:text-sm text-red-800">
            <svg class="w-4 h-4 sm:w-5 sm:h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <span>{{ error }}</span>
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="!items.length && !loading" class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
          <div class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
            <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
            </svg>
          </div>
          <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">还没有阅读记录</h2>
          <p class="text-xs sm:text-sm text-text-secondary px-4">开始阅读文章后，你的历史记录将显示在这里</p>
        </div>

        <!-- Date Sections -->
        <div v-else class="space-y-6 sm:space-y-8">
          <section v-for="group in groupedByDate" :key="group.label" class="space-y-3 sm:space-y-4">
            <h2 class="text-sm sm:text-base font-medium text-text">{{ group.label }}</h2>

            <article-card-list
                :loading="loading"
                :items="group.items"
                meta-field="readAt"
                :show-action="true"
                action-label="文章选项菜单"
            >
              <template #empty-thumbnail>
                <svg class="w-8 h-8 sm:w-10 sm:h-10 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                  <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
                </svg>
              </template>

              <!-- Dropdown Menu -->
              <template #action-dropdown="{ item, close }">
                <button
                    @click.stop.prevent="handleOpenInNewTab(item); close()"
                    class="w-full px-4 py-2 text-left text-sm text-text hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors flex items-center gap-2"
                >

                  在新标签页打开
                </button>

                <div class="border-t border-gray-200 dark:border-gray-700 my-1"></div>

                <button
                    @click.stop.prevent="handleDeleteItem(item); close()"
                    class="w-full px-4 py-2 text-left text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors flex items-center gap-2"
                >
                  删除
                </button>
              </template>

            </article-card-list>
          </section>
        </div>

        <!-- Pagination -->
        <pagination
            v-if="items.length && !loading"
            :current-page="page"
            :has-previous-page="hasPreviousPage"
            :has-next-page="hasNextPage"
            :disabled="loading"
            @prev-page="prevPage"
            @next-page="nextPage"
        />
      </template>
    </div>


    <!-- Delete Confirmation Dialog -->
    <div v-if="showDeleteDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="showDeleteDialog = false">
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
      <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-sm w-full p-6 space-y-4">
        <h3 class="text-lg font-semibold text-text">删除记录</h3>
        <p class="text-sm text-text-secondary">确定要删除这条阅读记录吗？</p>
        <div class="flex gap-3 justify-end">
          <button
              @click="showDeleteDialog = false"
              class="px-4 py-2 text-sm font-medium text-text-secondary hover:bg-surface-container rounded-lg transition-colors"
          >
            取消
          </button>
          <button
              @click="confirmDelete"
              :disabled="deleting"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors disabled:opacity-50"
          >
            {{ deleting ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useHistoryStore } from '../stores/history';
import { useAuthStore } from '../stores/auth';

const historyStore = useHistoryStore();
const authStore = useAuthStore();
const { loading, items, page, total, hasNextPage, hasPreviousPage, error } = storeToRefs(historyStore);

// 删除相关状态
const showDeleteDialog = ref(false);
const deleting = ref(false);
const itemToDelete = ref<string | null>(null);

// 菜单相关状态
const activeDropdown = ref<string | null>(null);

// 抽象的加载方法，供页面其它地方调用
const loadData = async (targetPage: number, options?: { scrollToTop?: boolean }) => {
  if (loading.value || !authStore.isAuthenticated) return;

  try {
    await historyStore.fetchHistory({ page: targetPage });

    if (options?.scrollToTop) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  } catch (err) {
    console.error('加载阅读历史失败:', err);
    throw err;
  }
};

const refresh = async () => {
  await loadData(0);
};

const nextPage = async () => {
  if (!hasNextPage.value) return;
  await loadData(page.value + 1, { scrollToTop: true });
};

const prevPage = async () => {
  if (!hasPreviousPage.value) return;
  await loadData(Math.max(1, page.value - 1), { scrollToTop: true });
};


// 在新标签页打开
const handleOpenInNewTab = (item: any) => {
  activeDropdown.value = null;
  if (item?.articleId) {
    const url = `/articles/${item.articleId}`;
    window.open(url, '_blank');
  }
};


// 处理删除菜单项
const handleDeleteItem = (item: any) => {
  activeDropdown.value = null;
  if (item) {
    itemToDelete.value = item.articleId;
    showDeleteDialog.value = true;
  }
};

const confirmDelete = async () => {
  if (!itemToDelete.value) return;

  deleting.value = true;
  try {
    await historyStore.deleteHistoryEntry(itemToDelete.value);
    showDeleteDialog.value = false;
    itemToDelete.value = null;
  } catch (err) {
    console.error('删除失败:', err);
  } finally {
    deleting.value = false;
  }
};


const totalText = computed(() =>
    (total.value === null || total.value === 0) ? '暂无记录' : `共 ${total.value} 条记录`
);

const getDateLabel = (date: Date): string => {
  const today = new Date();
  const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const todayOnly = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const daysDiff = Math.floor((todayOnly.getTime() - dateOnly.getTime()) / (86400000));

  if (daysDiff === 0) return '今天';
  if (daysDiff === 1) return '昨天';

  return date.getFullYear() === today.getFullYear()
      ? `${date.getMonth() + 1}月${date.getDate()}日`
      : `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
};

const groupedByDate = computed(() => {
  const groups = new Map<string, { label: string; date: Date; items: typeof items.value }>();

  items.value.forEach(item => {
    const date = new Date(item.readAt);
    const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const dateLabel = getDateLabel(date);

    if (!groups.has(dateLabel)) {
      groups.set(dateLabel, { label: dateLabel, date: dateOnly, items: [] });
    }
    groups.get(dateLabel)!.items.push(item);
  });

  return Array.from(groups.values()).sort((a, b) => b.date.getTime() - a.date.getTime());
});

onMounted(refresh);
</script>