<template>
  <header class="flex flex-wrap items-center justify-between px-2  sm:px-4 mb-3">
    <div>
      <h1 class="text-xl font-semibold text-text" v-text="title"></h1>
      <p class="text-sm text-text-secondary" v-text="subtitle">
      </p>
    </div>
    <div class="flex items-center gap-2">
      <button type="button"
        class="inline-flex items-center gap-1 rounded-xl border border-outline/40 bg-surface-container px-3 py-1.5 text-xs text-text-secondary transition hover:border-primary/50 hover:text-primary"
        @click="onRefresh()" :disabled="loading">
        <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M4 10a6 6 0 0 1 10-4.24M16 10a6 6 0 0 1-10 4.24M4 6V3.5M4 3.5h2.5M4 3.5 6.5 6M16 14v2.5M16 16.5h-2.5M16 16.5 13.5 14" />
        </svg>
        <span>{{ loading ? '刷新中…' : '刷新' }}</span>
      </button>
      <button @click="setView('list')" :aria-pressed="view === 'list'" :class="btnClass(view === 'list')" title="列表视图">
        列表
      </button>
      <button @click="setView('magazine')" :aria-pressed="view === 'magazine'" :class="btnClass(view === 'magazine')"
        title="杂志视图">
        杂志
      </button>
      <button @click="setView('only-title')" :aria-pressed="view === 'only-title'"
        :class="btnClass(view === 'only-title')" title="仅标题视图">
        仅标题
      </button>
    </div>
  </header>

  <!-- Loading skeleton -->
  <section v-if="loading" class="space-y-3 sm:space-y-4 ">
    <!-- 杂志视图骨架 -->
    <div v-if="view === 'magazine'" class="grid gap-3 sm:grid-cols-2 sm:gap-1 xl:grid-cols-3 2xl:grid-cols-4">
      <div v-for="i in 12" :key="`mag-skel-${i}`"
        class="animate-pulse rounded-xl bg-surface-container p-3 transition sm:px-5 sm:py-6">
        <figure class="relative aspect-video w-full overflow-hidden bg-surface rounded-xl">
          <div class="h-full w-full bg-outline/15 rounded"></div>
        </figure>
        <div class="mt-6 space-y-3">
          <div class="h-4 w-3/4 rounded-full bg-outline/15"></div>
          <div class="h-4 w-full rounded-full bg-outline/10"></div>
        </div>
      </div>
    </div>

    <!-- 列表视图骨架 -->
    <div v-else-if="view === 'list'" class="space-y-3">
      <div v-for="i in 10" :key="`list-skel-${i}`" class="flex items-start gap-3 animate-pulse  pb-3">
        <div class="flex-1 space-y-2">
          <div class="h-6 w-1/2 bg-outline/10 rounded"></div>
          <div class="h-4 w-3/4 bg-outline/10 rounded"></div>
          <div class="h-4 w-1/2 bg-outline/15 rounded"></div>
          <div class="h-3 w-1/5 bg-outline/15 rounded"></div>
        </div>
        <div class="w-24 h-24 bg-outline/10 rounded"></div>
      </div>
    </div>

    <!-- 仅标题视图骨架 -->
    <div v-else-if="view === 'only-title'" class="space-y-3">
      <div v-for="i in 15" :key="`title-skel-${i}`"
        class="flex items-center justify-between p-3 animate-pulse rounded-md bg-surface-container/30">
        <div class="h-5 flex-1 bg-outline/15 rounded mr-4"></div>
        <div class="flex items-center gap-3">
          <div class="h-3 w-20 bg-outline/10 rounded"></div>
          <div class="h-3 w-3 bg-outline/10 rounded-full"></div>
          <div class="h-3 w-16 bg-outline/10 rounded"></div>
        </div>
      </div>
    </div>
  </section>

  <!-- 内容区 -->
  <section v-else>
    <!-- 空状态 -->
    <div v-if="!items.length"
      class="flex flex-col items-center justify-center gap-3 rounded-xl border border-outline/30 bg-surface py-16 text-sm text-text-muted">
      <svg class="h-10 w-10 text-primary/40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 6.75h15m-15 4.5h15m-15 4.5h7.5" />
      </svg>
      <span class="max-w-xs text-center leading-relaxed">{{ emptyMessage }}</span>
    </div>

    <!-- 视图切换 -->
    <transition name="fade" mode="out-in">
      <!-- 列表视图 -->
      <div v-if="view === 'list'" key="view-list" class="overflow-hidden">
        <article v-for="item in items" :key="item.id"
          class="flex items-start gap-4 p-4 transition cursor-pointer rounded-lg hover:bg-primary/5"
          @click="onSelect(item.id)">
          <div class="flex-1 min-w-0">
            <h3 class="text-base font-semibold leading-snug text-text line-clamp-2">
              {{ item.title }}
            </h3>
            <p class="mt-1 text-sm text-text-secondary line-clamp-2">
              {{ item.summary }}
            </p>
            <p class="mt-1 text-xs text-text-muted">
              {{ item.feedTitle }} · {{ item.timeAgo }}
            </p>
            <div v-if="item.tags?.length" class="mt-2 flex flex-wrap gap-2">
              <button v-for="tag in item.tags" :key="tag" @click.stop="onSelectTag(tag)"
                class="rounded-full bg-surface px-3 py-1 text-xs text-text-secondary border border-outline/30 hover:border-outline/50">
                #{{ tag }}
              </button>
            </div>
          </div>

          <figure class="flex-shrink-0 w-24 h-24 overflow-hidden rounded-lg bg-outline/10">
            <img v-if="item.thumbnail && !thumbErrorMap[item.id]" :src="item.thumbnail" :alt="item.title"
              class="w-full h-full object-cover transition duration-300" loading="lazy"
              @error="onThumbError(item.id)" />
            <div v-else class="flex items-center justify-center w-full h-full bg-outline/5 text-text-muted text-xs">
              无图
            </div>
          </figure>
        </article>
      </div>

      <!-- 杂志视图（最新卡片 + 深夜模式） -->
      <div v-else-if="view === 'magazine'" key="view-magazine"
        class="grid gap-3 sm:grid-cols-2 sm:gap-1 xl:grid-cols-3 2xl:grid-cols-4">
        <article v-for="item in items" :key="item.id"
          class="group relative flex h-full cursor-pointer flex-col overflow-hidden rounded-xl bg-surface p-3"
          @click="onSelect(item.id)">
          <!-- 扩散 hover 背景 -->
          <span class="pointer-events-none absolute inset-0 origin-center scale-50 rounded h-1/5
                       bg-primary/5 transition-transform duration-100 ease-out
                       group-hover:scale-[1.02] group-hover:h-full"></span>

          <figure class="relative aspect-video w-full overflow-hidden bg-surface rounded-xl">
            <img v-if="item.thumbnail && !thumbErrorMap[item.id]" :src="item.thumbnail" alt="文章缩略图" loading="lazy"
              class="h-full w-full object-cover transition duration-300 group-hover:scale-[1.01]"
              @error="onThumbError(item.id)" />
            <div v-else class="flex h-full w-full items-center justify-center">
              <div class="flex h-full w-full items-center justify-center bg-primary/10">
                <div
                  class="flex items-center gap-2 rounded-md border border-outline/30 bg-surface px-3 py-2 text-xs text-text-muted">
                  <svg class="h-4 w-4 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="1.5">
                    <rect x="3" y="3" width="18" height="14" rx="2" ry="2"></rect>
                    <path d="M3 17l5-5 4 4 3-3 6 6"></path>
                  </svg>
                  缩略图不可用
                </div>
              </div>
            </div>
          </figure>

          <div class="flex flex-1 flex-col gap-3 py-4 sm:gap-4 sm:py-5">
            <header class="flex items-start gap-3 sm:gap-4">
              <div class="min-w-0 flex-1 space-y-1">
                <h3 class="text-base font-semibold leading-tight text-text line-clamp-2">
                  {{ item.title }}
                </h3>
                <p class="text-xs text-text-muted">
                  {{ item.feedTitle }} · {{ item.timeAgo }}
                </p>
              </div>
            </header>

            <p class="text-sm leading-relaxed text-text-secondary line-clamp-3">
              {{ item.summary }}
            </p>

            <footer v-if="item.tags?.length" class="mt-auto flex flex-wrap gap-2 text-xs text-text-muted">
              <button v-for="tag in item.tags" :key="tag" type="button"
                class="rounded-full bg-surface px-3 py-1 font-medium text-text-secondary border border-outline/30 hover:border-outline/50"
                @click.stop="onSelectTag(tag)">
                #{{ tag }}
              </button>
            </footer>
          </div>
        </article>
      </div>

      <!-- 仅标题视图 -->
      <div v-else key="view-only-title" class="space-y-2">
        <article v-for="item in items" :key="item.id"
          class="flex items-center justify-between py-2 px-4 transition cursor-pointer rounded-md hover:bg-primary/5"
          @click="onSelect(item.id)">
          <h3 class="flex-1 text-base font-medium text-text line-clamp-1 pr-4">
            {{ item.title }}
          </h3>
          <div class="flex items-center gap-3 text-xs text-text-muted">
            <span>{{ item.feedTitle }}</span>
            <span>·</span>
            <span>{{ item.timeAgo }}</span>
          </div>
        </article>
      </div>
    </transition>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';

