<template>
  <div class="space-y-3" :class="{'max-w-screen-lg mx-auto':view === 'magazine'}">
    <slot name="header"></slot>
    <!-- Header -->
    <header class="flex flex-wrap items-center justify-between gap-4 px-4">
      <div class="flex items-center gap-3">
        <template v-if="title">
          <h1 class="text-xl font-normal text-gray-900 dark:text-gray-100">
            {{ title }}
            <small v-text="subtitle"> </small>
          </h1>
          <button
              type="button"
              class="p-2 hover:bg-surface-container rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              @click="emit('refresh')"
              :disabled="loading"
              aria-label="刷新列表"
              title="刷新">
            <svg
                xmlns="http://www.w3.org/2000/svg"
                class="w-5 h-5 text-text-secondary transition-transform"
                :class="{ 'animate-spin': loading }"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
            >
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
            </svg>
          </button>
        </template>
      </div>
      <div class="flex items-center gap-2">
        <slot name="action"></slot>
        <button
            @click="setView('magazine')"
            :class="btnClass(view === 'magazine')"
            title="杂志视图"
            aria-label="杂志视图">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'magazine' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h8m-8 4h5m-5 5h8m-8 4h5"/>
            <rect x="15" y="4" width="5" height="5" rx="1"/>
            <rect x="15" y="14" width="5" height="5" rx="1"/>
          </svg>
        </button>
        <button
            @click="setView('card')"
            :class="btnClass(view === 'card')"
            title="卡片视图"
            aria-label="卡片视图">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'card' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
          </svg>
        </button>
        <button
            @click="setView('only-title')"
            :class="btnClass(view === 'only-title')"
            title="仅标题视图"
            aria-label="仅标题视图">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'only-title' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 8h16M4 16h16"/>
          </svg>
        </button>
      </div>
    </header>

    <!-- Loading skeleton -->
    <section v-if="loading" class="space-y-4">
      <div v-if="view === 'card'" class="grid gap-0 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <div v-for="i in 12" :key="`card-skel-${i}`" class="animate-pulse space-y-4 p-4">
          <div class="aspect-video w-full bg-surface-container rounded-lg"></div>
          <div class="space-y-2">
            <div class="h-4 w-3/4 bg-surface-container rounded"></div>
            <div class="h-3 w-1/4 bg-surface-container rounded"></div>
            <div class="h-4 w-full bg-surface-container rounded"></div>
            <div class="h-4 w-3/4 bg-surface-container rounded"></div>
          </div>
        </div>
      </div>

      <div v-else-if="view === 'magazine'" class="space-y-3">
        <div v-for="i in 10" :key="`mag-skel-${i}`" class="flex items-start gap-3 md:gap-6 animate-pulse p-4">
          <div class="flex-1 space-y-2">
            <div class="h-5 w-2/3 bg-surface-container rounded"></div>
            <div class="h-3 w-1/3 bg-surface-container rounded"></div>
            <div class="hidden md:block h-4 w-full bg-surface-container rounded"></div>
            <div class="hidden md:block h-4 w-3/4 bg-surface-container rounded"></div>
          </div>
          <div
              class="w-20 h-20 md:w-48 md:h-32 lg:w-56 lg:h-36 bg-surface-container rounded-lg md:rounded-xl flex-shrink-0"></div>
        </div>
      </div>

      <div v-else-if="view === 'only-title'">
        <div v-for="i in 15" :key="`title-skel-${i}`" class="flex items-center gap-3 px-4 py-2.5 animate-pulse">
          <div class="h-3 w-20 md:w-24 bg-surface-container rounded flex-shrink-0"></div>
          <div class="h-4 flex-1 bg-surface-container rounded"></div>
          <div class="h-3 w-12 bg-surface-container rounded flex-shrink-0"></div>
        </div>
      </div>
    </section>

    <!-- Content -->
    <section v-else>
      <!-- Empty state -->
      <div v-if="!items.length" class="flex flex-col items-center justify-center gap-3 py-16 text-center">
        <slot name="empty">
          <svg class="h-12 w-12 text-gray-400 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          <span class="text-sm text-gray-600 dark:text-gray-400 max-w-xs">{{ emptyMessage }}</span>
        </slot>
      </div>

      <transition name="fade" mode="out-in">
        <!-- Magazine view -->
        <div v-if="view === 'magazine'" key="view-magazine" class="space-y-4">
          <router-link
              v-for="item in items"
              :key="item.id"
              :to="{ name: 'article', params: { id: item.id } }"
              class="block group"
          >
            <article
                class="flex flex-row items-start gap-3 md:gap-6
             p-4 md:p-5 rounded-2xl transition-all duration-300 cursor-pointer
             active:bg-surface-container/60  hover:bg-surface-container/60
             focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              <!-- 文字内容 -->
              <div class="flex-1 min-w-0 order-1 space-y-2 md:space-y-3">
                <!-- 标题 + 来源 · 时间 -->
                <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-1 md:gap-2">
                  <h3
                      class="text-base font-normal text-gray-900 dark:text-gray-100
                   line-clamp-2 flex-1 md:pr-4"
                  >
                    {{ item.title }}
                  </h3>
                  <p
                      class="text-xs text-gray-500 dark:text-gray-500
                       flex-shrink-0 whitespace-nowrap
                       max-w-full sm:max-w-[16rem] truncate"
                      :title="item.feedTitle"
                  >
                    {{ item.feedTitle }} · {{ item.timeAgo }}
                  </p>
                </div>

                <!-- 摘要 -->
                <p
                    class="hidden md:block text-sm leading-relaxed text-gray-600 dark:text-gray-400
                 line-clamp-2"
                >
                  {{ item.summary }}
                </p>

                <!-- 标签 -->
                <div v-if="item.tags?.length" class="hidden md:flex flex-wrap gap-2">
                  <button
                      v-for="tag in item.tags"
                      :key="tag"
                      type="button"
                      @click.stop.prevent="emit('select-tag', tag)"
                      class="px-3 py-1 text-xs font-medium rounded-full
                   bg-secondary-100 dark:bg-secondary-900/50
                   text-secondary-700 dark:text-secondary-300
                   hover:bg-secondary-200 dark:hover:bg-secondary-800/50
                   transition-colors"
                  >
                    #{{ tag }}
                  </button>
                </div>
              </div>

              <!-- 缩略图 -->
              <figure
                  v-if="item.thumbnail && !thumbErrorMap[item.id]"
                  class="flex-shrink-0 order-2
               w-20 h-20 md:w-48 md:h-32 lg:w-56 lg:h-36
               overflow-hidden rounded-lg md:rounded-xl
               bg-gray-100 dark:bg-gray-800"
              >
                <img
                    :src="item.thumbnail"
                    :alt="item.title"
                    loading="lazy"
                    decoding="async"
                    class="w-full h-full object-cover transition-transform duration-500
                 md:group-hover:scale-105"
                    @error="thumbErrorMap[item.id] = true"
                />
              </figure>
              <figure
                  v-else
                  class="hidden md:flex flex-shrink-0 order-2
               md:w-48 md:h-32 lg:w-56 lg:h-36
               overflow-hidden rounded-xl
               bg-gray-100 dark:bg-gray-800
               flex-col items-center justify-center text-gray-400 dark:text-gray-600 gap-2"
              >
                <svg
                    class="w-12 h-12"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                >
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
              </figure>
            </article>
          </router-link>
        </div>


        <!-- Card view - 恢复原始紧凑样式 -->
        <div
            v-else-if="view === 'card'"
            key="view-card"
            class="grid gap-0 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <router-link
              v-for="item in items"
              :key="item.id"
              :to="{ name: 'article', params: { id: item.id } }"
              class="group relative flex h-full cursor-pointer flex-col overflow-hidden rounded-xl p-3
                     active:bg-surface-container/60 dark:bg-surface-container/60
                     focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
            <article>
              <!-- 原有的 hover 背景效果 -->
              <span
                  class="pointer-events-none absolute inset-0 origin-center scale-50 rounded-xl h-1/5 bg-surface-container transition-transform duration-200 ease-out group-hover:scale-[1.02] group-hover:h-full"></span>

              <figure class="relative aspect-video w-full overflow-hidden bg-gray-100 dark:bg-gray-800 rounded-xl">
                <img
                    v-if="item.thumbnail && !thumbErrorMap[item.id]"
                    :src="item.thumbnail"
                    alt="文章缩略图"
                    loading="lazy"
                    decoding="async"
                    class="h-full w-full object-cover transition duration-300 group-hover:scale-[1.02]"
                    @error="thumbErrorMap[item.id] = true"/>
                <div
                    v-else
                    class="flex h-full w-full flex-col items-center justify-center text-gray-400 dark:text-gray-600 gap-2">
                  <svg class="w-12 h-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                       stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                    <circle cx="8.5" cy="8.5" r="1.5"/>
                    <polyline points="21 15 16 10 5 21"/>
                  </svg>
                  <!--                  <span class="text-xs">无图</span>-->
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

                <footer v-if="item.tags?.length"
                        class="flex flex-wrap gap-3 mt-auto text-xs text-gray-500 dark:text-gray-500">
                  <button
                      v-for="tag in item.tags"
                      :key="tag"
                      type="button"
                      class="hover:text-primary transition-colors"
                      @click.stop.prevent="emit('select-tag', tag)">
                    #{{ tag }}
                  </button>
                </footer>
              </div>
            </article>
          </router-link>
        </div>

        <!-- Title only view -->
        <div v-else key="view-only-title">
          <router-link
              v-for="item in items"
              :key="item.id"
              :to="{ name: 'article', params: { id: item.id } }"
              class="flex items-start gap-3 px-4 py-2.5
                     transition-colors cursor-pointer
                     active:bg-surface-container/60  hover:bg-surface-container/60 rounded-xl
                     focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
            <!-- 来源 -->
            <span class="flex-shrink-0 w-20 md:w-24 text-xs text-gray-500 dark:text-gray-400 truncate pt-0.5">
              {{ item.feedTitle }}
            </span>
            <!-- 标题 + 摘要 -->
            <p class="flex-1 min-w-0 text-sm truncate">
              <span class="text-gray-900 dark:text-gray-100 font-medium">{{ item.title }}</span>
              <!--              <span v-if="item.summary" class="text-gray-500 dark:text-gray-400 ml-2">{{ item.summary }}</span>-->
            </p>
            <!-- 时间 -->
            <span class="flex-shrink-0 text-xs text-gray-400 dark:text-gray-500 pt-0.5">
              {{ item.timeAgo }}
            </span>
          </router-link>
        </div>
      </transition>
    </section>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref, watch} from 'vue'

