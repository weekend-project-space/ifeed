<template>
  <div ref="container" class="relative">
    <div :id="id" ref="body" class="summary-content social-text-body"
         :class="expanded ? '' : 'line-clamp-6 sm:line-clamp-8'">
      <p v-for="(paragraph, index) in paragraphs" :key="index" class="whitespace-pre-line">{{ paragraph }}</p>
    </div>
    <div ref="measurement" aria-hidden="true"
         class="summary-content social-text-body pointer-events-none invisible absolute inset-x-0 top-0 select-none line-clamp-6 sm:line-clamp-8">
      <p v-for="(paragraph, index) in paragraphs" :key="index" class="whitespace-pre-line">{{ paragraph }}</p>
    </div>
    <button v-if="overflowing" type="button" :aria-expanded="expanded" :aria-controls="id"
            class="mt-0.5 inline-flex min-h-8 items-center text-xs font-medium text-text-secondary transition-colors hover:text-primary focus-visible:rounded-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
            @click.stop="toggle">
      {{ expanded ? collapseLabel : expandLabel }}
    </button>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';

const props = defineProps<{
  id: string;
  text: string;
  expandLabel: string;
  collapseLabel: string;
}>();

const container = ref<HTMLElement | null>(null);
const body = ref<HTMLElement | null>(null);
const measurement = ref<HTMLElement | null>(null);
const expanded = ref(false);
const overflowing = ref(false);
const paragraphs = computed(() => (props.text || '')
    .replace(/\r\n?/g, '\n').trim().split(/\n\s*\n/)
    .map((paragraph: string) => paragraph.trim()).filter(Boolean));

let mounted = false;
let frame: number | null = null;
let observer: ResizeObserver | null = null;

const measure = () => {
  if (!mounted || frame !== null) return;
  frame = requestAnimationFrame(() => {
    frame = null;
    if (!measurement.value) return;
    overflowing.value = measurement.value.scrollHeight > measurement.value.clientHeight + 1;
  });
};

const toggle = async () => {
  const previousScroll = window.scrollY;
  const anchor = container.value?.closest('article') ?? container.value;
  const offset = anchor ? Number.parseFloat(getComputedStyle(anchor).scrollMarginTop) || 0 : 0;
  const restoreHeader = expanded.value && anchor && anchor.getBoundingClientRect().top < offset;
  expanded.value = !expanded.value;
  await nextTick();
  if (!mounted) return;
  body.value?.getBoundingClientRect();
  if (restoreHeader) anchor.scrollIntoView({block: 'start', behavior: 'auto'});
  else window.scrollTo({top: previousScroll, behavior: 'auto'});
};

watch(() => props.text, measure, {flush: 'post'});

onMounted(() => {
  mounted = true;
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(measure);
    if (measurement.value) observer.observe(measurement.value);
  }
  window.addEventListener('resize', measure);
  void document.fonts?.ready.then(measure);
  measure();
});

onBeforeUnmount(() => {
  mounted = false;
  observer?.disconnect();
  window.removeEventListener('resize', measure);
  if (frame !== null) cancelAnimationFrame(frame);
});
</script>

<style scoped>
.summary-content > p + p {
  margin-top: 10px;
}
</style>
