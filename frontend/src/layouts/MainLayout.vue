<template>
  <div class="flex h-screen bg-surface text-slate-800" :class="{ 'overflow-hidden': mobileNavOpen }">
    <aside
      class="w-64 flex-col border-r border-slate-200 bg-white z-30 transform transition-transform duration-200 ease-out"
      :class="[
        mobileNavOpen ? 'translate-x-0 fixed inset-y-0 left-0 shadow-2xl' : 'hidden lg:flex',
        !mobileNavOpen && 'lg:flex'
      ]"
    >
      <div class="px-6 py-6 border-b border-slate-200 flex items-center justify-between">
        <div>
          <h1 class="text-xl font-semibold text-slate-900">iFeed</h1>
          <p class="text-sm text-slate-500 mt-1">AI RSS 阅读器</p>
        </div>
        <button class="lg:hidden text-slate-400 hover:text-slate-600" @click="mobileNavOpen = false">✕</button>
      </div>
      <nav class="flex-1 px-4 py-6 space-y-2 text-sm overflow-y-auto">
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          :to="item.to"
          class="flex items-center gap-3 px-3 py-2 rounded-xl transition hover:bg-slate-100"
          :class="isActive(item.name) ? 'bg-blue-50 text-blue-600 font-medium' : 'text-slate-600'"
          @click="mobileNavOpen = false"
        >
          <span class="inline-flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-blue-600 text-sm font-semibold">
            {{ item.badge }}
          </span>
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="px-6 py-6 border-t border-slate-200">
        <div class="flex items-center gap-3">
          <div class="h-10 w-10 rounded-full bg-gradient-to-br from-blue-500 to-indigo-500 text-white flex items-center justify-center text-lg font-semibold">
            {{ userInitials }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium truncate">{{ user?.username ?? '访客' }}</p>
            <p class="text-xs text-slate-400 truncate">{{ user?.userId ?? '' }}</p>
          </div>
          <button class="text-xs text-red-500 hover:text-red-600" @click="handleLogout">退出</button>
        </div>
      </div>
    </aside>

    <div class="flex flex-1 flex-col overflow-hidden">
      <header class="flex items-center gap-4 px-6 py-4 bg-white border-b border-slate-200">
        <button class="lg:hidden rounded-xl border border-slate-200 p-2" @click="mobileNavOpen = true">菜单</button>
        <div class="relative flex-1">
          <input
            v-model="search"
            type="search"
            placeholder="搜索文章、标签、订阅..."
            class="w-full rounded-2xl border-slate-200 bg-slate-50 pl-12 pr-4 py-3 focus:border-primary focus:ring-primary/30"
            @keyup.enter="handleSearch"
          />
          <span class="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" aria-hidden="true">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="h-5 w-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-4.35-4.35m0 0A7.5 7.5 0 1010.5 18a7.5 7.5 0 006.15-3.35z" />
            </svg>
          </span>
        </div>
        <RouterLink
          :to="{ name: 'subscriptions' }"
          class="hidden md:inline-flex items-center gap-2 rounded-2xl bg-primary text-white px-4 py-2 shadow hover:bg-blue-600"
        >
          + 添加订阅
        </RouterLink>
      </header>
      <main class="flex-1 overflow-y-auto bg-surface/50 p-6">
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

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);

const mobileNavOpen = ref(false);
const search = ref('');

const navItems = [
  { name: 'home', label: '首页概览', to: { name: 'home' as const }, badge: 'H' },
  { name: 'subscriptions', label: '订阅管理', to: { name: 'subscriptions' as const }, badge: 'S' },
  { name: 'collections', label: '收藏夹', to: { name: 'collections' as const }, badge: 'F' },
  { name: 'history', label: '阅读历史', to: { name: 'history' as const }, badge: 'R' }
];

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

watch(
  () => route.query.q,
  (value) => {
    search.value = typeof value === 'string' ? value : '';
  },
  { immediate: true }
);
</script>
