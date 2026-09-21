<template>
  <div class="max-w-screen-lg mx-auto px-3 sm:px-6 py-4 sm:py-6">
    <div class="mb-6 sm:mb-8">
      <h1 class="text-2xl sm:text-3xl font-bold text-text">{{ title }}</h1>
    </div>
    <div v-if="!auth.isAuthenticated" class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
      <div class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
        <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z" />
        </svg>
      </div>
      <h2 class="text-lg sm:text-xl font-semibold text-text mb-2">请先登录</h2>
      <p class="text-xs sm:text-sm text-text-secondary px-4 mb-6">{{ loginText ?? '登录后即可查看' + title }}</p>
      <router-link :to="{ name: 'auth', query: { redirect: route.fullPath } }" class="inline-block px-6 py-2.5 bg-secondary text-white rounded-full hover:bg-secondary-hover transition-colors text-sm font-medium">前往登录</router-link>
    </div>
    <template v-else>
      <slot name="controls" />
      <div class="flex items-center justify-between mb-4 sm:mb-6">
        <div class="text-xs sm:text-sm text-text-secondary">{{ totalText ?? '共 ' + total + ' 条' }}</div>
        <button :disabled="fetching" :aria-label="refreshLabel ?? '刷新' + title" class="p-2 hover:bg-surface-container rounded-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed" @click="$emit('refresh')">
          <svg class="w-5 h-5 text-text-secondary transition-transform" :class="{ 'animate-spin': fetching }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
          </svg>
        </button>
      </div>
      <div v-if="error" role="alert" class="mb-4 p-3 sm:p-4 bg-red-50 border border-red-200 rounded-lg">
        <div class="flex items-center gap-2 text-xs sm:text-sm text-red-800">
          <svg class="w-4 h-4 sm:w-5 sm:h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" /><path d="M12 8v4m0 4h.01" />
          </svg>
          <span>{{ error }}</span>
        </div>
        <button :disabled="fetching" class="mt-2 text-sm text-red-800 underline disabled:opacity-50" @click="$emit('refresh')">重试加载列表</button>
      </div>
      <ArticleCardList v-if="pending" :items="[]" loading />
      <template v-else>
        <slot />
        <div v-if="!error && total === 0" class="flex flex-col items-center justify-center py-16 sm:py-20 text-center">
          <div class="w-20 h-20 sm:w-24 sm:h-24 mb-4 sm:mb-6 flex items-center justify-center rounded-full bg-surface-container">
            <svg class="w-10 h-10 sm:w-12 sm:h-12 text-text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
            </svg>
          </div>
          <h2 v-if="emptyTitle" class="text-lg sm:text-xl font-semibold text-text mb-2">{{ emptyTitle }}</h2>
          <p class="text-xs sm:text-sm text-text-secondary px-4">{{ emptyText }}</p>
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
