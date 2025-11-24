<template>
  <section class="p-4 bg-secondary/5 rounded-lg border border-secondary/10">
    <h2 class="text-xs font-semibold text-secondary/60 uppercase tracking-wide mb-2">
      {{ title }}
    </h2>
    <div>
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
interface Props {
  title: string;
}

defineProps<Props>();
</script>