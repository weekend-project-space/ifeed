<template>
  <div v-if="visible && title" class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
       @click.self="handleBackdrop">
    <div class="absolute inset-0 bg-black/45"></div>
    <div class="relative w-full max-w-sm rounded-[28px] bg-surface p-6 shadow-2xl space-y-4">
      <h3 class="text-base font-bold text-text">{{ title }}</h3>

      <div class="text-xs text-text-secondary leading-relaxed sm:text-sm">
        <slot>
          <p v-if="description">{{ description }}</p>
        </slot>
      </div>

      <div class="flex gap-2.5 justify-end pt-2">
        <button @click="handleCancel"
                class="inline-flex h-9 items-center justify-center rounded-full px-4 text-xs font-semibold text-text-secondary hover:bg-surface-container transition active:scale-95">
          {{ cancelText }}
        </button>
        <button @click="handleConfirm" :disabled="confirmDisabled || confirmLoading"
                class="inline-flex h-9 items-center justify-center rounded-full bg-red-600 px-4 text-xs font-semibold text-white shadow-xs transition hover:bg-red-700 disabled:opacity-40 active:scale-95">
          {{ confirmLoading ? (confirmLoadingText || confirmText) : confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { withDefaults, defineProps, defineEmits } from 'vue';

interface Props {
  visible: boolean;
  title: string;
  description?: string;
  confirmText?: string;
  confirmLoadingText?: string;
  cancelText?: string;
  confirmLoading?: boolean;
  confirmDisabled?: boolean;
  closeOnBackdrop?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  confirmText: '确认',
  cancelText: '取消',
  confirmLoadingText: '处理中...',
  confirmLoading: false,
  confirmDisabled: false,
  closeOnBackdrop: true,
});

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'confirm'): void;
  (e: 'cancel'): void;
  (e: 'close'): void;
}>();

const handleCancel = () => {
  emit('cancel');
  emit('update:visible', false);
  emit('close');
};

const handleConfirm = () => {
  emit('confirm');
};

const handleBackdrop = () => {
  if (!props.closeOnBackdrop) return;
  emit('update:visible', false);
  emit('close');
};
</script>