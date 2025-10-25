<template>
  <article
    class="group relative flex h-full cursor-pointer flex-col overflow-hidden rounded-xl border border-outline/20 bg-surface transition hover:border-outline/40"
    @click="handleSelect">
    <figure class="relative aspect-video w-full overflow-hidden bg-surface">
      <img v-if="article.thumbnail && !thumbError" :src="article.thumbnail" alt="文章缩略图" loading="lazy"
        class="h-full w-full object-cover transition duration-300 group-hover:scale-[1.02]"
        @error="thumbError = true" />
      <div v-else class="flex h-full w-full items-center justify-center">
        <div class="flex h-full w-full items-center justify-center">
          <div
            class="flex items-center gap-2 rounded-md border border-outline/30 bg-surface px-3 py-2 text-xs text-text-muted">
            <svg class="h-4 w-4 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5">
              <rect x="3" y="3" width="18" height="14" rx="2" ry="2"></rect>
              <path d="M3 17l5-5 4 4 3-3 6 6"></path>
            </svg>
            缩略图不可用
          </div>
        </div>
      </div>
      <!-- Remove hover gradient overlay for a flatter look -->
    </figure>
    <div class="flex flex-1 flex-col gap-4 p-5">
      <header class="flex items-start gap-4">
        <div class="min-w-0 flex-1 space-y-1">
          <h3 class="text-base font-semibold leading-tight text-text"
            style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">
            {{ article.title }}
          </h3>
          <p class="text-xs text-text-muted">
            {{ article.feedTitle }} · {{ article.timeAgo }}
          </p>
        </div>
        <!-- <button
          class="inline-flex shrink-0 items-center justify-center gap-2 rounded-full border border-outline/30 bg-surface px-3 py-1.5 text-xs font-medium text-text-secondary transition hover:border-outline/50 min-w-[6rem]"
          :class="article.collected ? 'border-primary bg-primary/15 text-primary' : ''"
          aria-label="收藏"
          @click.stop="handleToggleFavorite"
        >
          <svg
            class="h-4 w-4"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              v-if="article.collected"
              d="M9.049 2.927a1 1 0 0 1 1.902 0l1.071 3.296a1 1 0 0 0 .95.69h3.462a1 1 0 0 1 .588 1.806l-2.8 2.034a1 1 0 0 0-.364 1.118l1.07 3.296a1 1 0 0 1-1.538 1.118L10 14.347l-2.956 2.938a1 1 0 0 1-1.538-1.118l1.07-3.296a1 1 0 0 0-.364-1.118l-2.8-2.034a1 1 0 0 1 .588-1.806h3.462a1 1 0 0 0 .95-.69z"
            />
            <path
              v-else
              d="M9.049 2.927a1 1 0 0 1 1.902 0l1.071 3.296a1 1 0 0 0 .95.69h3.462a1 1 0 0 1 .588 1.806l-2.8 2.034a1 1 0 0 0-.364 1.118l1.07 3.296a1 1 0 0 1-1.538 1.118L10 14.347l-2.956 2.938a1 1 0 0 1-1.538-1.118l1.07-3.296a1 1 0 0 0-.364-1.118l-2.8-2.034a1 1 0 0 1 .588-1.806h3.462a1 1 0 0 0 .95-.69z"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
            />
          </svg>
          {{ article.collected ? '已收藏' : '收藏' }}
        </button> -->
      </header>
      <p class="text-sm leading-relaxed text-text-secondary"
        style="display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;">
        {{ article.summary }}
      </p>
      <footer class="mt-auto flex flex-wrap gap-2 text-xs text-text-muted">
        <button v-for="tag in article.tags" :key="tag" type="button"
          class="rounded-full bg-surface px-3 py-1 font-medium text-text-secondary border border-outline/30 hover:border-outline/50"
          @click.stop="handleTagClick(tag)">
          #{{ tag }}
        </button>
      </footer>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

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
const thumbError = ref(false);

const handleSelect = () => {
  emit('select', article.value.id);
};

// const handleToggleFavorite = () => {
//   emit('toggle-favorite', article.value.id);
// };

const handleTagClick = (tag: string) => {
  if (!tag) {
    return;
  }
  emit('select-tag', tag);
};
</script>
