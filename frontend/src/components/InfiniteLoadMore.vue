<template>
  <div ref="sentinel" class="mt-8 flex min-h-12 flex-col items-center justify-center gap-2.5 py-4 text-center" aria-live="polite">
    <!-- 加载中 -->
    <div v-if="loading" class="flex items-center gap-2 text-xs text-text-secondary">
      <svg class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <path d="M21 12a9 9 0 1 1-6.219-8.56" />
      </svg>
      <span>{{ tr('common.loadingMore') }}</span>
    </div>

    <!-- 加载失败 -->
    <template v-else-if="failed || error">
      <p class="text-xs text-red-600 dark:text-red-400">{{ tr('common.loadFailed') }}</p>
      <button
          type="button"
          :disabled="fetching"
          class="inline-flex h-8 items-center justify-center rounded-full bg-surface-container px-4 text-xs font-semibold text-text shadow-xs transition hover:bg-surface-container-high active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
          @click="loadMore"
      >
        {{ tr('common.retryLoading') }}
      </button>
    </template>

    <!-- 手动触发更多 -->
    <button
        v-else-if="hasNextPage"
        type="button"
        :disabled="fetching"
        class="inline-flex h-8.5 items-center justify-center rounded-full bg-surface-container/60 px-5 text-xs font-semibold text-text-secondary shadow-2xs transition hover:bg-surface-container hover:text-text active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
        @click="loadMore"
    >
      {{ tr('common.loadMoreText') }}
    </button>

    <!-- 全部加载完毕 -->
    <p v-else class="text-xs text-text-secondary/60">
      {{ tr('common.allLoaded') }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { useInfiniteScroll } from '@/composables/infiniteScroll';

defineOptions({ name: 'InfiniteLoadMore' });

const props = defineProps<{
  hasNextPage: boolean;
  loading: boolean;
  fetching: boolean;
  failed: boolean;
  blocked?: boolean;
  fetchMore: () => Promise<unknown>;
}>();

const { sentinel, loadMore, error } = useInfiniteScroll({
  hasNextPage: () => props.hasNextPage,
  isLoading: () => props.fetching,
  hasError: () => props.failed || Boolean(props.blocked),
  onLoadMore: () => props.fetchMore(),
});
</script>