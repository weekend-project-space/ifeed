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
              :aria-label="tr('common.refreshList')"
              :title="tr('common.refresh')">
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
            :title="tr('common.magazineView')"
            :aria-label="tr('common.magazineView')">
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
            :title="tr('common.socialView')"
            :aria-label="tr('common.socialView')">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'social' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </button>
        <button
            @click="setView('card')"
            :class="btnClass(view === 'card')"
            :title="tr('common.cardView')"
            :aria-label="tr('common.cardView')">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"
               :stroke-width="view === 'card' ? 2.5 : 2">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
          </svg>
        </button>
        <button
            @click="setView('only-title')"
            :class="btnClass(view === 'only-title')"
            :title="tr('common.titleView')"
            :aria-label="tr('common.titleView')">
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
           class="grid grid-cols-[repeat(auto-fill,minmax(min(100%,320px),1fr))] gap-x-4 gap-y-3 sm:gap-y-4 sm:px-4 lg:grid-cols-[repeat(auto-fill,minmax(min(100%,400px),1fr))]">
        <div v-for="i in 8" :key="`card-skel-${i}`"
             class="min-w-0 animate-pulse">
          <div class="aspect-video w-full rounded-xl bg-surface-container/60"></div>
          <div class="flex items-start gap-2 px-3 pt-2 sm:px-0 sm:pt-3">
            <div class="mt-0.5 h-7 w-7 shrink-0 rounded-full bg-surface-container"></div>
            <div class="min-w-0 flex-1 space-y-2">
              <div class="h-4 w-full rounded bg-surface-container"></div>
              <div class="h-4 w-2/3 rounded bg-surface-container"></div>
              <div class="flex h-5 items-center gap-2">
                <div class="h-3 w-1/2 rounded bg-surface-container/60"></div>
                <div class="h-3 w-8 rounded bg-surface-container/60"></div>
              </div>
              <div class="flex h-10 items-center gap-7">
                <div class="h-4 w-4 rounded bg-surface-container/60"></div>
                <div class="h-4 w-4 rounded bg-surface-container/60"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="view === 'magazine'" class="mx-auto max-w-screen-lg space-y-2">
        <div v-for="i in 8" :key="`mag-skel-${i}`"
             class="flex animate-pulse items-start gap-3 rounded-2xl bg-surface-container/25 p-3 md:gap-4 md:p-4">
          <div class="min-w-0 flex-1 space-y-2">
            <div class="h-4 w-2/3 rounded bg-surface-container"></div>
            <div class="h-3 w-1/3 rounded bg-surface-container/60"></div>
            <div class="h-3 w-full rounded bg-surface-container/40"></div>
            <div class="flex h-9 items-center gap-6">
              <div class="h-4 w-4 rounded bg-surface-container/60"></div>
              <div class="h-4 w-4 rounded bg-surface-container/60"></div>
            </div>
          </div>
          <div class="aspect-[8/5] w-28 shrink-0 rounded-2xl bg-surface-container/60 md:w-44"></div>
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

      <div v-else-if="view === 'social'"
           class="mx-auto max-w-[600px] divide-y divide-outline/40 overflow-hidden border-outline/40 sm:rounded-3xl sm:border" aria-hidden="true">
        <div v-for="i in 6" :key="`tl-skel-${i}`"
             class="flex animate-pulse items-start gap-3 px-3 py-3 sm:px-4">
          <div class="mt-0.5 h-9 w-9 shrink-0 rounded-full bg-surface-container"></div>
          <div class="min-w-0 flex-1 space-y-2">
            <div class="flex h-5 items-center gap-2">
              <div class="h-3 w-24 rounded bg-surface-container"></div>
              <div class="h-3 w-10 rounded bg-surface-container/60"></div>
            </div>
            <div class="h-4 w-4/5 rounded bg-surface-container"></div>
            <div class="h-3 w-full rounded bg-surface-container/60"></div>
            <div class="h-3 w-2/3 rounded bg-surface-container/60"></div>
            <div class="flex h-10 items-center gap-7">
              <div class="h-4 w-4 rounded bg-surface-container/60"></div>
              <div class="h-4 w-4 rounded bg-surface-container/60"></div>
            </div>
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
          <span class="max-w-xs text-xs text-text-secondary">{{ emptyMessage || tr('common.emptyArticlesHint') }}</span>
        </slot>
      </div>

      <transition name="fade" mode="out-in">
        <!-- 1. 杂志视图 (Magazine View) -->
        <div v-if="view === 'magazine'" key="view-magazine" class="mx-auto max-w-screen-lg space-y-2">
          <article
              v-for="item in items"
              :key="item.id"
              class="group flex cursor-pointer flex-row items-start gap-3 rounded-2xl p-3 transition duration-200 hover:bg-surface-container/50 md:gap-4 md:p-4"
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

              <h3 class="text-base font-semibold leading-snug tracking-tight text-text line-clamp-2">
                <router-link :to="{ name: 'article', params: { id: item.id } }" class="transition-colors hover:text-primary">
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
                    class="rounded-md bg-surface-container/60 px-2 py-0.5 text-[11px] font-medium text-text-secondary transition hover:bg-surface-container hover:text-primary"
                >
                  #{{ tag }}
                </button>
              </div>
              <div class="-ml-2.5 flex justify-start pt-1">
                <ArticleActions :article-id="item.id" :liked="item.liked ?? false" :collected="item.collected ?? false" :folder-id="item.folderId ?? null" compact dense />
              </div>
            </div>

            <router-link
                v-if="item.thumbnail && !thumbErrorMap[item.id]"
                :to="{ name: 'article', params: { id: item.id } }"
                class="order-2 aspect-[8/5] w-28 shrink-0 overflow-hidden rounded-2xl bg-surface-container/60 shadow-2xs transition md:w-44"
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

        <!-- 2. 卡片视图：视频站风格的大封面、双行标题与来源信息 -->
        <div
            v-else-if="view === 'card'"
            key="view-card"
            class="grid grid-cols-[repeat(auto-fill,minmax(min(100%,320px),1fr))] gap-x-4 gap-y-3 sm:gap-y-4 sm:px-4 lg:grid-cols-[repeat(auto-fill,minmax(min(100%,400px),1fr))]">
          <article
              v-for="item in items"
              :key="item.id"
              class="article-card group relative isolate flex min-w-0 cursor-pointer flex-col rounded-xl"
              @click="handleArticleContainerClick(item, $event)">

            <!-- 封面图：固定纵横比，移动端 100% 满宽 -->
            <router-link
                :to="{ name: 'article', params: { id: item.id } }"
                class="relative block aspect-video w-full shrink-0 overflow-hidden rounded-xl bg-surface-container/60
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
                  class="flex h-full w-full items-center justify-center text-text-muted">
                <svg class="w-8 h-8 opacity-40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                     stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
              </div>
            </router-link>

            <!-- 卡片主体：头像与标题并排，来源和时间位于标题下方 -->
            <div class="flex items-start gap-2 px-3 pt-2 sm:px-0 sm:pt-3">
                <!-- 头像 -->
                <component
                    :is="item.feedId ? 'router-link' : 'span'"
                    :to="item.feedId ? { name: 'feed', params: { feedId: item.feedId } } : undefined"
                    class="mt-0.5 shrink-0 rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                    :aria-label="tr('common.viewFeed', { value0: item.feedTitle })">
                  <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                       :src="item.feedAvatar"
                       :alt="item.feedTitle"
                       class="h-7 w-7 rounded-full object-cover"
                       @error="avatarErrorMap[item.id] = true"/>
                  <span v-else
                        class="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-[11px] font-semibold text-primary">
                    {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                  </span>
                </component>

                <!-- 标题与来源 -->
                <div class="min-w-0 flex-1">
                  <h3 class="break-words text-sm font-semibold leading-snug text-text line-clamp-2">
                    <router-link
                        :to="{ name: 'article', params: { id: item.id } }"
                        class="rounded hover:text-primary transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                      {{ item.title }}
                    </router-link>
                  </h3>
                  <div class="mt-1 flex min-w-0 items-center gap-1 text-xs leading-5 text-text-secondary">
                    <component :is="item.feedId ? 'router-link' : 'span'"
                               :to="item.feedId ? { name: 'feed', params: { feedId: item.feedId } } : undefined"
                               :title="item.feedTitle"
                               class="min-w-0 truncate transition-colors"
                               :class="item.feedId ? 'hover:text-primary' : ''">
                      {{ item.feedTitle }}
                    </component>
                    <span class="shrink-0" aria-hidden="true">·</span>
                    <span class="shrink-0 whitespace-nowrap">{{ item.timeAgo }}</span>
                  </div>

                  <div v-if="item.tags?.length"
                       class="mt-1 flex flex-wrap gap-x-2 gap-y-0.5 text-[11px] leading-4 text-text-secondary">
                    <button
                        v-for="tag in item.tags"
                        :key="tag"
                        type="button"
                        class="hover:text-primary transition-colors"
                        @click.stop="emit('select-tag', tag)">
                      #{{ tag }}
                    </button>
                  </div>
                  <div class="-ml-3 mt-0.5 flex justify-start">
                    <ArticleActions :article-id="item.id" :liked="item.liked ?? false" :collected="item.collected ?? false" :folder-id="item.folderId ?? null" compact />
                  </div>
                </div>
            </div>
          </article>
        </div>

        <!-- 3. 仅标题视图：HN 风格的编号、标题/来源与次行操作 -->
        <div v-else-if="view === 'only-title'" key="view-only-title" class="mx-auto max-w-screen-lg">
          <article
              v-for="(item, index) in items"
              :key="item.id"
              class="group flex cursor-pointer items-start gap-2 px-2 py-1 sm:gap-2.5 sm:px-3"
              @click="handleArticleContainerClick(item, $event)">
            <span class="w-6 shrink-0 pt-0.5 text-right font-mono text-xs tabular-nums text-text-secondary/60" aria-hidden="true">{{ index + 1 }}.</span>
            <div class="min-w-0 flex-1">
              <div class="leading-snug">
                <router-link
                    :to="{ name: 'article', params: { id: item.id } }"
                    class="break-words text-[13px] font-medium text-text transition-colors hover:text-primary sm:text-sm">
                  {{ item.title }}
                </router-link>
                <component
                    :is="item.feedId ? 'router-link' : 'span'"
                    :to="item.feedId ? { name: 'feed', params: { feedId: item.feedId } } : undefined"
                    class="ml-1.5 text-[11px] font-normal text-text-secondary/70 transition-colors"
                    :class="item.feedId ? 'hover:text-primary' : ''"
                    @click.stop>({{ item.feedTitle }})</component>
              </div>
              <div class="mt-0.5 flex flex-wrap items-center gap-x-1 text-[11px] text-text-secondary/70">
                <span class="mr-1">{{ item.timeAgo }}</span>
                <span aria-hidden="true">·</span>
                <ArticleActions :article-id="item.id" :liked="item.liked ?? false" :collected="item.collected ?? false"
                                :folder-id="item.folderId ?? null" compact inline />
              </div>
            </div>
          </article>
        </div>

        <!-- 4. 社交流视图：连续时间线、左侧头像与正文下方操作 -->
        <div v-else key="view-social"
             class="mx-auto max-w-[600px] divide-y divide-outline/40 overflow-hidden border-outline/40 sm:rounded-3xl sm:border">
          <article
              v-for="item in items"
              :key="item.id"
              class="cursor-pointer px-3 py-3 transition-colors hover:bg-surface-container/25 sm:px-4"
              @click="handleArticleContainerClick(item, $event)"
          >
            <div class="flex items-start gap-3">
              <component
                  :is="item.feedId ? 'router-link' : 'span'"
                  :to="item.feedId ? { name: 'feed', params: { feedId: item.feedId } } : undefined"
                  :title="item.feedTitle"
                  class="mt-0.5 shrink-0 rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
                <img v-if="item.feedAvatar && !avatarErrorMap[item.id]"
                     :src="item.feedAvatar"
                     :alt="item.feedTitle"
                     class="h-9 w-9 rounded-full object-cover"
                     @error="avatarErrorMap[item.id] = true"
                />
                <span v-else
                      class="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                  {{ item.feedTitle?.charAt(0)?.toUpperCase() }}
                </span>
              </component>

              <div class="min-w-0 flex-1">
                <div class="flex min-w-0 items-baseline gap-1.5 text-[13px] leading-5 text-text-secondary">
                  <component
                      :is="item.feedId ? 'router-link' : 'span'"
                      :to="item.feedId ? { name: 'feed', params: { feedId: item.feedId } } : undefined"
                      :title="item.feedTitle"
                      class="social-text-author min-w-0 truncate transition-colors"
                      :class="item.feedId ? 'hover:text-primary' : ''">{{ item.feedTitle }}</component>
                  <span class="shrink-0" aria-hidden="true">·</span>
                  <span class="social-text-time shrink-0">{{ item.timeAgo }}</span>
                </div>

                <h3 class="mt-0.5 break-words text-sm font-medium leading-snug text-text">
                  <router-link :to="{ name: 'article', params: { id: item.id } }" class="transition-colors hover:text-primary">
                    {{ item.title }}
                  </router-link>
                </h3>

                <p v-if="item.summary" class="social-text-body mt-1 line-clamp-3">
                  {{ item.summary }}
                </p>

                <router-link v-if="item.thumbnail && !thumbErrorMap[item.id]"
                             :to="{ name: 'article', params: { id: item.id } }"
                             class="mt-2 block w-fit max-w-full overflow-hidden rounded-2xl border border-outline/30 bg-surface-container/60">
                  <img
                      :src="item.thumbnail"
                      :alt="item.title"
                      loading="lazy"
                      decoding="async"
                      class="block h-auto max-h-[420px] w-auto max-w-full object-contain"
                      @error="thumbErrorMap[item.id] = true"
                  />
                </router-link>
                <div class="-ml-3 mt-0.5 flex justify-start">
                  <ArticleActions :article-id="item.id" :liked="item.liked ?? false" :collected="item.collected ?? false" :folder-id="item.folderId ?? null" compact />
                </div>
              </div>
            </div>
          </article>
        </div>
      </transition>
    </section>
  </div>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import {reactive, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import ArticleActions from './ArticleActions.vue'

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
  liked?: boolean
  collected?: boolean
  folderId?: string | null
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
  emptyMessage: ''
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
  const present = new Set(newItems.map((i: ArticleListItemProps) => i.id))
  Object.keys(thumbErrorMap).forEach(k => {
    if (!present.has(k)) delete thumbErrorMap[k]
  })
  Object.keys(avatarErrorMap).forEach(k => {
    if (!present.has(k)) delete avatarErrorMap[k]
  })
})
</script>

<style scoped>
.article-card::before {
  position: absolute;
  inset: 0;
  z-index: -1;
  border-radius: inherit;
  background: rgb(var(--md-surface-container) / 0.4);
  content: '';
  opacity: 0;
  pointer-events: none;
  transition: opacity 200ms ease;
}

@media (hover: hover) {
  .article-card:hover::before {
    opacity: 1;
  }
}

@media (min-width: 64rem) {
  .article-card::before {
    inset: -12px;
    border-radius: 24px;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
