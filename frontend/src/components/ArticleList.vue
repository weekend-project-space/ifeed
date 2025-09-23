<template>
  <section class="space-y-4">
    <ArticleListItem
      v-for="item in items"
      :key="item.id"
      :article="item"
      @select="emitSelect"
      @toggle-favorite="emitToggleFavorite"
      @select-tag="emitSelectTag"
    />
    <div v-if="!items.length" class="text-center text-slate-400 py-20 border-2 border-dashed border-slate-200 rounded-3xl">
      {{ emptyMessage }}
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
