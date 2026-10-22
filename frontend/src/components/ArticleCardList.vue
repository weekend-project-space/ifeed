<template>
  <!-- 骨架屏 -->
  <div v-if="loading" role="status" aria-live="polite" :class="sizes.skeletonOuter">
    <span class="sr-only">正在加载列表</span>
    <div v-for="i in 3" :key="i" :class="[sizes.skeletonRow, 'animate-pulse']">
      <div :class="['bg-surface-container rounded-2xl shrink-0', sizes.thumbnail]"></div>
      <div class="flex-1 space-y-2 py-1 sm:space-y-3 sm:py-2">
        <div :class="['bg-surface-container rounded-xl w-3/4', sizes.skeletonLine1]"></div>
        <div :class="['bg-surface-container rounded-lg w-1/2', sizes.skeletonLine2]"></div>
        <div v-if="!compact" class="h-3.5 sm:h-4 bg-surface-container/60 rounded-xl w-full"></div>
      </div>
    </div>
  </div>

  <!-- 文章列表：MD3 柔和表面卡片 -->
  <div v-else-if="items.length > 0" :class="sizes.listGap">
    <article v-for="item in itemsWithMeta" :key="item._key">
      <div :class="['group relative rounded-3xl transition-colors hover:bg-surface-container/60', sizes.cardFlex, sizes.cardPad]">
        <router-link :to="`/articles/${item._key}`" :class="sizes.link">
          <!-- 缩略图 -->
          <div :class="['relative shrink-0 overflow-hidden rounded-2xl bg-surface-container shadow-2xs', sizes.thumbnail]">
            <img
                v-if="shouldShowImage(item)"
                :src="item.thumbnail"
                :alt="item.title || '文章缩略图'"
                class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                loading="lazy"
                referrerpolicy="no-referrer"
                @error="handleImageError(item)"
            />
            <div v-else class="flex h-full w-full items-center justify-center text-text-muted">
              <slot name="empty-thumbnail" :item="item">
                <svg :class="sizes.thumbIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="3"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
              </slot>
            </div>
          </div>

          <!-- 内容主体 -->
          <div class="flex-1 min-w-0 flex flex-col justify-center py-0.5">
            <h3 :class="['font-semibold text-text line-clamp-2 leading-snug group-hover:text-primary transition-colors', sizes.title]">
              {{ item.title || '未命名文章' }}
            </h3>
            <div :class="['flex items-center gap-1.5 text-text-secondary', sizes.meta]">
              <span v-if="item.feedTitle" class="truncate font-medium">{{ item.feedTitle }}</span>
              <span v-if="item.feedTitle && item._meta" class="shrink-0 text-outline">·</span>
              <span v-if="item._meta" class="shrink-0">{{ item._meta }}</span>
            </div>
            <p v-if="item.summary && !compact" class="text-xs text-text-secondary line-clamp-2 leading-relaxed sm:text-sm">
              {{ item.summary }}
            </p>
          </div>
        </router-link>

        <!-- 移动端非紧凑模式的操作按钮浮层 -->
        <div v-if="showAction && !compact" class="sm:hidden absolute top-2 right-2 z-10">
          <button
              @click.stop.prevent="onActionClick(item, $event)"
              :aria-expanded="activeDropdown === item._key"
              class="flex h-8 w-8 items-center justify-center rounded-full bg-surface/80 shadow-xs backdrop-blur-sm transition-all relative z-20 text-text"
              :aria-label="actionLabel"
          >
            <slot name="action-icon" :item="item">
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="19" r="1.5"/>
              </svg>
            </slot>
          </button>
          <div v-if="hasDropdownSlot && activeDropdown === item._key" @click.stop.prevent class="absolute right-0 top-full mt-1 w-44 overflow-hidden rounded-2xl bg-surface py-1 shadow-xl border border-outline/10 z-50">
            <slot name="action-dropdown" :item="item" :close="closeDropdown"></slot>
          </div>
        </div>

        <!-- 桌面端操作按钮 -->
        <div v-if="showAction" :class="['relative flex self-center', sizes.actionWrapper]">
          <button
              @click.stop.prevent="onActionClick(item, $event)"
              :aria-expanded="activeDropdown === item._key"
              :class="['rounded-full transition-all relative z-20 hover:bg-surface-container', sizes.actionBtn]"
              :aria-label="actionLabel"
          >
            <slot name="action-icon" :item="item">
              <svg :class="['text-text-secondary', sizes.actionIcon]" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="19" r="1.5"/>
              </svg>
            </slot>
          </button>
          <div v-if="hasDropdownSlot && activeDropdown === item._key" @click.stop.prevent class="absolute right-0 top-full mt-1 w-44 overflow-hidden rounded-2xl bg-surface py-1 shadow-xl border border-outline/10 z-50">
            <slot name="action-dropdown" :item="item" :close="closeDropdown"></slot>
          </div>
        </div>
      </div>
    </article>
  </div>

  <!-- 空状态 -->
  <div v-else :class="['flex flex-col items-center justify-center text-center', sizes.emptyPad]">
    <div class="mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-surface-container/60">
      <svg class="h-6 w-6 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
        <rect x="3" y="3" width="18" height="18" rx="3"/>
        <path d="M3 9h18"/>
        <path d="M9 21V9"/>
      </svg>
    </div>
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
  likedAt?: string;
  folderId?: string | null;
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
  metaField?: 'collectedAt' | 'likedAt' | 'readAt' | 'timeAgo' | 'relativeTime' | 'custom';
  metaPrefix?: string;
  keyField?: keyof ArticleItem;
  compact?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showAction: false,
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
  const timestamp = item[props.metaField as 'collectedAt' | 'likedAt' | 'readAt'];
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