export interface ArticleListItemProps {
  id: string
  title: string
  summary: string
  feedTitle: string
  timeAgo: string
  tags?: string[]
  thumbnail?: string
}

const props = withDefaults(defineProps<{
  title: string
  subtitle: string
  items: ArticleListItemProps[]
  loading?: boolean
  emptyMessage?: string
}>(), {
  loading: false,
  emptyMessage: '暂无文章，添加订阅后即可看到推荐内容。'
})

const emit = defineEmits<{
  refresh: []
  'select-tag': [tag: string]
}>()

type ViewMode = 'magazine' | 'card' | 'only-title'
const STORAGE_KEY = 'article_feed_view_mode'
const saved = localStorage.getItem(STORAGE_KEY) as ViewMode | null
const view = ref<ViewMode>(saved ?? 'magazine')

function setView(v: ViewMode) {
  view.value = v
  try {
    localStorage.setItem(STORAGE_KEY, v)
  } catch {
  }
}

function btnClass(active: boolean) {
  return [
    'p-2 rounded-full transition-all',
    active
        ? 'bg-primary/10 text-primary shadow-sm'
        : 'text-gray-500 hover:bg-surface-container'
  ].join(' ')
}

const thumbErrorMap = reactive<Record<string, boolean>>({})

watch(() => props.items, (newItems) => {
  const present = new Set(newItems.map(i => i.id))
  Object.keys(thumbErrorMap).forEach(k => {
    if (!present.has(k)) delete thumbErrorMap[k]
  })
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0
}
</style>