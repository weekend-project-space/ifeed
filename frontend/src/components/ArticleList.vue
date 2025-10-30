<template>
  <section class="space-y-3 sm:space-y-4">
    <div v-if="items.length" class="grid gap-3 sm:grid-cols-2 sm:gap-1 xl:grid-cols-3 2xl:grid-cols-4">
      <ArticleListItem v-for="item in items" :key="item.id" :article="item" @select="emitSelect"
        @toggle-favorite="emitToggleFavorite" @select-tag="emitSelectTag" />
    </div>
    <div v-else
      class="flex flex-col items-center justify-center gap-3 rounded-xl border border-outline/30 bg-surface py-16 text-sm text-text-muted">
      <svg class="h-10 w-10 text-primary/40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 6.75h15m-15 4.5h15m-15 4.5h7.5" />
      </svg>
      <span class="max-w-xs text-center leading-relaxed">{{ emptyMessage }}</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ArticleListItemProps } from './ArticleListItem.vue';
import ArticleListItem from './ArticleListItem.vue';

const props = withDefaults(defineProps<{ items: ArticleListItemProps[]; emptyMessage?: string }>(), {
  emptyMessage: '暂无文章，添加订阅后即可看到 AI 推荐内容。'
});
const emit = defineEmits<{
  select: [string];
  'toggle-favorite': [string];
  'select-tag': [string];
}>();

const items = computed(() => props.items);
const emptyMessage = computed(() => props.emptyMessage);

const emitSelect = (articleId: string) => emit('select', articleId);
const emitToggleFavorite = (articleId: string) => emit('toggle-favorite', articleId);
const emitSelectTag = (tag: string) => emit('select-tag', tag);
</script>
