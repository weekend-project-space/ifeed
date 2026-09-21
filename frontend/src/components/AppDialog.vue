<template>
  <Teleport to="body">
    <dialog ref="dialog" :aria-label="title" class="w-[calc(100%-2rem)] max-w-md rounded-2xl bg-white p-0 text-text shadow-xl backdrop:bg-black/50 dark:bg-gray-900"
      @cancel.prevent="close" @click="onBackdrop">
      <div class="p-5">
        <header class="mb-4 flex items-center justify-between gap-3">
          <h2 class="text-lg font-semibold">{{ title }}</h2>
          <button type="button" aria-label="关闭弹窗" :disabled="busy" class="rounded-full px-3 py-1 hover:bg-surface-container disabled:opacity-50" @click="close">✕</button>
        </header>
        <slot />
      </div>
    </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps<{ title: string; busy?: boolean }>();
const emit = defineEmits<{ close: [] }>();
const dialog = ref<HTMLDialogElement | null>(null);
const close = () => { if (!props.busy) emit('close'); };
const onBackdrop = (event: MouseEvent) => { if (event.target === dialog.value) close(); };
onMounted(() => dialog.value?.showModal());
onBeforeUnmount(() => dialog.value?.close());
</script>
