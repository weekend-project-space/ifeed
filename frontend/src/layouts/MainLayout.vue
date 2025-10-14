<template>
  <div class="min-h-screen bg-surface text-text transition-colors duration-300"
    :class="{ 'overflow-hidden': mobileNavOpen }">
    <header class="sticky top-0 z-40 border-b border-outline/40 bg-surface/90 backdrop-blur">
      <div class="flex items-center gap-3 px-4 py-3 sm:px-6">
        <div class="flex items-center gap-3">
          <button type="button"
            class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-outline/60 text-text transition hover:border-primary/50 hover:text-primary lg:hidden"
            aria-label="展开导航" @click="mobileNavOpen = true">
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" d="M4 7h16M4 12h16M4 17h16" />
            </svg>
          </button>
          <RouterLink :to="{ name: 'home' }"
            class="flex items-center gap-2 text-lg font-semibold tracking-tight text-text">
            <span
              class="inline-flex h-9 w-9 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
              i
            </span>
            <span class="leading-none">iFeed</span>
          </RouterLink>
        </div>

        <div class="hidden flex-1 lg:flex">
          <div class="relative mx-auto w-full max-w-2xl">
            <input v-model="search" type="search" placeholder="搜索文章、标签、订阅..."
              class="w-full rounded-full border border-outline/40 bg-surface-container pl-12 pr-28 py-3 text-sm text-text focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
              @keyup.enter="handleSearch" />
            <span class="pointer-events-none absolute left-5 top-1/2 -translate-y-1/2 text-text-muted">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="m21 21-4.35-4.35m0 0A7.5 7.5 0 1 0 10.5 18a7.5 7.5 0 0 0 6.15-3.35Z" />
              </svg>
            </span>
            <button type="button"
              class="absolute right-3 top-1/2 flex h-9 -translate-y-1/2 items-center justify-center gap-1 rounded-full bg-surface-variant px-4 text-xs font-medium text-text-secondary transition hover:bg-primary/15 hover:text-primary"
              @click="handleSearch">
              搜索
            </button>
          </div>
        </div>

        <div class="flex items-center gap-2 sm:gap-3">
          <button type="button"
            class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-outline/60 bg-surface-container text-text transition hover:border-primary/50 hover:text-primary"
            :aria-label="`切换主题，当前${themeLabel}`" @click="toggleTheme">
            <svg v-if="isDark" class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z" />
            </svg>
            <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M12 18a6 6 0 1 1 0-12 6 6 0 0 1 0 12zm0 4a1 1 0 0 1-1-1v-1.2a.8.8 0 0 1 1.6 0V21a1 1 0 0 1-1 1zm0-18a1 1 0 0 1-1-1V1.2a.8.8 0 0 1 1.6 0V3a1 1 0 0 1-1 1zm9 7h-1.2a.8.8 0 0 1 0-1.6H21a1 1 0 1 1 0 2zm-18 0H1a1 1 0 1 1 0-2h1.2a.8.8 0 1 1 0 1.6zM5.64 19.36a1 1 0 0 1-1.41-1.41l.85-.85a.8.8 0 0 1 1.13 1.13zm13.14-13.14-.85.85a.8.8 0 1 1-1.13-1.13l.85-.85a1 1 0 0 1 1.13 1.13zm0 13.14-1.13-1.13a.8.8 0 1 1 1.13-1.13l.85.85a1 1 0 0 1-1.41 1.41zM5.64 4.64 4.79 3.8A1 1 0 1 1 6.2 2.36l.85.85a.8.8 0 1 1-1.13 1.13z" />
            </svg>
          </button>

          <RouterLink :to="{ name: 'subscriptions' }"
            class="hidden items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition hover:bg-primary/90 sm:flex">
            <span class="text-lg leading-none">＋</span>
            添加订阅
          </RouterLink>
          <RouterLink :to="{ name: 'subscriptions' }"
            class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-outline/60 text-text transition hover:border-primary/60 hover:text-primary sm:hidden"
            aria-label="添加订阅">
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 5v14m-7-7h14" />
            </svg>
          </RouterLink>

          <div
            class="hidden items-center gap-3 rounded-full border border-outline/40 bg-surface-container px-3 py-1.5 md:flex">
            <div
              class="inline-flex h-9 w-9 items-center justify-center rounded-2xl bg-primary text-sm font-semibold text-primary-foreground">
              {{ userInitials }}
            </div>
            <div class="min-w-0">
              <p class="truncate text-sm font-medium text-text">{{ user?.username ?? '访客' }}</p>
            </div>
            <button type="button" class="text-xs font-medium text-danger transition hover:opacity-80"
              @click="handleLogout">
              退出
            </button>
          </div>
        </div>
      </div>

      <div class="px-4 pb-3 lg:hidden">
        <div class="relative">
          <input v-model="search" type="search" placeholder="搜索文章、标签、订阅..."
            class="w-full rounded-full border border-outline/40 bg-surface-container pl-12 pr-12 py-3 text-sm text-text focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
            @keyup.enter="handleSearch" />
          <span class="pointer-events-none absolute left-5 top-1/2 -translate-y-1/2 text-text-muted">
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
              <path stroke-linecap="round" stroke-linejoin="round"
                d="m21 21-4.35-4.35m0 0A7.5 7.5 0 1 0 10.5 18a7.5 7.5 0 0 0 6.15-3.35Z" />
            </svg>
          </span>
          <button type="button"
            class="absolute right-2 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-surface-variant text-text-secondary transition hover:bg-primary/15 hover:text-primary"
            @click="handleSearch">
            <svg class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd"
                d="M12.9 14.32a8 8 0 1 1 1.414-1.414l3.387 3.387a1 1 0 0 1-1.414 1.414l-3.387-3.387ZM14 8a6 6 0 1 1-12 0 6 6 0 0 1 12 0Z"
                clip-rule="evenodd" />
            </svg>
          </button>
        </div>
      </div>
    </header>

    <transition name="fade">
      <div v-if="mobileNavOpen" class="fixed inset-0 z-30 bg-black/40 backdrop-blur-xs lg:hidden"
        @click="mobileNavOpen = false" />
    </transition>

    <transition name="sidebar">
      <div v-if="mobileNavOpen"
        class="fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-outline/30 bg-surface py-6 shadow-xl lg:hidden">
        <div class="flex h-full flex-col overflow-hidden">
          <div class="flex items-center justify-between px-4 pb-4">
            <RouterLink :to="{ name: 'home' }" class="flex items-center gap-2 text-base font-semibold text-text"
              @click="mobileNavOpen = false">
              <span
                class="inline-flex h-9 w-9 items-center justify-center rounded-2xl bg-primary text-primary-foreground">i</span>
              <span class="leading-none">iFeed</span>
            </RouterLink>
            <button type="button"
              class="inline-flex h-9 w-9 items-center justify-center rounded-full border border-outline/50 text-text transition hover:border-primary/50 hover:text-primary"
              aria-label="关闭导航" @click="mobileNavOpen = false">
              <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round" d="m5 5 10 10M15 5 5 15" />
              </svg>
            </button>
          </div>
          <div class="px-4 pb-3">
            <p class="text-[11px] font-semibold text-text-muted/80">导航</p>
          </div>
          <nav class="flex-1 space-y-5 overflow-y-auto px-2">
            <div v-for="(section, index) in navSections" :key="section.id" class="space-y-2">
              <div v-if="section.title" class="px-3 text-[11px] font-semibold text-text-muted/70">
                {{ section.title }}
              </div>
              <div class="space-y-1">
                <component v-for="item in section.items" :is="item.to ? 'RouterLink' : 'button'" :key="item.id"
                  v-bind="item.to ? { to: item.to } : { type: 'button' }"
                  class="group flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-colors"
                  :class="isActiveItem(item)
                    ? 'bg-surface-variant/80 text-text font-semibold shadow-sm'
                    : 'text-text-secondary hover:bg-surface-variant/50 hover:text-text'"
                  @click="handleNavItemClick(item)">
                  <span v-if="item.icon"
                    class="flex h-8 w-8 items-center justify-center rounded-full text-text-muted transition group-hover:text-primary"
                    :class="isActiveItem(item) ? 'bg-surface text-text' : 'bg-surface-variant/60'">
                    <svg class="h-5 w-5" :viewBox="item.icon.viewBox ?? '0 0 20 20'"
                      :fill="item.icon.stroke ? 'none' : 'currentColor'"
                      :stroke="item.icon.stroke ? 'currentColor' : 'none'"
                      :stroke-width="item.icon.stroke ? 1.6 : undefined"
                      :stroke-linecap="item.icon.stroke ? 'round' : undefined"
                      :stroke-linejoin="item.icon.stroke ? 'round' : undefined">
                      <path v-for="path in item.icon.paths" :key="path" :d="path"
                        :fill="item.icon.stroke ? 'none' : 'currentColor'" />
                    </svg>
                  </span>
                  <span v-else-if="item.avatarText"
                    class="flex h-8 w-8 items-center justify-center rounded-md text-xs font-semibold uppercase"
                    :class="item.accent ?? 'bg-primary'">
                    {{ item.avatarText }}
                  </span>
                  <div class="flex min-w-0 flex-1 items-center justify-between gap-3">
                    <span class="truncate">{{ item.label }}</span>
                    <span v-if="item.badge"
                      class="inline-flex h-5 min-w-[1.5rem] items-center justify-center rounded-full bg-primary/10 px-2 text-[11px] font-semibold text-primary">
                      {{ item.badge }}
                    </span>
                  </div>
                </component>
              </div>
              <div v-if="index < navSections.length - 1" class="px-3 pt-2">
                <div class="h-px bg-outline/20" />
              </div>
            </div>
          </nav>
          <div class="mt-auto px-4 pt-6">
            <div class="rounded-2xl border border-outline/40 bg-surface-container px-4 py-4">
              <div class="flex items-center gap-3">
                <div
                  class="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-primary text-sm font-semibold text-primary-foreground">
                  {{ userInitials }}
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-medium text-text">{{ user?.username ?? '访客' }}</p>
                </div>
              </div>
              <button type="button"
                class="mt-4 w-full rounded-full bg-danger/10 py-2 text-xs font-semibold text-danger transition hover:bg-danger/15"
                @click="handleLogout">
                退出登录
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <div class="flex flex-1 overflow-hidden lg:pl-64">
      <!-- Make sidebar fixed and full height on desktop so it never scrolls with main content -->
      <aside
        class="fixed left-0 top-[75px] bottom-0 z-30 hidden w-64 border-r border-outline/30 bg-surface py-6 lg:block">
        <div class="flex h-full flex-col overflow-hidden">
          <div class="px-5 pb-3">
            <p class="text-[11px] font-semibold text-text-muted/80">导航</p>
          </div>
          <nav class="flex-1 space-y-5 overflow-y-auto px-3">
            <div v-for="(section, index) in navSections" :key="section.id" class="space-y-2">
              <div v-if="section.title" class="px-3 text-[11px] font-semibold text-text-muted/70">
                {{ section.title }}
              </div>
              <div class="space-y-1">
                <component v-for="item in section.items" :is="item.to ? 'RouterLink' : 'button'" :key="item.id"
                  v-bind="item.to ? { to: item.to } : { type: 'button' }"
                  class="group flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-colors"
                  :class="isActiveItem(item)
                    ? 'bg-surface-variant/80 text-text font-semibold shadow-sm'
                    : 'text-text-secondary hover:bg-surface-variant/50 hover:text-text'"
                  @click="handleNavItemClick(item)">
                  <span v-if="item.icon"
                    class="flex h-8 w-8 items-center justify-center rounded-full text-text-muted transition group-hover:text-primary"
                    :class="isActiveItem(item) ? 'bg-surface text-text' : 'bg-surface-variant/60'">
                    <svg class="h-5 w-5" :viewBox="item.icon.viewBox ?? '0 0 20 20'"
                      :fill="item.icon.stroke ? 'none' : 'currentColor'"
                      :stroke="item.icon.stroke ? 'currentColor' : 'none'"
                      :stroke-width="item.icon.stroke ? 1.6 : undefined"
                      :stroke-linecap="item.icon.stroke ? 'round' : undefined"
                      :stroke-linejoin="item.icon.stroke ? 'round' : undefined">
                      <path v-for="path in item.icon.paths" :key="path" :d="path"
                        :fill="item.icon.stroke ? 'none' : 'currentColor'" />
                    </svg>
                  </span>
                  <span v-else-if="item.avatarText"
                    class="flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold uppercase"
                    :class="item.accent ?? 'bg-primary'">
                    {{ item.avatarText }}
                  </span>
                  <div class="flex min-w-0 flex-1 items-center justify-between gap-3">
                    <span class="truncate">{{ item.label }}</span>
                    <span v-if="item.badge"
                      class="inline-flex h-5 min-w-[1.5rem] items-center justify-center rounded-full bg-primary/10 px-2 text-[11px] font-semibold text-primary">
                      {{ item.badge }}
                    </span>
                  </div>
                </component>
              </div>
              <div v-if="index < navSections.length - 1" class="px-3 pt-2">
                <div class="h-px bg-outline/20" />
              </div>
            </div>
          </nav>
        </div>
      </aside>

      <main class="flex-1 overflow-y-auto bg-surface-variant/30 px-4 pb-12 pt-6 sm:px-6">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { RouteLocationNormalizedLoaded, RouteLocationRaw } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '../stores/auth';
