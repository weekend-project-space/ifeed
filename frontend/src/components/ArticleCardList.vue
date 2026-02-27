<!--ArticleCardList.vue-->
<template>
  <!-- Loading Skeleton -->
  <div v-if="loading" role="status" aria-live="polite" :class="sizes.skeletonOuter">
    <span class="sr-only">正在加载列表</span>
    <div v-for="i in 3" :key="i" :class="[sizes.skeletonRow, 'animate-pulse']">
      <div :class="['bg-surface-container rounded-lg flex-shrink-0', sizes.thumbnail]"></div>
      <div class="flex-1 space-y-2 py-1 sm:space-y-3 sm:py-2">
        <div :class="['bg-surface-container rounded w-3/4', sizes.skeletonLine1]"></div>
        <div :class="['bg-surface-container rounded w-1/2', sizes.skeletonLine2]"></div>
        <div v-if="!compact" class="h-3 sm:h-4 bg-surface-container rounded w-full"></div>
      </div>
    </div>
  </div>

  <!-- List -->
  <div v-else-if="items.length > 0" :class="sizes.listGap">
    <article v-for="item in itemsWithMeta" :key="item._key">
      <div :class="['group relative hover:bg-surface-container/60 -mx-2 px-2 rounded-lg transition-colors', sizes.cardFlex, sizes.cardPad]">
        <router-link :to="`/articles/${item._key}`" :class="sizes.link">
          <!-- Thumbnail -->
          <div :class="['relative flex-shrink-0 rounded-lg overflow-hidden bg-surface-container', sizes.thumbnail]">
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
                <svg :class="['text-text-muted', sizes.thumbIcon]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
              </slot>
            </div>
          </div>

          <!-- Content -->
          <div class="flex-1 min-w-0 flex flex-col py-0 sm:py-0.5">
            <h3 :class="['font-normal text-text line-clamp-2 leading-normal', sizes.title]">
              {{ item.title || '未命名文章' }}
            </h3>
            <div :class="['flex items-center gap-1.5 text-text-secondary', sizes.meta]">
              <span v-if="item.feedTitle" class="truncate">{{ item.feedTitle }}</span>
              <span v-if="item.feedTitle && item._meta" class="flex-shrink-0">•</span>
              <span v-if="item._meta" class="flex-shrink-0">{{ item._meta }}</span>
            </div>
            <p v-if="item.summary && !compact" class="text-xs sm:text-sm text-text-secondary line-clamp-2 leading-relaxed">
              {{ item.summary }}
            </p>
          </div>
        </router-link>

        <!-- Non-compact mobile: absolute overlay on thumbnail.
             NOTE: action-icon slot and dropdown are intentionally duplicated here
             and in the inline section below — the two positions require genuinely
             different CSS (absolute vs inline-flex), so merging them is not feasible
             without significant layout complexity. -->
        <div v-if="showAction && !compact" class="sm:hidden absolute top-2 right-2 z-10">
          <button
            @click.stop.prevent="onActionClick(item, $event)"
            :aria-expanded="activeDropdown === item._key"
            class="p-2 bg-black/60 backdrop-blur-sm hover:bg-black/80 rounded-full transition-all relative z-20"
            :aria-label="actionLabel"
          >
            <slot name="action-icon" :item="item">
              <svg class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="19" r="1.5"/>
              </svg>
            </slot>
          </button>
          <div v-if="hasDropdownSlot && activeDropdown === item._key" @click.stop.prevent class="absolute right-0 top-full mt-1 w-44 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-50">
            <slot name="action-dropdown" :item="item" :close="closeDropdown"></slot>
          </div>
        </div>

        <!-- Compact (all sizes) + non-compact desktop: inline -->
        <div v-if="showAction" :class="['relative flex self-start', sizes.actionWrapper]">
          <button
            @click.stop.prevent="onActionClick(item, $event)"
            :aria-expanded="activeDropdown === item._key"
            :class="['rounded-full transition-all relative z-20 hover:bg-surface-container/60', sizes.actionBtn]"
            :aria-label="actionLabel"
          >
            <slot name="action-icon" :item="item">
              <svg :class="['text-text-secondary', sizes.actionIcon]" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="19" r="1.5"/>
              </svg>
            </slot>
          </button>
          <div v-if="hasDropdownSlot && activeDropdown === item._key" @click.stop.prevent class="absolute right-0 top-full mt-1 w-44 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-50">
            <slot name="action-dropdown" :item="item" :close="closeDropdown"></slot>
          </div>
        </div>
      </div>
    </article>
  </div>

  <!-- Empty -->
  <div v-else :class="['flex flex-col items-center justify-center text-center', sizes.emptyPad]">
    <svg :class="['text-text-muted', sizes.emptyIcon]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
      <path d="M3 9h18"/>
      <path d="M9 21V9"/>
    </svg>
    <p :class="['text-text-secondary', sizes.emptyText]">暂无文章</p>
  </div>

  <div v-if="hasDropdownSlot && activeDropdown" @click="closeDropdown" class="fixed inset-0 z-0"></div>
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
  relativeTime?: string;
}

