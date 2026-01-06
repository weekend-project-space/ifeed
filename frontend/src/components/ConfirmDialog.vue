<template>
    <div v-if="visible && title" class="fixed inset-0 z-50 flex items-center justify-center p-4"
      @click.self="handleBackdrop">
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
      <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-sm w-full p-6 space-y-4">
        <h3 class="text-lg font-semibold text-text">{{ title }}</h3>

        <div class="text-sm text-text-secondary">
          <slot>
            <p v-if="description">{{ description }}</p>
          </slot>
        </div>

        <div class="flex gap-3 justify-end">
          <button @click="handleCancel"
            class="px-4 py-2 text-sm font-medium text-text-secondary hover:bg-surface-container rounded-lg transition-colors">
            {{ cancelText }}
          </button>
          <button @click="handleConfirm" :disabled="confirmDisabled || confirmLoading"
            class="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors disabled:opacity-50">
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
