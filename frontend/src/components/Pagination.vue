<template>
  <nav
      v-if="hasPreviousPage || hasNextPage"
      class="mt-6 sm:mt-8 flex items-center justify-center gap-2 border-t border-outline/10 pt-4 sm:pt-6"
      :aria-label="tr('common.pagination')"
  >
    <button
        type="button"
        @click="handlePrevPage"
        :disabled="!hasPreviousPage || disabled"
        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
        :aria-label="tr('common.previousPage')"
    >
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="15 18 9 12 15 6" />
      </svg>
    </button>

    <div class="rounded-full bg-surface-container/60 px-4 py-1.5 text-xs font-semibold text-text shadow-2xs">
       {{ currentPage }}
    </div>

    <button
        type="button"
        @click="handleNextPage"
        :disabled="!hasNextPage || disabled"
        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
        :aria-label="tr('common.nextPage')"
    >
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="9 18 15 12 9 6" />
      </svg>
    </button>
  </nav>
</template>

<script setup>
import { translate as tr } from '@/i18n';
defineOptions({ name: 'Pagination' })

const props = defineProps({
  currentPage: {
    type: Number,
    required: true,
    default: 1
  },
  hasPreviousPage: {
    type: Boolean,
    default: false
  },
  hasNextPage: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['prev-page', 'next-page', 'update:currentPage'])

const handlePrevPage = () => {
  if (props.hasPreviousPage && !props.disabled) {
    emit('prev-page')
    emit('update:currentPage', props.currentPage - 1)
  }
}

const handleNextPage = () => {
  if (props.hasNextPage && !props.disabled) {
    emit('next-page')
    emit('update:currentPage', props.currentPage + 1)
  }
}
</script>
