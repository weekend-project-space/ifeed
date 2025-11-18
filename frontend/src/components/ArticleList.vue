<template>
  <div class="space-y-6">
    <!-- Header -->
    <header class="flex flex-wrap items-center justify-between gap-4 px-4">
      <div>
        <h1 class="text-xl font-normal text-gray-900 dark:text-gray-100" v-text="title"></h1>
        <p class="text-sm text-gray-600 dark:text-gray-400" v-text="subtitle"></p>
      </div>
      <div class="flex items-center gap-2">
        <button
            type="button"
            class="px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-800 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors disabled:opacity-50"
            @click="emit('refresh')"
            :disabled="loading">
          {{ loading ? '刷新中…' : '刷新' }}
        </button>
        <button
            @click="setView('magazine')"
            :class="btnClass(view === 'magazine')"
            title="杂志视图">
          杂志
        </button>
        <button
            @click="setView('card')"
            :class="btnClass(view === 'card')"
            title="卡片视图">
          卡片
        </button>
        <button
            @click="setView('only-title')"
            :class="btnClass(view === 'only-title')"
            title="仅标题视图">
          仅标题
        </button>
      </div>
    </header>

    <!-- Loading skeleton -->
    <section v-if="loading" class="space-y-4">
      <!-- 卡片视图骨架 -->
      <div v-if="view === 'card'" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <div
            v-for="i in 12"
            :key="`card-skel-${i}`"
            class="animate-pulse space-y-4 p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
          <div class="aspect-video w-full bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
          <div class="space-y-2">
            <div class="h-4 w-3/4 bg-gray-200 dark:bg-gray-700 rounded"></div>
            <div class="h-4 w-full bg-gray-200 dark:bg-gray-700 rounded"></div>
          </div>
        </div>
      </div>

      <!-- 杂志视图骨架 -->
      <div v-else-if="view === 'magazine'" class="space-y-3">
        <div
            v-for="i in 10"
            :key="`mag-skel-${i}`"
            class="flex items-start gap-4 animate-pulse p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
          <div class="flex-1 space-y-2">
            <div class="h-5 w-2/3 bg-gray-200 dark:bg-gray-700 rounded"></div>
            <div class="h-4 w-full bg-gray-200 dark:bg-gray-700 rounded"></div>
            <div class="h-3 w-1/3 bg-gray-200 dark:bg-gray-700 rounded"></div>
          </div>
          <div class="w-28 h-28 bg-gray-200 dark:bg-gray-700 rounded-lg flex-shrink-0"></div>
        </div>
      </div>

      <!-- 仅标题视图骨架 -->
      <div v-else-if="view === 'only-title'" class="space-y-2">
        <div
            v-for="i in 15"
            :key="`title-skel-${i}`"
            class="flex items-center justify-between p-3 animate-pulse bg-gray-50 dark:bg-gray-800/50 rounded-lg">
          <div class="h-4 flex-1 bg-gray-200 dark:bg-gray-700 rounded mr-4"></div>
          <div class="h-3 w-24 bg-gray-200 dark:bg-gray-700 rounded"></div>
        </div>
      </div>
    </section>

    <!-- Content -->
    <section v-else>
      <!-- Empty state -->
      <div
          v-if="!items.length"
          class="flex flex-col items-center justify-center gap-3 py-16 text-center">
        <svg class="h-12 w-12 text-gray-400 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <span class="text-sm text-gray-600 dark:text-gray-400 max-w-xs">{{ emptyMessage }}</span>
      </div>

      <!-- View switcher -->
      <transition name="fade" mode="out-in">
        <!-- Magazine view -->
        <div v-if="view === 'magazine'" key="view-magazine" class="space-y-2">
          <article
              v-for="item in items"
              :key="item.id"
              class="flex items-start gap-4 p-4 transition cursor-pointer rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800/50"
              @click="emit('select', item.id)">
            <div class="flex-1 min-w-0 space-y-2">
              <h3 class="text-base font-medium text-gray-900 dark:text-gray-100 line-clamp-2">
                {{ item.title }}
              </h3>
              <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
                {{ item.summary }}
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-500">
                {{ item.feedTitle }} · {{ item.timeAgo }}
              </p>
              <div v-if="item.tags?.length" class="flex flex-wrap gap-2 pt-1">
                <button
                    v-for="tag in item.tags"
                    :key="tag"
                    @click.stop="emit('select-tag', tag)"
                    class="px-2 py-1 text-xs font-medium text-secondary-700 dark:text-secondary-300 bg-secondary-50 dark:bg-secondary-900/30 rounded-full hover:bg-secondary-100 dark:hover:bg-secondary-900/50 transition-colors">
                  {{ tag }}
                </button>
              </div>
            </div>

            <figure class="flex-shrink-0 w-28 h-28 overflow-hidden rounded-lg bg-gray-100 dark:bg-gray-800">
              <img
                  v-if="item.thumbnail && !thumbErrorMap[item.id]"
                  :src="item.thumbnail"
                  :alt="item.title"
                  class="w-full h-full object-cover"
                  loading="lazy"
                  @error="thumbErrorMap[item.id] = true" />
              <div
                  v-else
                  class="flex items-center justify-center w-full h-full text-gray-400 dark:text-gray-600 text-xs">
                无图
              </div>
            </figure>
          </article>
        </div>

        <!-- Card view -->
        <div
            v-else-if="view === 'card'"
            key="view-card"
            class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <article
              v-for="item in items"
              :key="item.id"
              class="group relative flex h-full cursor-pointer flex-col overflow-hidden rounded-xl bg-white dark:bg-gray-800 p-3"
              @click="emit('select', item.id)">

            <!-- Hover background effect -->
            <span class="pointer-events-none absolute inset-0 origin-center scale-50 rounded-xl h-1/5 bg-primary/5 transition-transform duration-200 ease-out group-hover:scale-[1.02] group-hover:h-full"></span>

            <figure class="relative aspect-video w-full overflow-hidden bg-gray-100 dark:bg-gray-800 rounded-xl">
              <img
                  v-if="item.thumbnail && !thumbErrorMap[item.id]"
                  :src="item.thumbnail"
                  alt="文章缩略图"
                  loading="lazy"
                  class="h-full w-full object-cover transition duration-300 group-hover:scale-[1.02]"
                  @error="thumbErrorMap[item.id] = true" />
              <div
                  v-else
                  class="flex h-full w-full items-center justify-center">
                <div class="flex h-full w-full items-center justify-center bg-primary/10">
                  <div class="flex items-center gap-2 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-xs text-gray-500 dark:text-gray-400">
                    <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
                      <rect x="3" y="3" width="18" height="14" rx="2" ry="2"></rect>
                      <path d="M3 17l5-5 4 4 3-3 6 6"></path>
                    </svg>
                    缩略图不可用
                  </div>
                </div>
              </div>
            </figure>

            <div class="relative flex flex-1 flex-col gap-3 py-4">
              <header class="flex items-start gap-3">
                <div class="min-w-0 flex-1 space-y-2">
                  <h3 class="text-base font-normal leading-tight text-gray-900 dark:text-gray-100 line-clamp-2">
                    {{ item.title }}
                  </h3>
                  <p class="text-xs text-gray-500 dark:text-gray-500">
                    {{ item.feedTitle }} · {{ item.timeAgo }}
                  </p>
                </div>
              </header>

              <p class="text-sm leading-relaxed text-gray-600 dark:text-gray-400 line-clamp-2">
                {{ item.summary }}
              </p>

              <footer v-if="item.tags?.length" class="flex flex-wrap gap-3 mt-auto text-xs text-gray-500 dark:text-gray-500">
                <button
                    v-for="tag in item.tags"
                    :key="tag"
                    type="button"
                    class="hover:text-primary transition-colors"
                    @click.stop="emit('select-tag', tag)">
                  #{{ tag }}
                </button>
              </footer>
            </div>
          </article>
        </div>

        <!-- Title only view -->
        <div v-else key="view-only-title" class="space-y-1">
          <article
              v-for="item in items"
              :key="item.id"
              class="flex items-center gap-4 px-4 py-3 transition cursor-pointer rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800/50"
              @click="emit('select', item.id)">

            <!-- Left: Title + Summary -->
            <div class="flex-1 min-w-0 flex items-center gap-4">
              <h3 class="flex-shrink-0 text-sm font-medium text-gray-900 dark:text-gray-100 whitespace-nowrap">
                {{ item.title }}
              </h3>
              <p
                  v-if="item.summary"
                  class="flex-1 text-sm text-gray-600 dark:text-gray-400 line-clamp-1 min-w-0">
                {{ item.summary }}
              </p>
            </div>

            <!-- Right: Source · Time -->
            <div class="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-500 flex-shrink-0 whitespace-nowrap">
              <span class="max-w-32 truncate">{{ item.feedTitle }}</span>
              <span>·</span>
              <span>{{ item.timeAgo }}</span>
            </div>
          </article>
        </div>
      </transition>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';

