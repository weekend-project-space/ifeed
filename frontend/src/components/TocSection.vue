<template>
  <nav class="p-4 bg-secondary/5 rounded-lg border border-secondary/10">
    <h2 class="text-xs font-semibold text-secondary/60 uppercase tracking-wide mb-2">
      目录
    </h2>
    <div class="space-y-1">
      <button
          v-for="item in items"
          :key="item.id"
          type="button"
          class="block w-full text-left py-1.5 px-2 text-sm rounded transition-colors truncate "
          :class="activeId === item.id
            ? 'text-secondary bg-secondary/10 font-medium'
            : 'text-secondary/70 hover:text-secondary hover:bg-secondary/5'"
          :style="{ paddingLeft: getTocPadding(item.level) + 'px' }"
          @click="handleNavigate(item.id)">
        {{ item.text }}
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
interface TocItem {
  id: string;
  text: string;
  level: number;
}

interface Props {
  items: TocItem[];
  activeId?: string;
}

interface Emits {
  (e: 'navigate', id: string): void;
}

const props = withDefaults(defineProps<Props>(), {
  activeId: ''
});

const emit = defineEmits<Emits>();

const getTocPadding = (level: number): number => {
  return 0 + Math.max(0, level - 1) * 12;
};

const handleNavigate = (id: string) => {
  emit('navigate', id);
};
</script>