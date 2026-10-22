<template>
  <div class="space-y-3.5 text-text">
    <slot name="header"></slot>

    <!-- Header：M3 视图控制栏与标题 -->
    <header class="mx-auto flex max-w-screen-lg flex-wrap items-center justify-between gap-4 px-4">
      <div class="flex items-center gap-3">
        <template v-if="title">
          <h1 class="text-xl font-bold tracking-tight text-text sm:text-2xl">
            {{ title }}
            <small v-if="subtitle" class="ml-2 text-xs font-normal text-text-secondary">{{ subtitle }}</small>
          </h1>
          <button
              type="button"
              class="flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text disabled:cursor-not-allowed disabled:opacity-40"
              @click="emit('refresh')"
              :disabled="loading"
              aria-label="刷新列表"
              title="刷新">
            <svg
                xmlns="http://www.w3.org/2000/svg"
                class="h-4 w-4 transition-transform"
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

      <!-- 分段视图切换按钮群 -->
      <div class="flex items-center gap-1 rounded-full bg-surface-container/50 p-1">
        <slot name="action"></slot>
        <button
            @click="setView('magazine')"
            :class="btnClass(view === 'magazine')"
            title="杂志视图"
            aria-label="杂志视图">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'magazine' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h8m-8 4h5m-5 5h8m-8 4h5"/>
            <rect x="15" y="4" width="5" height="5" rx="1.5"/>
            <rect x="15" y="14" width="5" height="5" rx="1.5"/>
          </svg>
        </button>
        <button
            @click="setView('social')"
            :class="btnClass(view === 'social')"
            title="社交视图"
            aria-label="社交视图">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'social' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </button>
        <button
            @click="setView('card')"
            :class="btnClass(view === 'card')"
            title="卡片视图"
            aria-label="卡片视图">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"
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
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'only-title' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 8h16M4 16h16"/>
          </svg>
        </button>
      </div>
    </header>

    <!-- 骨架屏 -->
    <section v-if="loading" class="space-y-4">
      <div v-if="view === 'card'"
           class="grid gap-2 grid-cols-1 md:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4 px-2 sm:px-4">
        <div v-for="i in 8" :key="`card-skel-${i}`"
             class="animate-pulse space-y-3 rounded-xl bg-surface-container/30 p-3">
          <div class="aspect-video w-full rounded-xl bg-surface-container/60"></div>
          <div class="flex items-start gap-2">
            <div class="h-7 w-7 rounded-full bg-surface-container shrink-0 mt-0.5"></div>
            <div class="flex-1 space-y-2">
              <div class="h-4 w-3/4 rounded bg-surface-container"></div>
              <div class="h-3 w-1/3 rounded bg-surface-container/60"></div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="view === 'magazine'" class="mx-auto max-w-screen-lg space-y-3">
        <div v-for="i in 8" :key="`mag-skel-${i}`"
             class="flex items-start gap-4 rounded-2xl bg-surface-container/25 p-4 animate-pulse md:gap-6">
          <div class="flex-1 space-y-2.5">
            <div class="h-4 w-2/3 rounded bg-surface-container"></div>
            <div class="h-3 w-1/3 rounded bg-surface-container/60"></div>
            <div class="hidden h-3 w-full rounded bg-surface-container/40 md:block"></div>
          </div>
          <div class="h-20 w-20 shrink-0 rounded-2xl bg-surface-container/60 md:h-28 md:w-44"></div>
        </div>
      </div>

      <div v-else-if="view === 'only-title'" class="mx-auto max-w-screen-lg space-y-1">
        <div v-for="i in 12" :key="`title-skel-${i}`"
             class="flex items-center gap-3 rounded-xl px-4 py-2.5 animate-pulse">
          <div class="h-3 w-20 shrink-0 rounded bg-surface-container md:w-24"></div>
          <div class="h-3.5 flex-1 rounded bg-surface-container/80"></div>
          <div class="h-3 w-12 shrink-0 rounded bg-surface-container/40"></div>
        </div>
      </div>

      <div v-else-if="view === 'social'" class="mx-auto max-w-[600px] space-y-3">
        <div v-for="i in 6" :key="`tl-skel-${i}`"
             class="animate-pulse rounded-2xl bg-surface-container/25 p-4 space-y-3">
          <div class="flex items-center gap-3">
            <div class="h-8 w-8 shrink-0 rounded-full bg-surface-container"></div>
            <div class="h-3.5 w-28 rounded bg-surface-container"></div>
          </div>
          <div class="space-y-2 pl-11">
            <div class="h-4 w-4/5 rounded bg-surface-container"></div>
            <div class="h-3 w-full rounded bg-surface-container/60"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- 内容展示区 -->
    <section v-else>
      <!-- 空状态 -->
      <div v-if="!items.length" class="flex flex-col items-center justify-center gap-3 py-20 text-center">
        <slot name="empty">
          <div class="flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/60">
            <svg class="h-7 w-7 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
          </div>
          <span class="max-w-xs text-xs text-text-secondary">{{ emptyMessage }}</span>
        </slot>
      </div>

      <transition name="fade" mode="out-in">
        <!-- 1. 杂志视图 (Magazine View) -->
        <div v-if="view === 'magazine'" key="view-magazine" class="mx-auto max-w-screen-lg space-y-3">
          <article
              v-for="item in items"
              :key="item.id"
              class="group flex cursor-pointer flex-row items-start gap-4 rounded-3xl p-4 transition duration-200 hover:bg-surface-container/50 md:gap-6 md:p-5"
              @click="handleArticleContainerClick(item, $event)"
          >
            <div class="order-1 flex-1 min-w-0 space-y-2">
              <div class="flex items-center gap-2 text-xs text-text-secondary">
                <router-link
                    v-if="item.feedId"
                    :to="{ name: 'feed', params: { feedId: item.feedId } }"
                    class="flex min-w-0 items-center gap-1.5 truncate font-medium hover:text-primary transition-colors focus-visible:outline-none"
                    :title="item.feedTitle">
                  <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                       :src="item.feedAvatar"
                       :alt="item.feedTitle"
                       class="h-4 w-4 shrink-0 rounded-full object-cover shadow-2xs"
                       @error="avatarErrorMap[item.id] = true"/>
                  <span v-else
                        class="flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[9px] font-bold text-primary">
                    {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                  </span>
                  <span class="truncate">{{ item.feedTitle }}</span>
                </router-link>
                <span v-else class="flex min-w-0 items-center gap-1.5 truncate">
                  <span class="truncate">{{ item.feedTitle }}</span>
                </span>
                <span>·</span>
                <span class="shrink-0 text-[11px]">{{ item.timeAgo }}</span>
              </div>

              <h3 class="text-base font-semibold leading-snug tracking-tight text-text line-clamp-2 group-hover:text-primary transition-colors">
                <router-link :to="{ name: 'article', params: { id: item.id } }">
                  {{ item.title }}
                </router-link>
              </h3>

              <p v-if="item.summary" class="text-xs leading-relaxed text-text-secondary line-clamp-2">
                {{ item.summary }}
              </p>

              <div v-if="item.tags?.length" class="flex flex-wrap gap-1.5 pt-0.5">
                <button
                    v-for="tag in item.tags"
                    :key="tag"
                    type="button"
                    @click.stop="emit('select-tag', tag)"
                    class="rounded-md bg-surface-container/60 px-2 py-0.5 text-[11px] font-medium text-text-secondary transition hover:bg-surface-container hover:text-text"
                >
                  #{{ tag }}
                </button>
              </div>
            </div>

            <router-link
                v-if="item.thumbnail && !thumbErrorMap[item.id]"
                :to="{ name: 'article', params: { id: item.id } }"
                class="order-2 h-20 w-24 shrink-0 overflow-hidden rounded-2xl bg-surface-container/60 shadow-2xs transition sm:h-28 sm:w-40 md:h-32 md:w-48"
            >
              <img
                  :src="item.thumbnail"
                  :alt="item.title"
                  loading="lazy"
                  decoding="async"
                  class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                  @error="thumbErrorMap[item.id] = true"
              />
            </router-link>
          </article>
        </div>

        <!-- 2. 卡片视图 (Card View) - 严格复原移动端完整适配布局 -->
        <div
            v-else-if="view === 'card'"
            key="view-card"
            class="grid grid-cols-1 gap-2 md:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4 px-2 sm:px-4">
          <article
              v-for="item in items"
              :key="item.id"
              class="group relative flex h-full w-full flex-col overflow-hidden rounded-xl p-3
                     bg-surface dark:bg-surface-container/60 hover:bg-surface-container/60
                     transition-colors duration-200 cursor-pointer"
              @click="handleArticleContainerClick(item, $event)">

            <!-- 封面图：固定纵横比，移动端 100% 满宽 -->
            <router-link
                :to="{ name: 'article', params: { id: item.id } }"
                class="relative block aspect-video w-full overflow-hidden bg-surface-container rounded-xl
                       focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
              <img
                  v-if="item.thumbnail && !thumbErrorMap[item.id]"
                  :src="item.thumbnail"
                  :alt="item.title"
                  loading="lazy"
                  decoding="async"
                  class="h-full w-full object-cover transition duration-300 group-hover:scale-[1.02]"
                  @error="thumbErrorMap[item.id] = true"/>
              <div
                  v-else
                  class="flex h-full w-full flex-col items-center justify-center text-text-muted gap-2">
                <svg class="w-8 h-8 opacity-40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                     stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
              </div>
            </router-link>

            <!-- 卡片主体：头像 + 标题 + 来源 + 标签 -->
            <div class="relative flex flex-1 flex-col gap-2 py-3">
              <header class="flex items-start gap-2.5">
                <!-- 头像 -->
                <router-link
                    v-if="item.feedId"
                    :to="{ name: 'feed', params: { feedId: item.feedId } }"
                    class="shrink-0 rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                    :aria-label="`查看 ${item.feedTitle}`">
                  <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                       :src="item.feedAvatar"
                       :alt="item.feedTitle"
                       class="w-7 h-7 rounded-full object-cover mt-0.5"
                       @error="avatarErrorMap[item.id] = true"/>
                  <span v-else
                        class="w-7 h-7 rounded-full bg-primary/10 text-primary flex items-center justify-center
                               text-[10px] font-semibold mt-0.5">
                    {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                  </span>
                </router-link>
                <span v-else class="shrink-0">
                  <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                       :src="item.feedAvatar"
                       :alt="item.feedTitle"
                       class="w-7 h-7 rounded-full object-cover mt-0.5"
                       @error="avatarErrorMap[item.id] = true"/>
                  <span v-else
                        class="w-7 h-7 rounded-full bg-primary/10 text-primary flex items-center justify-center
                               text-[10px] font-semibold mt-0.5">
                    {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                  </span>
                </span>

                <!-- 标题与来源 -->
                <div class="min-w-0 flex-1 space-y-1">
                  <h3 class="text-sm font-semibold leading-snug text-text line-clamp-2">
                    <router-link
                        :to="{ name: 'article', params: { id: item.id } }"
                        class="rounded hover:text-primary transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                      {{ item.title }}
                    </router-link>
                  </h3>
                  <p class="text-xs text-text-secondary flex items-center gap-1">
                    <router-link v-if="item.feedId"
                                 :to="{ name: 'feed', params: { feedId: item.feedId } }"
                                 class="truncate hover:text-primary transition-colors focus-visible:outline-none">
                      {{ item.feedTitle }}
                    </router-link>
                    <span v-else class="truncate">{{ item.feedTitle }}</span>
                    <span>·</span>
                    <span class="shrink-0">{{ item.timeAgo }}</span>
                  </p>

                  <div v-if="item.tags?.length"
                       class="flex flex-wrap gap-2 mt-2 pt-0.5 text-xs text-text-secondary">
                    <button
                        v-for="tag in item.tags"
                        :key="tag"
                        type="button"
                        class="hover:text-primary transition-colors"
                        @click.stop="emit('select-tag', tag)">
                      #{{ tag }}
                    </button>
                  </div>
                </div>
              </header>
            </div>
          </article>
        </div>

        <!-- 3. 仅标题视图 (Title Only View) -->
        <!-- Title only view：融合 MD3 语义色 + 原版移动端隐藏来源文字的宽视野 -->
        <div v-else-if="view === 'only-title'" key="view-only-title" class="mx-auto max-w-screen-lg space-y-1">
          <article
              v-for="item in items"
              :key="item.id"
              class="flex cursor-pointer items-start gap-2.5 sm:gap-3 rounded-2xl px-3.5 py-2.5 transition hover:bg-surface-container/60"
              @click="handleArticleContainerClick(item, $event)">

            <!-- 来源：小屏仅头像，大屏展示完整名称 -->
            <component
                :is="item.feedId ? 'router-link' : 'span'"
                :to="item.feedId ? { name: 'feed', params: { feedId: item.feedId } } : undefined"
                class="shrink-0 flex items-center gap-1.5 text-xs text-text-secondary sm:w-32 md:w-36 group"
                :title="item.feedTitle"
                @click.stop>
              <img
                  v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                  :src="item.feedAvatar"
                  :alt="item.feedTitle"
                  class="h-4 w-4 shrink-0 rounded-full object-cover mt-0.5"
                  @error="avatarErrorMap[item.id] = true"/>
              <span
                  v-else
                  class="flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-surface-container-high text-[9px] font-bold text-text-secondary mt-0.5">
        {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
      </span>
              <!-- 小屏隐藏文本，大屏显示截断，防止抢占标题空间 -->
              <span class="hidden sm:inline truncate group-hover:text-primary transition-colors">
        {{ item.feedTitle }}
      </span>
            </component>

            <!-- 标题：占据全部剩余宽度 -->
            <router-link
                :to="{ name: 'article', params: { id: item.id } }"
                class="min-w-0 flex-1 truncate text-xs font-semibold text-text transition hover:text-primary sm:text-sm">
              {{ item.title }}
            </router-link>

            <!-- 时间 -->
            <span class="shrink-0 text-[11px] text-text-secondary/70 pt-0.5">
      {{ item.timeAgo }}
    </span>
          </article>
        </div>

        <!-- 4. 社交流视图 (Social View) -->
        <div v-else key="view-social" class="mx-auto max-w-[600px] space-y-2.5">
          <article
              v-for="item in items"
              :key="item.id"
              class="cursor-pointer rounded-3xl bg-surface-container/25 p-4.5 transition hover:bg-surface-container/50 sm:p-5"
              @click="handleArticleContainerClick(item, $event)"
          >
            <div class="flex gap-3.5">
              <router-link
                  v-if="item.feedId"
                  :to="{ name: 'feed', params: { feedId: item.feedId } }"
                  class="shrink-0">
                <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                     :src="item.feedAvatar"
                     :alt="item.feedTitle"
                     class="h-9 w-9 rounded-full object-cover shadow-2xs"
                     @error="avatarErrorMap[item.id] = true"
                />
                <span v-else
                      class="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                  {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                </span>
              </router-link>

              <div class="min-w-0 flex-1 space-y-2">
                <div class="flex flex-wrap items-baseline gap-1.5 text-xs text-text-secondary">
                  <span class="font-bold text-text">{{ item.feedTitle }}</span>
                  <span>·</span>
                  <span class="text-[11px]">{{ item.timeAgo }}</span>
                </div>

                <h3 class="text-sm font-semibold leading-snug text-text transition group-hover:text-primary">
                  {{ item.title }}
                </h3>

                <p v-if="item.summary" class="text-xs leading-relaxed text-text-secondary line-clamp-3">
                  {{ item.summary }}
                </p>

                <figure v-if="item.thumbnail && !thumbErrorMap[item.id]"
                        class="mt-2.5 aspect-video overflow-hidden rounded-2xl bg-surface-container/60 shadow-2xs">
                  <img
                      :src="item.thumbnail"
                      :alt="item.title"
                      loading="lazy"
                      decoding="async"
                      class="h-full w-full object-cover transition-transform duration-300 hover:scale-[1.02]"
                      @error="thumbErrorMap[item.id] = true"
                  />
                </figure>
              </div>
            </div>
          </article>
        </div>
      </transition>
    </section>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref, watch} from 'vue'
import {useRouter} from 'vue-router'

const router = useRouter()

function handleArticleContainerClick(item: ArticleListItemProps, event: MouseEvent) {
  const target = event.target as HTMLElement | null
  if (target?.closest('a, button')) return
  if (item.id) {
    router.push({name: 'article', params: {id: item.id}})
  }
}

export interface ArticleListItemProps {
  id: string
  feedId?: string
  title: string
  summary: string
  feedTitle: string
  feedAvatar?: string
  timeAgo: string
  tags?: string[]
  thumbnail?: string
}

const props = withDefaults(defineProps<{
  title?: string
  subtitle?: string
  items: ArticleListItemProps[]
  loading?: boolean
  emptyMessage?: string
}>(), {
  title: '',
  subtitle: '',
  loading: false,
  emptyMessage: '暂无文章，添加订阅后即可看到推荐内容。'
})

const emit = defineEmits<{
  refresh: []
  'select-tag': [tag: string]
}>()

type ViewMode = 'magazine' | 'card' | 'only-title' | 'social'
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
    'flex h-7 w-7 items-center justify-center rounded-full transition duration-200 select-none',
    active
        ? 'bg-primary/15 font-semibold text-primary shadow-2xs'
        : 'text-text-secondary hover:bg-surface-container hover:text-text'
  ].join(' ')
}

const thumbErrorMap = reactive<Record<string, boolean>>({})
const avatarErrorMap = reactive<Record<string, boolean>>({})

watch(() => props.items, (newItems) => {
  const present = new Set(newItems.map(i => i.id))
  Object.keys(thumbErrorMap).forEach(k => {
    if (!present.has(k)) delete thumbErrorMap[k]
  })
  Object.keys(avatarErrorMap).forEach(k => {
    if (!present.has(k)) delete avatarErrorMap[k]
  })
})
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