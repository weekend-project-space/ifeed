<template>
  <Teleport to="body">
    <dialog ref="dialog" :aria-label="title"
      class="app-dialog bg-surface p-0 text-text shadow-2xl"
      @cancel.prevent="close" @click="onBackdrop">
      <div class="p-5">
        <header class="mb-4 flex items-center justify-between gap-3">
          <h2 class="text-lg font-semibold">{{ title }}</h2>
          <button type="button" :aria-label="tr('common.closeDialog')" :disabled="busy"
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text disabled:opacity-50"
            @click="close">
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path stroke-linecap="round" d="M6 6l12 12M18 6 6 18" />
            </svg>
          </button>
        </header>
        <slot />
      </div>
    </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps<{ title: string; busy?: boolean }>();
const emit = defineEmits<{ close: [] }>();
const dialog = ref<HTMLDialogElement | null>(null);
const close = () => { if (!props.busy) emit('close'); };
const onBackdrop = (event: MouseEvent) => { if (event.target === dialog.value) close(); };
onMounted(() => dialog.value?.showModal());
onBeforeUnmount(() => dialog.value?.close());
</script>

<style scoped>
@reference "../styles/tailwind.css";

.app-dialog {
  position: fixed;
  top: 50%;
  left: 50%;
  width: min(28rem, calc(100vw - 2rem));
  max-width: none;
  max-height: calc(100dvh - 2rem);
  margin: 0;
  overflow-y: auto;
  border: 0;
  border-radius: 1rem;
  background: rgb(var(--md-surface));
  transform: translate(-50%, -50%);
}

.app-dialog::backdrop {
  background: rgb(0 0 0 / 0.5);
}
</style>
