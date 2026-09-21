<template>
  <div ref="sentinel" class="mt-6 flex min-h-12 flex-col items-center gap-3 py-4" aria-live="polite">
    <p v-if="loading" class="text-sm text-text-secondary">正在加载更多…</p>
    <template v-else-if="failed || error">
      <p class="text-sm text-red-700">加载更多失败，请重试。</p>
      <button :disabled="fetching" class="rounded-full bg-secondary px-4 py-2 text-sm text-white disabled:opacity-50" @click="loadMore">重试</button>
    </template>
    <button v-else-if="hasNextPage" :disabled="fetching" class="rounded-full px-4 py-2 text-sm text-text-secondary hover:bg-surface-container disabled:opacity-50" @click="loadMore">加载更多</button>
    <p v-else class="text-sm text-text-muted">已加载全部</p>
  </div>
</template>

<script setup lang="ts">
import { useInfiniteScroll } from '@/composables/infiniteScroll';

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
