<template>
  <nav
      class="flex items-center justify-center gap-2 mt-6 sm:mt-8 pt-4 sm:pt-6 border-t border-outline/10"
      aria-label="分页导航"
  >
    <button
        @click="handlePrevPage"
        :disabled="!hasPreviousPage || disabled"
        class="p-2 hover:bg-surface-container rounded-full disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
        aria-label="上一页"
    >
      <svg class="w-5 h-5 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="15 18 9 12 15 6"/>
      </svg>
    </button>

    <div class="px-3 sm:px-4 py-1 text-xs sm:text-sm text-text-secondary">
      第 {{ currentPage }} 页
    </div>

    <button
        @click="handleNextPage"
        :disabled="!hasNextPage || disabled"
        class="p-2 hover:bg-surface-container rounded-full disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
        aria-label="下一页"
    >
      <svg class="w-5 h-5 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="9 18 15 12 9 6"/>
      </svg>
    </button>
  </nav>
</template>

<script setup>

const props = defineProps({
  // 当前页码
  currentPage: {
    type: Number,
    required: true,
    default: 1
  },
  // 是否有上一页
  hasPreviousPage: {
    type: Boolean,
    default: false
  },
  // 是否有下一页
  hasNextPage: {
    type: Boolean,
    default: false
  },
  // 是否禁用（加载中时）
  disabled: {
    type: Boolean,
    default: false
  },
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

<style scoped>
</style>