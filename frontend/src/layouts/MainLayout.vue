<template>
  <div
    class="min-h-screen bg-surface text-text transition-colors duration-300"
    :class="{ 'overflow-hidden': mobileNavOpen }"
  >
    <header class="sticky top-0 z-40 border-b border-outline/40 bg-surface/90 backdrop-blur">
      <div class="flex items-center gap-3 px-4 py-3 sm:px-6">
        <div class="flex items-center gap-3">
          <button
            type="button"
            class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-outline/60 text-text transition hover:border-primary/50 hover:text-primary lg:hidden"
            aria-label="展开导航"
            @click="mobileNavOpen = true"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" d="M4 7h16M4 12h16M4 17h16" />
            </svg>
          </button>
          <RouterLink
            :to="{ name: 'home' }"
            class="flex items-center gap-2 text-lg font-semibold tracking-tight text-text"
          >
            <span class="inline-flex h-9 w-9 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
              i
            </span>
            <span class="leading-none">iFeed</span>
          </RouterLink>
        </div>

        <div class="hidden flex-1 lg:flex">
          <div class="relative mx-auto w-full max-w-2xl">
            <input
              v-model="search"
              type="search"
              placeholder="搜索文章、标签、订阅..."
              class="w-full rounded-full border border-outline/40 bg-surface-container pl-12 pr-28 py-3 text-sm text-text focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
              @keyup.enter="handleSearch"
            />
            <span class="pointer-events-none absolute left-5 top-1/2 -translate-y-1/2 text-text-muted">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-4.35-4.35m0 0A7.5 7.5 0 1 0 10.5 18a7.5 7.5 0 0 0 6.15-3.35Z" />
              </svg>
            </span>
            <button
              type="button"
              class="absolute right-3 top-1/2 flex h-9 -translate-y-1/2 items-center justify-center gap-1 rounded-full bg-surface-variant px-4 text-xs font-medium text-text-secondary transition hover:bg-primary/15 hover:text-primary"
              @click="handleSearch"
            >
              搜索
            </button>
          </div>
        </div>

        <div class="flex items-center gap-2 sm:gap-3">
          <button
            type="button"
            class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-outline/60 bg-surface-container text-text transition hover:border-primary/50 hover:text-primary"
            :aria-label="`切换主题，当前${themeLabel}`"
            @click="toggleTheme"
          >
            <svg v-if="isDark" class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z"
              />
            </svg>
            <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M12 18a6 6 0 1 1 0-12 6 6 0 0 1 0 12zm0 4a1 1 0 0 1-1-1v-1.2a.8.8 0 0 1 1.6 0V21a1 1 0 0 1-1 1zm0-18a1 1 0 0 1-1-1V1.2a.8.8 0 0 1 1.6 0V3a1 1 0 0 1-1 1zm9 7h-1.2a.8.8 0 0 1 0-1.6H21a1 1 0 1 1 0 2zm-18 0H1a1 1 0 1 1 0-2h1.2a.8.8 0 1 1 0 1.6zM5.64 19.36a1 1 0 0 1-1.41-1.41l.85-.85a.8.8 0 0 1 1.13 1.13zm13.14-13.14-.85.85a.8.8 0 1 1-1.13-1.13l.85-.85a1 1 0 0 1 1.13 1.13zm0 13.14-1.13-1.13a.8.8 0 1 1 1.13-1.13l.85.85a1 1 0 0 1-1.41 1.41zM5.64 4.64 4.79 3.8A1 1 0 1 1 6.2 2.36l.85.85a.8.8 0 1 1-1.13 1.13z"
              />
            </svg>
          </button>

          <RouterLink
            :to="{ name: 'subscriptions' }"
            class="hidden items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition hover:bg-primary/90 sm:flex"
          >
            <span class="text-lg leading-none">＋</span>
            添加订阅
          </RouterLink>
          <RouterLink
            :to="{ name: 'subscriptions' }"
            class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-outline/60 text-text transition hover:border-primary/60 hover:text-primary sm:hidden"
            aria-label="添加订阅"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 5v14m-7-7h14" />
            </svg>
          </RouterLink>

          <div class="hidden items-center gap-3 rounded-full border border-outline/40 bg-surface-container px-3 py-1.5 md:flex">
            <div class="inline-flex h-9 w-9 items-center justify-center rounded-2xl bg-primary text-sm font-semibold text-primary-foreground">
              {{ userInitials }}
            </div>
            <div class="min-w-0">
              <p class="truncate text-sm font-medium text-text">{{ user?.username ?? '访客' }}</p>
            </div>
            <button
              type="button"
              class="text-xs font-medium text-danger transition hover:opacity-80"
              @click="handleLogout"
            >
              退出
            </button>
          </div>
        </div>
      </div>

      <div class="px-4 pb-3 lg:hidden">
        <div class="relative">
          <input
            v-model="search"
            type="search"
            placeholder="搜索文章、标签、订阅..."
            class="w-full rounded-full border border-outline/40 bg-surface-container pl-12 pr-12 py-3 text-sm text-text focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
            @keyup.enter="handleSearch"
          />
          <span class="pointer-events-none absolute left-5 top-1/2 -translate-y-1/2 text-text-muted">
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
              <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-4.35-4.35m0 0A7.5 7.5 0 1 0 10.5 18a7.5 7.5 0 0 0 6.15-3.35Z" />
            </svg>
          </span>
          <button
            type="button"
            class="absolute right-2 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-surface-variant text-text-secondary transition hover:bg-primary/15 hover:text-primary"
            @click="handleSearch"
          >
            <svg class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M12.9 14.32a8 8 0 1 1 1.414-1.414l3.387 3.387a1 1 0 0 1-1.414 1.414l-3.387-3.387ZM14 8a6 6 0 1 1-12 0 6 6 0 0 1 12 0Z"
                clip-rule="evenodd"
              />
            </svg>
          </button>
        </div>
      </div>
    </header>

    <transition name="fade">
      <div
        v-if="mobileNavOpen"
        class="fixed inset-0 z-30 bg-black/40 backdrop-blur-xs lg:hidden"
        @click="mobileNavOpen = false"
      />
    </transition>

    <div class="flex flex-1 overflow-hidden">
      <aside
        class="fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r border-outline/40 bg-surface py-6 transition-transform duration-200 ease-out lg:top-[72px] lg:translate-x-0"
        :class="mobileNavOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'"
      >
        <div class="px-6 pb-4">
          <p class="text-xs font-semibold uppercase tracking-wide text-text-muted">快速访问</p>
        </div>
        <nav class="flex-1 space-y-1 overflow-y-auto px-2">
          <RouterLink
            v-for="item in navItems"
            :key="item.name"
            :to="item.to"
            class="group relative flex items-center gap-3 rounded-2xl border border-transparent px-4 py-3 text-sm font-medium transition"
            :class="isActive(item.name)
              ? 'border-primary/30 bg-primary/10 text-primary'
              : 'text-text-secondary hover:bg-surface-variant/70 hover:text-text'"
            @click="mobileNavOpen = false"
          >
            <span
              class="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-surface-variant text-sm font-semibold text-text"
              :class="isActive(item.name) ? 'bg-primary text-primary-foreground' : ''"
            >
              {{ item.badge }}
            </span>
            <span>{{ item.label }}</span>
            <span
              v-if="isActive(item.name)"
              class="absolute right-3 h-8 w-1 rounded-full bg-primary"
            />
          </RouterLink>
        </nav>
        <div class="mt-auto px-6 pt-6 lg:hidden">
          <div class="rounded-2xl border border-outline/40 bg-surface-container px-4 py-4">
            <div class="flex items-center gap-3">
              <div class="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-primary text-sm font-semibold text-primary-foreground">
                {{ userInitials }}
              </div>
              <div class="min-w-0">
                <p class="truncate text-sm font-medium text-text">{{ user?.username ?? '访客' }}</p>
              </div>
            </div>
            <button
              type="button"
              class="mt-4 w-full rounded-full bg-danger/10 py-2 text-xs font-semibold text-danger transition hover:bg-danger/15"
              @click="handleLogout"
            >
              退出登录
            </button>
          </div>
        </div>
      </aside>

      <main class="flex-1 overflow-y-auto bg-surface-variant/30 px-4 pb-12 pt-6 sm:px-6 lg:ml-72">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '../stores/auth';
import { useThemeStore } from '../stores/theme';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const themeStore = useThemeStore();

const { user } = storeToRefs(authStore);
const { isDark, label: themeLabel } = storeToRefs(themeStore);

const mobileNavOpen = ref(false);
const search = ref('');

const navItems = [
  { name: 'home', label: '首页概览', to: { name: 'home' as const }, badge: 'H' },
  { name: 'subscriptions', label: '订阅管理', to: { name: 'subscriptions' as const }, badge: 'S' },
  { name: 'collections', label: '收藏夹', to: { name: 'collections' as const }, badge: 'F' },
  { name: 'history', label: '阅读历史', to: { name: 'history' as const }, badge: 'R' }
] as const;

const isActive = (name: string) => route.name === name;

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

const handleLogout = () => {
  authStore.logout();
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
</style>