type AugmentedItem = ArticleItem & { _key: string; _meta: string };

interface Props {
  items: ArticleItem[];
  loading: boolean;
  showAction?: boolean;
  actionLabel?: string;
  metaField?: 'collectedAt' | 'readAt' | 'timeAgo' | 'relativeTime' | 'custom';
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
  compact: false,
});

const slots = useSlots();
const hasDropdownSlot = computed(() => !!slots['action-dropdown']);
const emit = defineEmits<{ action: [item: ArticleItem, event: MouseEvent] }>();

const activeDropdown = ref<string | null>(null);
const closeDropdown = () => { activeDropdown.value = null; };

const onActionClick = ({ _key, _meta: _, ...item }: AugmentedItem, event: MouseEvent) => {
  if (hasDropdownSlot.value) {
    activeDropdown.value = activeDropdown.value === _key ? null : _key;
  }
  emit('action', item, event);
};

const failedImages = ref(new Set<string>());

const getMetaText = (item: ArticleItem): string => {
  if (props.metaField === 'custom') return '';
  if (props.metaField === 'relativeTime') return item.relativeTime || '';
  if (props.metaField === 'timeAgo') return item.timeAgo || '';
  const timestamp = item[props.metaField as 'collectedAt' | 'readAt'];
  if (!timestamp) return '';
  const timeText = formatRelativeTime(timestamp);
  return props.metaPrefix ? `${props.metaPrefix}${timeText}` : timeText;
};

const itemsWithMeta = computed<AugmentedItem[]>(() =>
  props.items.map(item => ({
    ...item,
    _key: String(item[props.keyField]),
    _meta: getMetaText(item),
  }))
);

// All compact/normal size tokens in one place.
// compact ≈ 3/4 of normal: thumbnail 160×96px vs 208×128px, text xs vs sm/base
const sizes = computed(() => {
  const c = props.compact;
  return {
    skeletonOuter:  c ? 'space-y-2 sm:space-y-2.5'              : 'space-y-3 sm:space-y-4',
    skeletonRow:    c ? 'flex items-start gap-2'                 : 'flex flex-col sm:flex-row gap-2 sm:gap-4',
    skeletonLine1:  c ? 'h-3 sm:h-4'                            : 'h-4 sm:h-5',
    skeletonLine2:  c ? 'h-2 sm:h-3'                            : 'h-3 sm:h-4',
    listGap:        c ? 'space-y-2 sm:space-y-2.5'              : 'space-y-4 sm:space-y-5',
    cardFlex:       c ? 'flex items-start'                       : 'sm:flex sm:items-start',
    cardPad:        c ? 'py-1.5'                                 : 'py-2',
    link:           c ? 'flex-1 flex flex-row gap-2'            : 'flex-1 flex flex-col sm:flex-row gap-2 sm:gap-4',
    thumbnail:      c ? 'w-32 h-20'                             : 'w-full sm:w-52 h-40 sm:h-32',
    thumbIcon:      c ? 'w-6 h-6 sm:w-7 sm:h-7'                : 'w-8 h-8 sm:w-10 sm:h-10',
    title:          c ? 'text-xs sm:text-sm mb-1'               : 'text-sm sm:text-base mb-1.5 sm:mb-2',
    meta:           c ? 'text-xs mb-1'                          : 'text-xs sm:text-sm mb-1.5 sm:mb-2',
    actionWrapper:  c ? 'ml-1'                                  : 'hidden sm:flex ml-2',
    actionBtn:      c ? 'p-1.5'                                 : 'p-2 opacity-0 group-hover:opacity-100',
    actionIcon:     c ? 'w-4 h-4'                               : 'w-5 h-5',
    emptyPad:       c ? 'py-6 sm:py-8'                          : 'py-12 sm:py-16',
    emptyIcon:      c ? 'w-8 h-8 sm:w-10 sm:h-10 mb-2 sm:mb-3' : 'w-12 h-12 sm:w-16 sm:h-16 mb-3 sm:mb-4',
    emptyText:      c ? 'text-xs sm:text-sm'                    : 'text-sm sm:text-base',
  };
});

const handleImageError = (item: AugmentedItem) => {
  failedImages.value.add(item._key);
};

const shouldShowImage = (item: AugmentedItem): boolean =>
  !!item.thumbnail && !failedImages.value.has(item._key);
</script>
