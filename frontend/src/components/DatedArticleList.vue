<template>
  <div class="space-y-6 sm:space-y-8 text-text">
    <section v-for="group in groups" :key="group.key" class="space-y-3">
      <!-- 分组日期：MD3 柔和微标题 -->
      <h2 class="text-xs font-semibold tracking-wide text-text-secondary sm:text-sm">{{ group.label }}</h2>

      <!-- 文章卡片列表 -->
      <ArticleCardList :loading="false" :items="group.items" :meta-field="timeField" show-action action-label="文章选项菜单">
        <template #empty-thumbnail>
          <svg class="h-7 w-7 text-text-muted sm:h-8 sm:w-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
          </svg>
        </template>
        <template #action-dropdown="{ item, close }">
          <router-link :to="`/articles/${item.articleId}`" target="_blank" class="menu-action" @click="close">
            在新标签页打开
          </router-link>
          <div class="border-t border-outline/10 my-1"></div>
          <button :disabled="removing" class="menu-action text-red-600 dark:text-red-400 hover:bg-red-500/10 disabled:opacity-40" @click="$emit('remove', item.articleId); close()">
            {{ removeLabel }}
          </button>
        </template>
      </ArticleCardList>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import ArticleCardList from './ArticleCardList.vue';
import type { HistoryEntryDto } from '@/api/history';

type DatedArticle = HistoryEntryDto & { likedAt?: string };
const props = defineProps<{ items: DatedArticle[]; timeField: 'readAt' | 'likedAt'; removing: boolean; removeLabel: string }>();
defineEmits<{ remove: [articleId: string] }>();

const groups = computed(() => {
  const today = new Date();
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
  const yesterdayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1).getTime();
  const grouped = new Map<number, { key: number; label: string; items: DatedArticle[] }>();

  for (const item of props.items) {
    const date = new Date(item[props.timeField] ?? '');
    const day = Number.isNaN(date.getTime()) ? -Infinity : new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
    const label = day === -Infinity ? '时间未知' : day === todayStart ? '今天' : day === yesterdayStart ? '昨天'
        : `${date.getFullYear() === today.getFullYear() ? '' : `${date.getFullYear()}年`}${date.getMonth() + 1}月${date.getDate()}日`;
    if (!grouped.has(day)) grouped.set(day, { key: day, label, items: [] });
    grouped.get(day)!.items.push(item);
  }

  // 修复了减号语法
  return [...grouped.values()].sort((left, right) => right.key - left.key);
});
</script>

<style scoped>
@reference "../styles/tailwind.css";

.menu-action {
  @apply flex w-full items-center gap-2 px-4 py-2 text-left text-xs font-medium text-text transition hover:bg-surface-container;
}
</style>