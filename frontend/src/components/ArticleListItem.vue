<template>
  <article
    class="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 hover:shadow-md transition cursor-pointer"
    @click="handleSelect"
  >
    <div class="flex gap-4">
      <figure
        v-if="article.thumbnail"
        class="hidden overflow-hidden rounded-2xl bg-slate-100 sm:block sm:h-28 sm:w-28 lg:h-32 lg:w-32 flex-shrink-0"
      >
        <img
          :src="article.thumbnail"
          alt="文章缩略图"
          loading="lazy"
          class="h-full w-full object-cover"
        />
      </figure>
      <div class="flex-1">
        <header class="flex items-start justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-900 leading-tight">
              {{ article.title }}
            </h3>
            <p class="text-sm text-slate-400 mt-1">
              {{ article.feedTitle }} - {{ article.timeAgo }}
            </p>
          </div>
          <button
            class="text-sm rounded-full border border-slate-200 px-3 py-1 hover:border-blue-400 hover:text-blue-600 transition"
            :class="article.collected ? 'bg-blue-50 border-blue-200 text-blue-600' : ''"
            aria-label="收藏"
            @click.stop="handleToggleFavorite"
          >
            {{ article.collected ? '已收藏' : '收藏' }}
          </button>
        </header>
        <p
          class="text-sm text-slate-600 mt-4 leading-relaxed overflow-hidden text-ellipsis"
          style="display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical;"
        >
          {{ article.summary }}
        </p>
        <footer class="mt-4 flex flex-wrap gap-2 text-xs text-slate-500">
          <button
            v-for="tag in article.tags"
            :key="tag"
            type="button"
            class="px-2 py-1 rounded-full bg-slate-100 hover:bg-blue-50 hover:text-blue-600 transition"
            @click.stop="handleTagClick(tag)"
          >
            #{{ tag }}
          </button>
        </footer>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue';

export interface ArticleListItemProps {
  id: string;
  title: string;
  summary: string;
  feedTitle: string;
  timeAgo: string;
  tags: string[];
  collected?: boolean;
  link?: string;
  thumbnail?: string;
  enclosure?: string;
}

const props = defineProps<{ article: ArticleListItemProps }>();
const emit = defineEmits<{
  select: [string];
  'toggle-favorite': [string];
  'select-tag': [string];
}>();

const article = computed(() => props.article);

const handleSelect = () => {
  emit('select', article.value.id);
};

const handleToggleFavorite = () => {
  emit('toggle-favorite', article.value.id);
};

const handleTagClick = (tag: string) => {
  if (!tag) {
    return;
  }
  emit('select-tag', tag);
};
</script>