import { useThemeStore } from '../stores/theme';
import { useSubscriptionsStore } from '../stores/subscriptions';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const themeStore = useThemeStore();
const subscriptionsStore = useSubscriptionsStore();

const { user } = storeToRefs(authStore);
const { isDark, label: themeLabel } = storeToRefs(themeStore);

const mobileNavOpen = ref(false);
const search = ref('');

type NavIcon = {
  paths: string[];
  stroke?: boolean;
  viewBox?: string;
};

type NavItem = {
  id: string;
  label: string;
  to?: RouteLocationRaw;
  icon?: NavIcon;
  avatarText?: string;
  accent?: string;
  badge?: string;
  activeMatch?: (current: RouteLocationNormalizedLoaded) => boolean;
  action?: () => void;
};

type NavSection = {
  id: string;
  title?: string;
  items: NavItem[];
};

// Base navigation sections; the dynamic "订阅" section will be composed with store data
const baseNavSections: NavSection[] = [
  {
    id: 'primary',
    items: [
      {
        id: 'home',
        label: '首页',
        to: { name: 'home' as const },
        icon: {
          paths: ['M10 2.25 2.75 8.5V17a1 1 0 0 0 1 1h4.5v-4h4.5v4H17a1 1 0 0 0 1-1V8.5L10 2.25z']
        },
        activeMatch: (current) => {
          const view = current.query.view as string | undefined;
          const section = current.query.section as string | undefined;
          return current.name === 'home' && view !== 'shorts' && !section;
        }
      }
      // ,
      //   {
      //     id: 'shorts',
      //     label: 'Shorts',
      //     to: { name: 'home' as const, query: { view: 'shorts' } },
      //     icon: {
      //       paths: [
      //         'M5.75 4h8.5A1.75 1.75 0 0 1 16 5.75v8.5A1.75 1.75 0 0 1 14.25 16h-8.5A1.75 1.75 0 0 1 4 14.25v-8.5A1.75 1.75 0 0 1 5.75 4Z',
      //         'M9.25 7.6a.75.75 0 0 1 1.125-.65l3.1 1.83a.75.75 0 0 1 0 1.3l-3.1 1.82A.75.75 0 0 1 9.25 11V7.6Z'
      //       ]
      //     },
      //     activeMatch: (current) => {
      //       const view = current.query.view as string | undefined;
      //       return current.name === 'home' && view === 'shorts';
      //     }
      //   },
      //   {
      //     id: 'subscriptions',
      //     label: '订阅',
      //     to: { name: 'subscriptions' as const },
      //     icon: {
      //       paths: [
      //         'M4 5.5A1.5 1.5 0 0 1 5.5 4h9A1.5 1.5 0 0 1 16 5.5v9A1.5 1.5 0 0 1 14.5 16h-9A1.5 1.5 0 0 1 4 14.5v-9Z',
      //         'M9.2 7.25a.6.6 0 0 0-.6.6v4.3a.6.6 0 0 0 .92.52l3.16-2.15a.6.6 0 0 0 0-1.04L9.52 7.33a.6.6 0 0 0-.32-.08Z'
      //       ]
      //     },
      //     activeMatch: (current) => current.name === 'subscriptions'
      //   }
    ]
  },
  {
    id: 'you',
    title: '我',
    items: [
      {
        id: 'history',
        label: '历史记录',
        to: { name: 'history' as const },
        icon: {
          stroke: true,
          paths: ['M10 4.5a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11Z', 'M10 6.5v3.2l2.2 1.4']
        },
        activeMatch: (current) => current.name === 'history'
      },
      {
        id: 'library',
        label: '收藏夹',
        to: { name: 'collections' as const },
        icon: {
          paths: ['M6 3.25A1.25 1.25 0 0 1 7.25 2h5.5A1.25 1.25 0 0 1 14 3.25v13.63a.75.75 0 0 1-1.085.67L10 15.875l-2.915 1.675A.75.75 0 0 1 6 16.88Z']
        },
        activeMatch: (current) => {
          const tab = current.query.tab as string | undefined;
          return current.name === 'collections' && !tab;
        }
      },
      // {
      //   id: 'watch-later',
      //   label: '稍后再看',
      //   to: { name: 'collections' as const, query: { tab: 'later' } },
      //   icon: {
      //     stroke: true,
      //     paths: ['M10 4.25a5.75 5.75 0 1 0 0 11.5 5.75 5.75 0 0 0 0-11.5Z', 'M10 6.5v4l2.5 1.5']
      //   },
      //   activeMatch: (current) => {
      //     const tab = current.query.tab as string | undefined;
      //     return current.name === 'collections' && tab === 'later';
      //   }
      // },
      // {
      //   id: 'clips',
      //   label: '我的剪辑',
      //   to: { name: 'collections' as const, query: { tab: 'clips' } },
      //   icon: {
      //     stroke: true,
      //     paths: ['M5.25 5.25h9.5a1.25 1.25 0 0 1 1.25 1.25v8a1.25 1.25 0 0 1-1.25 1.25h-9.5A1.25 1.25 0 0 1 4 14.5v-8A1.25 1.25 0 0 1 5.25 5.25Z', 'M4 8.5h12']
      //   },
      //   activeMatch: (current) => {
      //     const tab = current.query.tab as string | undefined;
      //     return current.name === 'collections' && tab === 'clips';
      //   }
      // },
      // {
      //   id: 'liked',
      //   label: '点赞的视频',
      //   to: { name: 'collections' as const, query: { tab: 'liked' } },
      //   icon: {
      //     paths: ['M10 16.9 8.78 15.83C6 13.5 4.25 11.9 4.25 9.75c0-1.77 1.43-3.25 3.2-3.25 1 .02 1.95.53 2.55 1.35.6-.82 1.55-1.33 2.55-1.35 1.77 0 3.2 1.48 3.2 3.25 0 2.15-1.75 3.75-4.53 6.08L10 16.9Z']
      //   },
      //   activeMatch: (current) => {
      //     const tab = current.query.tab as string | undefined;
      //     return current.name === 'collections' && tab === 'liked';
      //   }
      // }
    ]
  },
  // {
  //   id: 'explore',
  //   title: '探索',
  //   items: [
  //     {
  //       id: 'trending',
  //       label: '热门',
  //       to: { name: 'home' as const, query: { section: 'trending' } },
  //       icon: {
  //         paths: ['M11 2.25c-2.63 3.18.38 4.7-1.84 7.78-.88 1.2-1.67 1.64-2.64 1.96a4.25 4.25 0 0 0 7.3 3.16c1.18-1.14 1.78-2.63 1.78-4.08 0-3.43-2.13-4.83-3.46-7.87l-.14-.27Z']
  //       },
  //       activeMatch: (current) => {
  //         const section = current.query.section as string | undefined;
  //         return current.name === 'home' && section === 'trending';
  //       }
  //     },
  //     {
  //       id: 'music',
  //       label: '音乐',
  //       to: { name: 'home' as const, query: { section: 'music' } },
  //       icon: {
  //         paths: ['M12.5 4.25v5.88a2.75 2.75 0 1 1-1.5-2.43V6.2l4-1v4.56a2.75 2.75 0 1 1-1.5-2.43V3.75l-1 .25Z']
  //       },
  //       activeMatch: (current) => {
  //         const section = current.query.section as string | undefined;
  //         return current.name === 'home' && section === 'music';
  //       }
  //     },
  //     {
  //       id: 'gaming',
  //       label: '游戏',
  //       to: { name: 'home' as const, query: { section: 'gaming' } },
  //       icon: {
  //         paths: [
  //           'M6 7a2.5 2.5 0 0 0-2.5 2.5v2A2.5 2.5 0 0 0 6 14h8a2.5 2.5 0 0 0 2.5-2.5v-2A2.5 2.5 0 0 0 14 7H6Z',
  //           'M7.5 9h-1v1H5.5v1h1v1h1v-1h1v-1h-1V9Z',
  //           'M13.5 10.25a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Zm1.5-1.5a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5Z'
  //         ]
  //       },
  //       activeMatch: (current) => {
  //         const section = current.query.section as string | undefined;
  //         return current.name === 'home' && section === 'gaming';
  //       }
  //     },
  //     {
  //       id: 'news',
  //       label: '新闻',
  //       to: { name: 'home' as const, query: { section: 'news' } },
  //       icon: {
  //         stroke: true,
  //         paths: ['M5 5.5h10a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1Z', 'M5 7.5h10', 'M7 9.5h6', 'M7 11.5h4']
  //       },
  //       activeMatch: (current) => {
  //         const section = current.query.section as string | undefined;
  //         return current.name === 'home' && section === 'news';
  //       }
  //     },
  //     {
  //       id: 'live',
  //       label: '直播',
  //       to: { name: 'home' as const, query: { section: 'live' } },
  //       icon: {
  //         stroke: true,
  //         paths: [
  //           'M10 6a4 4 0 1 1 0 8 4 4 0 0 1 0-8Z',
  //           'M4.5 10a5.5 5.5 0 1 1 11 0 5.5 5.5 0 0 1-11 0Z',
  //           'M2.5 10a7.5 7.5 0 1 1 15 0 7.5 7.5 0 0 1-15 0Z'
  //         ]
  //       },
  //       activeMatch: (current) => {
  //         const section = current.query.section as string | undefined;
  //         return current.name === 'home' && section === 'live';
  //       }
  //     }
  //   ]
  // }
];