export interface ArticleListItemProps {
  id: string;
  title: string;
  summary: string;
  feedTitle: string;
  timeAgo: string;
  tags?: string[];
  collected?: boolean;
  link?: string;
  thumbnail?: string;
  enclosure?: string;
}

const props = withDefaults(defineProps<{
  title: string,
  subtitle: string,
  items: ArticleListItemProps[];
  loading?: boolean;
  emptyMessage?: string;
}>(), {
  items: () => [],
  loading: false,
  emptyMessage: '暂无文章，添加订阅后即可看到推荐内容。'
});

const emit = defineEmits<{
  select: [id: string];
  refresh: [],
  'toggle-favorite': [id: string];
  'select-tag': [tag: string];
}>();

type ViewMode = 'list' | 'magazine' | 'only-title';
const STORAGE_KEY = 'article_feed_view_mode';
const saved = localStorage.getItem(STORAGE_KEY) as ViewMode | null;
const view = ref<ViewMode>(saved ?? 'list');

function setView(v: ViewMode) {
  view.value = v;
  try {
    localStorage.setItem(STORAGE_KEY, v);
  } catch { }
}

function btnClass(active: boolean) {
  return [
    'px-3 py-1 rounded-xl border text-sm transition ',
    active ? 'bg-primary/5 font-semibold' : 'bg-surface text-text-secondary hover:bg-primary/10 hover:text-primary'
  ].join(' ');
}

const items = computed(() => props.items);
const loading = computed(() => !!props.loading);
const emptyMessage = computed(() => props.emptyMessage);

const thumbErrorMap = reactive<Record<string, boolean>>({});

function onThumbError(id: string) {
  thumbErrorMap[id] = true;
}
function onSelectTag(tag: string) { emit('select-tag', tag); }
function onSelect(id: string) { emit('select', id); }
function onRefresh() { emit('refresh'); }

watch(items, (newItems) => {
  const present = new Set(newItems.map(i => i.id));
  Object.keys(thumbErrorMap).forEach(k => {
    if (!present.has(k)) delete thumbErrorMap[k];
  });
});
</script>