const sizes = computed(() => {
  const c = props.compact;
  return {
    skeletonOuter:  c ? 'space-y-2'                                 : 'space-y-3',
    skeletonRow:    c ? 'flex items-start gap-2.5'                  : 'flex flex-col sm:flex-row gap-3 sm:gap-4',
    skeletonLine1:  c ? 'h-3.5'                                     : 'h-4 sm:h-5',
    skeletonLine2:  c ? 'h-2.5'                                     : 'h-3 sm:h-4',
    listGap:        c ? 'space-y-2'                                 : 'space-y-2.5',
    cardFlex:       c ? 'flex items-center'                         : 'sm:flex sm:items-start',
    // 增加 compact 模式下的纵向和横向留白，使其更显大气和现代
    cardPad:        c ? 'p-2.5 sm:px-3.5 sm:py-3'                   : 'p-3 sm:p-3.5',
    link:           c ? 'flex-1 flex flex-row items-center gap-3.5' : 'flex-1 flex flex-col sm:flex-row gap-3 sm:gap-4',
    // 稍微放大 compact 模式下的缩略图（从 w-24 h-16 改为 w-28 h-20 左右或等比），让视觉质感更好
    thumbnail:      c ? 'w-24 h-18 sm:w-28 sm:h-20 rounded-2xl'     : 'w-full sm:w-48 h-40 sm:h-28',
    thumbIcon:      c ? 'w-5 h-5'                                   : 'w-7 h-7 sm:w-8 sm:h-8',
    title:          c ? 'text-xs sm:text-sm mb-1'                   : 'text-sm sm:text-base mb-1.5',
    meta:           c ? 'text-[11px] sm:text-xs'                    : 'text-xs mb-1.5',
    actionWrapper:  c ? 'ml-2'                                      : 'hidden sm:flex ml-2',
    actionBtn:      c ? 'p-2'                                       : 'p-2 opacity-0 group-hover:opacity-100',
    actionIcon:     c ? 'w-4 h-4'                                   : 'w-5 h-5',
    emptyPad:       c ? 'py-6'                                      : 'py-16',
    emptyIcon:      c ? 'w-6 h-6'                                   : 'w-8 h-8',
    emptyText:      c ? 'text-xs'                                   : 'text-sm',
  };
});

const handleImageError = (item: AugmentedItem) => {
  failedImages.value.add(item._key);
};

const shouldShowImage = (item: AugmentedItem): boolean =>
    !!item.thumbnail && !failedImages.value.has(item._key);
</script>