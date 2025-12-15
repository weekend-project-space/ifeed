<!--ArticleCardList.vue-->
<template>
  <div v-if="loading" role="status" aria-live="polite" class="space-y-3 sm:space-y-4">
    <span class="sr-only">正在加载列表</span>
    <div v-for="i in 3" :key="i" class="flex flex-col sm:flex-row gap-3 sm:gap-4 animate-pulse">
      <div :class="[
        'bg-surface-container rounded-lg flex-shrink-0',
        compact ? 'w-full sm:w-24 h-20 sm:h-16' : 'w-full sm:w-52 h-40 sm:h-32'
      ]"></div>
      <div class="flex-1 space-y-2 sm:space-y-3 py-1 sm:py-2">
        <div :class="['bg-surface-container rounded w-3/4', compact ? 'h-3 sm:h-4' : 'h-4 sm:h-5']"></div>
        <div :class="['bg-surface-container rounded w-1/2', compact ? 'h-2 sm:h-3' : 'h-3 sm:h-4']"></div>
        <div v-if="!compact" class="h-3 sm:h-4 bg-surface-container rounded w-full"></div>
      </div>
    </div>
  </div>
  <div v-else :class="compact ? 'space-y-2 sm:space-y-2.5' : 'space-y-4 sm:space-y-5'">
    <article v-for="(item, index) in items" :key="getItemKey(item, index)">
      <router-link
          :to="`/articles/${item.articleId}`"
          :class="[
            'group flex flex-col sm:flex-row hover:bg-surface-container/50 -mx-2 px-2 rounded-lg transition-colors',
            compact ? 'gap-2 sm:gap-3 py-1.5' : 'gap-3 sm:gap-4 py-2'
          ]"
      >
        <!-- Thumbnail -->
        <div :class="[
          'relative flex-shrink-0 rounded-lg overflow-hidden bg-surface-container',
          compact ? 'w-full sm:w-24 h-20 sm:h-16' : 'w-full sm:w-52 h-40 sm:h-32'
        ]">
          <img
              v-if="item.thumbnail"
              :src="item.thumbnail"
              :alt="item.title || '文章缩略图'"
              class="w-full h-full object-cover"
              loading="lazy"
              referrerpolicy="no-referrer"
              @error="handleImageError"
          />
          <div v-else class="w-full h-full flex items-center justify-center">
            <slot name="empty-thumbnail" :item="item">
              <svg :class="['text-text-muted', compact ? 'w-6 h-6 sm:w-7 sm:h-7' : 'w-8 h-8 sm:w-10 sm:h-10']" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
              </svg>
            </slot>
          </div>

          <!-- Action Button (Mobile) -->
          <button
              v-if="showAction"
              @click.stop.prevent="$emit('action', item)"
              :class="[
                'sm:hidden absolute top-2 right-2 bg-black/60 backdrop-blur-sm hover:bg-black/80 rounded-full transition-all',
                compact ? 'p-1.5' : 'p-2'
              ]"
              :aria-label="actionLabel"
          >
            <slot name="action-icon-mobile" :item="item">
              <svg :class="['text-white', compact ? 'w-4 h-4' : 'w-5 h-5']" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="1.5"/>
                <circle cx="12" cy="5" r="1.5"/>
                <circle cx="12" cy="19" r="1.5"/>
              </svg>
            </slot>
          </button>
        </div>

        <!-- Content -->
        <div class="flex-1 min-w-0 flex flex-col py-0 sm:py-0.5">
          <h3 :class="[
            'font-normal text-text line-clamp-2 leading-normal',
            compact ? 'text-xs sm:text-sm mb-1' : 'text-sm sm:text-base mb-1.5 sm:mb-2'
          ]">
            {{ item.title || '未命名文章' }}
          </h3>

          <div :class="[
            'flex items-center gap-1.5 text-text-secondary',
            compact ? 'text-xs mb-1' : 'text-xs sm:text-sm mb-1.5 sm:mb-2'
          ]">
            <span v-if="item.feedTitle" class="truncate">{{ item.feedTitle }}</span>
            <span v-if="item.feedTitle && getMetaText(item)" class="flex-shrink-0">•</span>
            <span v-if="getMetaText(item)" class="flex-shrink-0">{{ getMetaText(item) }}</span>
          </div>

          <p v-if="item.summary && !compact" class="text-xs sm:text-sm text-text-secondary line-clamp-2 leading-relaxed">
            {{ item.summary }}
          </p>
        </div>

        <!-- Action Button (Desktop) -->
        <button
            v-if="showAction"
            @click.stop.prevent="$emit('action', item)"
            :class="[
              'hidden sm:block self-start opacity-0 group-hover:opacity-100 hover:bg-surface-container rounded-full transition-all',
              compact ? 'p-1.5' : 'p-2'
            ]"
            :aria-label="actionLabel"
        >
          <slot name="action-icon-desktop" :item="item">
            <svg :class="['text-text-secondary', compact ? 'w-4 h-4' : 'w-5 h-5']" viewBox="0 0 24 24" fill="currentColor">
              <circle cx="12" cy="12" r="1.5"/>
              <circle cx="12" cy="5" r="1.5"/>
              <circle cx="12" cy="19" r="1.5"/>
            </svg>
          </slot>
        </button>
      </router-link>
    </article>
  </div>
</template>

<script setup lang="ts">
import { formatRelativeTime } from '../utils/datetime';

interface ArticleItem {
  articleId: string;
  title?: string;
  thumbnail?: string;
  summary?: string;
  feedTitle?: string;
  collectedAt?: string;
  readAt?: string;
  timeAgo?: string;
  [key: string]: any;
}

interface Props {
  items: ArticleItem[];
  loading: boolean;
  showAction?: boolean;
  actionLabel?: string;
  metaField?: 'collectedAt' | 'readAt' | 'timeAgo' | 'custom';
  metaPrefix?: string;
  keyField?: string;
  compact?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showAction: true,
  actionLabel: '选项',
  metaField: 'readAt',
  metaPrefix: '',
  keyField: 'articleId',
  compact: false
});

defineEmits<{
  action: [item: ArticleItem];
}>();

const getItemKey = (item: ArticleItem, index: number): string => {
  const timestamp = item.collectedAt || item.readAt || '';
  return `${item[props.keyField]}-${timestamp}-${index}`;
};

const getMetaText = (item: ArticleItem): string => {
  if (props.metaField === 'custom') return '';
  if (item.timeAgo) return item.timeAgo;
  const timestamp = item[props.metaField];
  if (!timestamp) return '';

  const timeText = formatRelativeTime(timestamp);
  return props.metaPrefix ? `${props.metaPrefix}${timeText}` : timeText;
};

const handleImageError = (e: Event) => {
  (e.target as HTMLImageElement).style.display = 'none';
};
</script>