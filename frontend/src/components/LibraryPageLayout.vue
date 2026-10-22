<template>
  <div class="mx-auto max-w-screen-lg px-4 py-6 text-text sm:px-6 sm:py-8">
    <div class="mb-6 sm:mb-8">
      <h1 class="text-2xl font-bold tracking-tight text-text sm:text-3xl">{{ title }}</h1>
    </div>

    <!-- 未登录引导 -->
    <div v-if="!auth.isAuthenticated" class="flex flex-col items-center justify-center py-20 text-center">
      <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/60">
        <svg class="h-8 w-8 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z" />
        </svg>
      </div>
      <h2 class="text-base font-bold text-text mb-1">请先登录</h2>
      <p class="mb-5 text-xs text-text-secondary">{{ loginText ?? '登录后即可查看' + title }}</p>
      <router-link :to="{ name: 'auth', query: { redirect: route.fullPath } }"
                   class="inline-flex h-9 items-center justify-center rounded-full bg-primary px-6 text-xs font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-95">
        前往登录
      </router-link>
    </div>

    <!-- 登录后内容 -->
    <template v-else>
      <slot name="controls" />
      <div class="mb-4 flex items-center justify-between sm:mb-6">
        <div class="text-xs font-medium text-text-secondary">{{ totalText ?? '共 ' + total + ' 条' }}</div>
        <button :disabled="fetching" :aria-label="refreshLabel ?? '刷新' + title"
                class="flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text disabled:opacity-40"
                @click="$emit('refresh')">
          <svg class="h-4 w-4 transition-transform" :class="{ 'animate-spin': fetching }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
          </svg>
        </button>
      </div>

      <!-- 错误横幅 -->
      <div v-if="error" role="alert" class="mb-4 rounded-2xl bg-red-500/10 p-4 text-xs text-red-600 dark:text-red-400">
        <div class="flex items-center gap-2">
          <svg class="h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" /><path d="M12 8v4m0 4h.01" />
          </svg>
          <span>{{ error }}</span>
        </div>
        <button :disabled="fetching" class="mt-2 font-semibold underline hover:opacity-80" @click="$emit('refresh')">重试加载列表</button>
      </div>

      <ArticleCardList v-if="pending" :items="[]" loading />

      <template v-else>
        <slot />
        <div v-if="!error && total === 0" class="flex flex-col items-center justify-center py-20 text-center">
          <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container/60">
            <svg class="h-8 w-8 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
            </svg>
          </div>
          <h2 v-if="emptyTitle" class="text-base font-bold text-text mb-1">{{ emptyTitle }}</h2>
          <p class="text-xs text-text-secondary">{{ emptyText }}</p>
        </div>
        <slot name="footer" />
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
import ArticleCardList from './ArticleCardList.vue';
import { useAuthStore } from '@/stores/auth';
import { useDocumentTitle } from '@/composables/documentTitle';

const props = defineProps<{
  title: string; total: number; pending: boolean; fetching: boolean; error: string;
  emptyTitle?: string; emptyText: string; totalText?: string; refreshLabel?: string; loginText?: string;
}>();
defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const route = useRoute();
useDocumentTitle(() => props.title);
</script>