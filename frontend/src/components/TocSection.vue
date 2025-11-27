<template>
  <nav class="p-4 rounded-lg bg-secondary/5 border border-secondary/10">
    <h2 class="text-xs font-semibold text-secondary/60  uppercase tracking-wide mb-2">
      章节
    </h2>
    <div class="space-y-1">
      <button
          v-for="item in items"
          :key="item.id"
          type="button"
          class="block w-full text-left py-1.5 px-0 text-sm rounded transition-colors truncate "
          :class="activeId === item.id
            ? 'text-secondary font-medium'
            : 'text-secondary/70 hover:text-secondary '"
          :style="{ paddingLeft: getTocPadding(item.level) + 'px' }"
          @click="handleNavigate(item.id)">
        {{ item.text }}
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
import {computed} from "vue";

interface TocItem {
  id: string;
  text: string;
  level: number;
}
interface Props {
  items: TocItem[];
  activeId?: string;
}
const maxToc = computed(()=>Math.min(...props.items.map(a=>a.level)))
interface Emits {
  (e: 'navigate', id: string): void;
}

const props = withDefaults(defineProps<Props>(), {
  activeId: ''
});

const emit = defineEmits<Emits>();

const getTocPadding = (level: number): number => {
  return Math.max(0, level - maxToc) * 12;
};

const handleNavigate = (id: string) => {
  emit('navigate', id);
};
</script>