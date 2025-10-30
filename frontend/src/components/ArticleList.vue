<template>
  <section class="space-y-3 sm:space-y-4">
    <div v-if="loading" class="grid gap-3 sm:grid-cols-2 sm:gap-1 xl:grid-cols-3 2xl:grid-cols-4">
      <div v-for="i in 20" :key="`recommend-skeleton-${i}`"
        class="animate-pulse rounded-xl bg-surface-container px-4 py-5 shadow-sm shadow-black/0 transition sm:px-5 sm:py-6">
        <figure class="relative aspect-video w-full overflow-hidden bg-surface rounded-xl">
          <div class="h-full w-full bg-outline/15 rounded">
          </div>
        </figure>
        <div class="mt-8 flex items-center justify-between gap-3">
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
        <!-- <div class="mt-6 flex flex-wrap gap-2">
          <div class="h-6 w-16 rounded-full bg-outline/10" />
          <div class="h-6 w-20 rounded-full bg-outline/10" />
          <div class="h-6 w-12 rounded-full bg-outline/10" />
        </div> -->
      </div>
    </div>
    <div v-else-if="items.length" class="grid gap-3 sm:grid-cols-2 sm:gap-1 xl:grid-cols-3 2xl:grid-cols-4">
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

const props = withDefaults(defineProps<{ items: ArticleListItemProps[]; emptyMessage?: string, loading?: boolean }>(), {
  emptyMessage: '暂无文章，添加订阅后即可看到 AI 推荐内容。', loading: false
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
