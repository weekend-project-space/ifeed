<template>
  <div
      class="flex items-center justify-center gap-2 overflow-hidden text-xs text-text-secondary transition-[height] duration-200 ease-out motion-reduce:transition-none lg:hidden"
      :style="{ height: `${height}px`, transition: pulling ? 'none' : undefined }"
      :aria-hidden="height === 0"
      role="status"
      aria-live="polite"
  >
    <svg
        class="h-4 w-4 shrink-0 transition-transform motion-reduce:transition-none"
        :class="{ 'animate-spin motion-reduce:animate-none': refreshing, 'rotate-180': ready && !refreshing }"
        viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"
    >
      <path v-if="refreshing" stroke-linecap="round" d="M20 12a8 8 0 1 1-8-8" />
      <path v-else stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m-6-6 6 6 6-6" />
    </svg>
    <span>{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';

const props = defineProps<{height: number; pulling: boolean; ready: boolean; refreshing: boolean}>();
const {t} = useI18n();
const label = computed(() => t(props.refreshing
    ? 'common.refreshing'
    : props.ready ? 'common.releaseToRefresh' : 'common.pullToRefresh'));
</script>
