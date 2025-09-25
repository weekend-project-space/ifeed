<template>
  <article
    class="group relative cursor-pointer overflow-hidden rounded-3xl border border-primary/15 bg-surface-container p-6 transition hover:-translate-y-1 hover:border-primary/40"
    @click="handleSelect"
  >
    <div class="pointer-events-none absolute inset-0 bg-gradient-to-br from-primary/0 via-primary/5 to-primary/10 opacity-0 transition duration-300 group-hover:opacity-100"></div>
    <div class="relative flex flex-col gap-4 sm:flex-row">
      <figure
        v-if="article.thumbnail"
        class="overflow-hidden rounded-2xl border border-primary/15 bg-surface-variant sm:h-28 sm:w-36"
      >
        <img
          :src="article.thumbnail"
          alt="文章缩略图"
          loading="lazy"
          class="h-full w-full object-cover transition duration-300 group-hover:scale-105"
        />
      </figure>
      <div class="flex flex-1 flex-col">
        <header class="flex flex-wrap items-start justify-between gap-4">
          <div class="space-y-1">
            <h3 class="text-lg font-semibold leading-tight text-text transition group-hover:text-primary">
              {{ article.title }}
            </h3>
            <p class="text-xs font-medium uppercase tracking-wide text-text-muted">
              {{ article.feedTitle }} · {{ article.timeAgo }}
            </p>
          </div>
          <button
            class="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-surface px-3 py-1.5 text-xs font-semibold text-text-secondary transition hover:bg-primary/10 hover:text-primary"
            :class="article.collected ? 'border-transparent bg-primary text-primary-foreground' : ''"
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
          </button>
        </header>
        <p
          class="mt-3 text-sm leading-relaxed text-text-secondary"
          style="display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;"
        >
          {{ article.summary }}
        </p>
        <footer class="mt-4 flex flex-wrap gap-2 text-xs text-text-muted">
          <button
            v-for="tag in article.tags"
            :key="tag"
            type="button"
            class="rounded-full bg-primary/10 px-3 py-1 font-medium text-primary transition hover:bg-primary/20"
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
