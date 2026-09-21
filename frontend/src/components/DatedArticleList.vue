<template>
  <div class="space-y-6 sm:space-y-8">
    <section v-for="group in groups" :key="group.key" class="space-y-3 sm:space-y-4">
      <h2 class="text-sm sm:text-base font-medium text-text">{{ group.label }}</h2>
      <ArticleCardList :loading="false" :items="group.items" :meta-field="timeField" show-action action-label="文章选项菜单">
        <template #empty-thumbnail>
          <svg class="w-8 h-8 sm:w-10 sm:h-10 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
            <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
          </svg>
        </template>
        <template #action-dropdown="{ item, close }">
          <router-link :to="`/articles/${item.articleId}`" target="_blank" class="menu-action text-text hover:bg-gray-100 dark:hover:bg-gray-700" @click="close">在新标签页打开</router-link>
          <div class="border-t border-gray-200 dark:border-gray-700 my-1"></div>
          <button :disabled="removing" class="menu-action text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 disabled:opacity-50" @click="$emit('remove', item.articleId); close()">{{ removeLabel }}</button>
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
  return [...grouped.values()].sort((left, right) => right.key - left.key);
});
</script>

<style scoped>
.menu-action {
  @apply flex w-full items-center gap-2 px-4 py-2 text-left text-sm transition-colors;
}
</style>