// Generate dynamic subscriptions section from store items
const subscriptionNavSection = computed<NavSection>(() => {
  const items = subscriptionsStore.items.map((s, idx) => {
    const label = s.title?.trim() || (new URL(s.siteUrl || s.url).hostname);
    const initials = label
      .split(/\s+/)
      .map((p) => p.charAt(0).toUpperCase())
      .join('')
      .slice(0, 2) || 'S';

  const accentPalette = [
      'bg-slate-200 text-slate-900',
      'bg-zinc-200 text-zinc-900',
      'bg-stone-200 text-stone-900',
      'bg-gray-200 text-gray-900',
      'bg-neutral-200 text-neutral-900'
    ];

    const accent = accentPalette[idx % accentPalette.length];

    return {
      id: `sub-${s.feedId}`,
      label,
      to: { name: 'feed' as const, params: { feedId: s.feedId } },
      avatarText: initials,
      accent,
      activeMatch: (current: RouteLocationNormalizedLoaded) => {
        const paramId = typeof current.params.feedId === 'string' ? current.params.feedId : undefined;
        return current.name === 'feed' && paramId === s.feedId;
      }
    } satisfies NavItem;
  });

  // Always include a manage entry at the end
  items.push({
    id: 'manage-subscriptions',
    label: '管理订阅',
    to: { name: 'subscriptions' as const, query: { manage: 'true' } },
    icon: {
      stroke: true,
      paths: ['M5 5.5h10a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1Z', 'M5 7.5h10', 'M7 9.5h6', 'M7 11.5h4'],
      viewBox: '0 0 20 20'
    },
    activeMatch: (current) => {
      const manage = current.query.manage as string | undefined;
      return current.name === 'subscriptions' && manage === 'true';
    }
  });

  return {
    id: 'subscriptions',
    title: '订阅',
    items
  } satisfies NavSection;
});

