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
  <div v-else-if="items.length > 0" :class="compact ? 'space-y-2 sm:space-y-2.5' : 'space-y-4 sm:space-y-5'">
    <article v-for="item in items" :key="getItemKey(item)">
      <router-link
          :to="`/articles/${item[keyField]}`"
          :class="[
            'group flex flex-col sm:flex-row hover:bg-surface-container/50 -mx-2 px-2 rounded-lg transition-colors',
            compact ? 'gap-2 sm:gap-3 py-1.5' : 'gap-3 sm:gap-4 py-2'
          ]"
      >
        <!-- Thumbnail -->
        <div :class="[
          'relative flex-shrink-0 rounded-lg overflow-hidden bg-surface-container',
          compact ? 'w-full sm:w-24 h-24 sm:h-16' : 'w-full sm:w-52 h-40 sm:h-32'
        ]">
          <img
              v-if="shouldShowImage(item)"
              :src="item.thumbnail"
              :alt="item.title || '文章缩略图'"
              class="w-full h-full object-cover"
              loading="lazy"
              referrerpolicy="no-referrer"
              @error="handleImageError(item)"
          />
          <div v-else class="w-full h-full flex items-center justify-center">
            <slot name="empty-thumbnail" :item="item">
              <svg :class="['text-text-muted', compact ? 'w-6 h-6 sm:w-7 sm:h-7' : 'w-8 h-8 sm:w-10 sm:h-10']" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <polyline points="21 15 16 10 5 21"/>
              </svg>
            </slot>
          </div>

          <!-- Action Button (Mobile) -->
          <button
              v-if="showAction"
              @click.stop.prevent="onActionClick(item, $event)"
              :aria-expanded="activeDropdown === getItemKey(item)"
              :class="[
                'sm:hidden absolute top-2 right-2 bg-black/60 backdrop-blur-sm hover:bg-black/80 rounded-full transition-all',
                compact ? 'p-1.5' : 'p-2'
              ]"
              :aria-label="actionLabel"
          >
            <slot name="action-icon" :item="item">
              <svg :class="['text-white', compact ? 'w-4 h-4' : 'w-5 h-5']" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="1.5"/>
                <circle cx="12" cy="5" r="1.5"/>
                <circle cx="12" cy="19" r="1.5"/>
              </svg>
            </slot>
            <div
                v-if="hasDropdownSlot && activeDropdown === getItemKey(item)"
                @click.stop
                class="absolute right-0 top-full mt-1 w-44 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-50"
            >
              <slot name="action-dropdown" :item="item" :close="closeDropdown"></slot>
            </div>
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
            @click.stop.prevent="onActionClick(item, $event)"
            :aria-expanded="activeDropdown === getItemKey(item)"
            :class="[
              'hidden sm:block self-start opacity-0 group-hover:opacity-100 hover:bg-surface-container rounded-full transition-all relative z-20',
              compact ? 'p-1.5' : 'p-2'
            ]"
            :aria-label="actionLabel"
        >
          <slot name="action-icon" :item="item">
            <svg :class="['text-text-secondary', compact ? 'w-4 h-4' : 'w-5 h-5']" viewBox="0 0 24 24" fill="currentColor">
              <circle cx="12" cy="12" r="1.5"/>
              <circle cx="12" cy="5" r="1.5"/>
              <circle cx="12" cy="19" r="1.5"/>
            </svg>
          </slot>
          <div
              v-if="hasDropdownSlot && activeDropdown === getItemKey(item)"
              @click.stop.prevent
              class="absolute right-0 top-full mt-1 w-44 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-50"
          >
            <slot name="action-dropdown" :item="item" :close="closeDropdown"></slot>
          </div>
        </button>
      </router-link>
    </article>
  </div>
  <div v-else class="flex flex-col items-center justify-center py-12 sm:py-16 text-center">
    <svg class="w-12 h-12 sm:w-16 sm:h-16 text-text-muted mb-3 sm:mb-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
      <path d="M3 9h18"/>
      <path d="M9 21V9"/>
    </svg>
    <p class="text-sm sm:text-base text-text-secondary">暂无文章</p>
  </div>
  <div v-if="activeDropdown" @click="closeDropdown" class="fixed inset-0 z-0"></div>
</template>

<script setup lang="ts">
import { ref, computed, useSlots } from 'vue';
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
}

interface Props {
  items: ArticleItem[];
  loading: boolean;
  showAction?: boolean;
  actionLabel?: string;
  metaField?: 'collectedAt' | 'readAt' | 'timeAgo' | 'custom';
  metaPrefix?: string;
  keyField?: keyof ArticleItem;
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

const slots = useSlots();
const hasDropdownSlot = computed(() => !!slots['action-dropdown']);

const emit = defineEmits<{
  action: [item: ArticleItem, event: MouseEvent];
}>();

const activeDropdown = ref<string | null>(null);

const toggleDropdown = (key: string) => {
  activeDropdown.value = activeDropdown.value === key ? null : key;
};

const closeDropdown = () => {
  activeDropdown.value = null;
};

const onActionClick = (item: ArticleItem, event: MouseEvent) => {
  toggleDropdown(getItemKey(item));
  emit('action', item, event);
};

const failedImages = ref<Set<string>>(new Set());

const getItemKey = (item: ArticleItem): string => {
  return String(item[props.keyField]);
};

const getMetaText = (item: ArticleItem): string => {
  if (props.metaField === 'custom') return '';
  if (item.timeAgo) return item.timeAgo;
  const timestamp = item[props.metaField as keyof ArticleItem] as string | undefined;
  if (!timestamp) return '';

  const timeText = formatRelativeTime(timestamp);
  return props.metaPrefix ? `${props.metaPrefix}${timeText}` : timeText;
};

const handleImageError = (item: ArticleItem) => {
  const key = String(item[props.keyField]);
  failedImages.value.add(key);
};

const shouldShowImage = (item: ArticleItem): boolean => {
  const key = String(item[props.keyField]);
  return !!item.thumbnail && !failedImages.value.has(key);
};
</script>