export interface ArticleListItemProps {
  id: string;
  title: string;
  summary: string;
  feedTitle: string;
  timeAgo: string;
  tags?: string[];
  thumbnail?: string;
}

const props = withDefaults(defineProps<{
  title: string;
  subtitle: string;
  items: ArticleListItemProps[];
  loading?: boolean;
  emptyMessage?: string;
}>(), {
  loading: false,
  emptyMessage: '暂无文章，添加订阅后即可看到推荐内容。'
});

const emit = defineEmits<{
  select: [id: string];
  refresh: [];
  'select-tag': [tag: string];
}>();

type ViewMode = 'magazine' | 'card' | 'only-title';
const STORAGE_KEY = 'article_feed_view_mode';
const saved = localStorage.getItem(STORAGE_KEY) as ViewMode | null;
const view = ref<ViewMode>(saved ?? 'magazine');

function setView(v: ViewMode) {
  view.value = v;
  try {
    localStorage.setItem(STORAGE_KEY, v);
  } catch { }
}

function btnClass(active: boolean) {
  return [
    'px-3 py-1 text-sm font-medium rounded-lg transition-colors whitespace-nowrap',
    active
        ? 'bg-secondary text-secondary-foreground' : 'bg-secondary/5 text-secondary hover:bg-secondary/20'
  ].join(' ');
}

const thumbErrorMap = reactive<Record<string, boolean>>({});

watch(() => props.items, (newItems) => {
  const present = new Set(newItems.map(i => i.id));
  Object.keys(thumbErrorMap).forEach(k => {
    if (!present.has(k)) delete thumbErrorMap[k];
  });
});
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>