// Combine base + dynamic sections for rendering
const navSections = computed<NavSection[]>(() => {
  return [...baseNavSections, subscriptionNavSection.value];
});

const handleNavItemClick = (item: NavItem) => {
  if (item.action) {
    item.action();
  }
  if (mobileNavOpen.value) {
    mobileNavOpen.value = false;
  }
};

const isActiveItem = (item: NavItem) => {
  if (item.activeMatch) {
    return item.activeMatch(route);
  }
  if (!item.to) {
    return false;
  }
  const resolved = router.resolve(item.to);
  if (resolved.name && resolved.name === route.name) {
    return true;
  }
  return resolved.path === route.path;
};

const userInitials = computed(() => {
  const username = user.value?.username ?? '';
  if (!username) {
    return 'U';
  }
  return username
    .split(/\s+/)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')
    .slice(0, 2);
});

const handleSearch = () => {
  const keyword = search.value.trim();
  const currentType = typeof route.query.type === 'string' ? route.query.type : undefined;
  const query: Record<string, string> = {};
  if (keyword) {
    query.q = keyword;
    if (currentType === 'semantic') {
      query.type = currentType;
    }
  }
  router.push({ name: 'home', query });
};

const handleLogout = async () => {
  await authStore.logout();
  router.replace({ name: 'auth' });
};

const toggleTheme = () => {
  themeStore.toggle();
};

watch(
  () => route.query.q,
  (value) => {
    search.value = typeof value === 'string' ? value : '';
  },
  { immediate: true }
);

// Ensure subscriptions are loaded once when layout mounts
onMounted(async () => {
  if (!subscriptionsStore.items.length && !subscriptionsStore.loading) {
    try {
      await subscriptionsStore.fetchSubscriptions();
    } catch {
      // ignore here; page components already show error details where needed
    }
  }
});
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.sidebar-enter-active,
.sidebar-leave-active {
  transition: transform 0.2s ease;
}

.sidebar-enter-from,
.sidebar-leave-to {
  transform: translateX(-100%);
}
</style>
