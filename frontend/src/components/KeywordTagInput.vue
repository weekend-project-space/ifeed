<template>
  <div class="keyword-field" :class="{ 'keyword-field-error': error }" @click.self="input?.focus()">
    <div class="flex items-baseline justify-between gap-2">
      <label :for="id" class="text-[11px] font-medium text-text-secondary">{{ label }}</label>
      <span class="text-[10px] text-text-muted">{{ modelValue.length }} / {{ maxTerms }}</span>
    </div>
    <div class="mt-1.5 flex flex-wrap items-center gap-1.5" @click.self="input?.focus()">
      <span v-for="(tag, index) in modelValue" :key="tag" class="keyword-tag" :class="{ 'keyword-tag-blocked': blocked }">
        <span class="min-w-0 break-words">{{ tag }}</span>
        <button type="button" class="keyword-remove" :aria-label="tr('common.removeKeyword', { value0: label, value1: tag })" :title="tr('common.removeNamed', { value0: tag })" @click="remove(index)">×</button>
      </span>
      <div class="flex min-w-0 flex-1 items-center gap-1 basis-28">
        <input :id="id" ref="input" v-model="draft" type="text" class="keyword-input" autocomplete="off"
               :placeholder="blocked ? tr('common.addBlockedKeyword') : tr('common.addKeyword')" :aria-invalid="!!error" :aria-describedby="`${id}-status`"
               @input="onInput" @keydown="onKeydown" @blur="commit" @paste="onPaste" @compositionend="onInput" />
        <button type="button" class="keyword-add" :disabled="!draft.trim()" :aria-label="tr('common.addNamed', { value0: label })" :title="tr('common.addNamed', { value0: label })"
                @mousedown.prevent @click="commit">+</button>
      </div>
    </div>
    <p :id="`${id}-status`" class="mt-1 text-[10px]" :class="error ? 'text-red-600 dark:text-red-400' : 'text-text-muted'" :role="error ? 'alert' : undefined">
      {{ error || tr('common.keywordInputHint') }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { computed, ref } from 'vue';

const props = defineProps<{
  id: string;
  label: string;
  modelValue: string[];
  maxTerms: number;
  maxTermLength: number;
  blocked?: boolean;
}>();
const emit = defineEmits<{ 'update:modelValue': [value: string[]] }>();
const input = ref<HTMLInputElement | null>(null);
const draft = ref('');
const errorKind = ref<'' | 'length' | 'count'>('');
const error = computed(() => errorKind.value === 'length'
    ? tr('common.keywordLengthLimit', { value0: props.maxTermLength })
    : errorKind.value === 'count' ? tr('common.keywordCountLimit', { value0: props.maxTerms }) : '');

const commit = () => {
  const terms = [...new Set(draft.value.split(/[，,\r\n]/).map(term => term.trim()).filter(Boolean))];
  if (!terms.length) { draft.value = ''; errorKind.value = ''; return true; }
  if (terms.some(term => term.length > props.maxTermLength)) {
    errorKind.value = 'length';
    return false;
  }
  const next = [...new Set([...props.modelValue, ...terms])];
  if (next.length > props.maxTerms) {
    errorKind.value = 'count';
    return false;
  }
  emit('update:modelValue', next);
  draft.value = '';
  errorKind.value = '';
  return true;
};
const remove = (index: number) => {
  emit('update:modelValue', props.modelValue.filter((_, position) => position !== index));
  errorKind.value = '';
};
const onInput = (event: Event) => {
  errorKind.value = '';
  const value = (event.target as HTMLInputElement).value;
  draft.value = value;
  if (!(event as InputEvent).isComposing && /[，,\r\n]/.test(value)) commit();
};
const onKeydown = (event: KeyboardEvent) => {
  if (event.isComposing || event.keyCode === 229) return;
  if (event.key === 'Enter') { event.preventDefault(); commit(); }
};
const onPaste = (event: ClipboardEvent) => {
  const text = event.clipboardData?.getData('text');
  if (!text || !/[，,\r\n]/.test(text)) return;
  event.preventDefault();
  const element = event.target as HTMLInputElement;
  const start = element.selectionStart ?? draft.value.length;
  const end = element.selectionEnd ?? start;
  draft.value = draft.value.slice(0, start) + text + draft.value.slice(end);
  commit();
};
defineExpose({ commit });
</script>

<style scoped>
@reference "../styles/tailwind.css";

.keyword-field {
  @apply min-w-0 rounded-lg border border-outline/50 bg-surface px-3 py-2 transition-colors focus-within:border-text;
}
.keyword-field-error { @apply border-red-500; }
.keyword-tag {
  @apply inline-flex max-w-full items-center gap-1 rounded-full bg-surface-container/70 px-2 py-0.5 text-xs font-medium text-text;
}
.keyword-tag-blocked { @apply border border-red-500/20 bg-red-500/5 text-red-600 dark:text-red-400; }
.keyword-remove {
  @apply flex h-4 w-4 shrink-0 items-center justify-center rounded-full text-sm leading-none opacity-60 transition hover:bg-outline/30 hover:opacity-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary;
}
.keyword-input {
  @apply block min-w-0 w-full border-0 bg-transparent p-0 text-xs leading-6 text-text shadow-none outline-none ring-0 placeholder:text-text-muted focus:border-0 focus:shadow-none focus:outline-none focus:ring-0;
}
.keyword-add {
  @apply flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-surface-container/60 text-base text-text transition hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:cursor-default disabled:opacity-30;
}
</style>
