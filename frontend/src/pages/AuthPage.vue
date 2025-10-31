<template>
  <div class="min-h-screen bg-surface text-text transition-colors duration-300">
    <div class="mx-auto flex min-h-screen max-w-5xl items-center justify-center px-3 py-8 sm:px-4 sm:py-12">
      <div class="w-full max-w-md overflow-hidden rounded-3xl border border-outline/40 bg-surface-container">
        <div class="flex items-center justify-between border-b border-outline/30 px-5 py-5 sm:px-8 sm:py-6">
          <div>
            <h1 class="text-2xl font-semibold text-text">欢迎使用 iFeed</h1>
            <p class="mt-1 text-sm text-text-secondary">AI 驱动的 RSS 阅读体验</p>
          </div>
          <button type="button"
            class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-outline/60 text-text transition hover:border-primary/60 hover:text-primary"
            :aria-label="`切换主题，当前${themeLabel}`" @click="toggleTheme">
            <svg v-if="isDark" class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z" />
            </svg>
            <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M12 18a6 6 0 1 1 0-12 6 6 0 0 1 0 12zm0 4a1 1 0 0 1-1-1v-1.2a.8.8 0 0 1 1.6 0V21a1 1 0 0 1-1 1zm0-18a1 1 0 0 1-1-1V1.2a.8.8 0 0 1 1.6 0V3a1 1 0 0 1-1 1zm9 7h-1.2a.8.8 0 0 1 0-1.6H21a1 1 0 1 1 0 2zm-18 0H1a1 1 0 1 1 0-2h1.2a.8.8 0 1 1 0 1.6zM5.64 19.36a1 1 0 0 1-1.41-1.41l.85-.85a.8.8 0 0 1 1.13 1.13zm13.14-13.14-.85.85a.8.8 0 1 1-1.13-1.13l.85-.85a1 1 0 0 1 1.13 1.13zm0 13.14-1.13-1.13a.8.8 0 1 1 1.13-1.13l.85.85a1 1 0 0 1-1.41 1.41zM5.64 4.64 4.79 3.8A1 1 0 1 1 6.2 2.36l.85.85a.8.8 0 1 1-1.13 1.13z" />
            </svg>
          </button>
        </div>
        <div class="px-5 py-5 sm:px-8 sm:py-6">
          <div
            class="mb-5 flex rounded-full border border-outline/40 bg-surface-variant/60 p-1 text-sm font-medium text-text-muted sm:mb-6">
            <button type="button" class="flex-1 rounded-full py-2 transition"
              :class="mode === 'login' ? 'bg-surface text-text border border-outline/50' : ''"
              @click="switchMode('login')">
              登录
            </button>
            <button type="button" class="flex-1 rounded-full py-2 transition"
              :class="mode === 'register' ? 'bg-surface text-text border border-outline/50' : ''"
              @click="switchMode('register')">
              注册
            </button>
          </div>
          <form class="space-y-4" @submit.prevent="handleSubmit">
            <div>
              <label class="block text-sm font-medium text-text-muted">用户名</label>
              <input v-model.trim="form.username" type="text" autocomplete="username" required
                class="mt-1 block w-full rounded-2xl border border-outline/50 bg-surface px-3 py-2.5 text-sm text-text transition focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 sm:px-4 sm:py-3"
                placeholder="输入用户名" />
            </div>
            <div>
              <label class="block text-sm font-medium text-text-muted">密码</label>
              <input v-model="form.password" type="password" autocomplete="current-password" required
                class="mt-1 block w-full rounded-2xl border border-outline/50 bg-surface px-3 py-2.5 text-sm text-text transition focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 sm:px-4 sm:py-3"
                placeholder="输入密码" />
            </div>
            <transition name="fade">
              <div v-if="mode === 'register'">
                <label class="block text-sm font-medium text-text-muted">确认密码</label>
                <input v-model="form.confirmPassword" type="password" autocomplete="new-password" required
                  class="mt-1 block w-full rounded-2xl border border-outline/50 bg-surface px-3 py-2.5 text-sm text-text transition focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 sm:px-4 sm:py-3"
                  placeholder="再次输入密码" />
              </div>
            </transition>

            <p v-if="errorMessage"
              class="rounded-2xl border border-danger/30 bg-danger/10 px-3 py-1.5 text-sm text-danger sm:px-4 sm:py-2">
              {{ errorMessage }}
            </p>

            <button type="submit"
              class="w-full rounded-2xl bg-primary py-2.5 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60 sm:py-3"
              :disabled="submitting">
              <span v-if="submitting" class="inline-flex items-center justify-center gap-2">
                <svg class="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
                </svg>
                处理中...
              </span>
              <span v-else>{{ mode === 'login' ? '登录' : '创建账户' }}</span>
            </button>
          </form>
          <p class="mt-6 text-center text-xs text-text-muted">
            还没有账号？
            <button type="button" class="font-semibold text-primary transition hover:opacity-80" @click="toggleMode">
              {{ mode === 'login' ? '注册新账户' : '返回登录' }}
            </button>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useThemeStore } from '../stores/theme';
import { useSubscriptionsStore } from '../stores/subscriptions';

type Mode = 'login' | 'register';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const themeStore = useThemeStore();
const { error, isAuthenticated } = storeToRefs(authStore);
const { isDark, label: themeLabel } = storeToRefs(themeStore);

const mode = ref<Mode>('login');
const submitting = ref(false);
const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
});
const localError = ref('');

const redirectToTarget = async () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/';
  await router.replace(redirect);
};

const handleSubmit = async () => {
  if (!form.username || !form.password) {
    localError.value = '请输入用户名和密码';
    return;
  }
  if (mode.value === 'register' && form.password !== form.confirmPassword) {
    localError.value = '两次输入的密码不一致';
    return;
  }
  submitting.value = true;
  localError.value = '';
  try {
    if (mode.value === 'login') {
      await authStore.login({ username: form.username, password: form.password });
    } else {
      await authStore.register({ username: form.username, password: form.password });
    }
    await redirectToTarget();
  } catch (err) {
    const message = err instanceof Error && err.message ? err.message : error.value || '操作失败，请稍后再试';
    localError.value = message;
  } finally {
    submitting.value = false;
  }
};

const toggleMode = () => {
  switchMode(mode.value === 'login' ? 'register' : 'login');
};

const switchMode = (target: Mode) => {
  if (mode.value === target) {
    return;
  }
  mode.value = target;
  localError.value = '';
  error.value = null;
  form.password = '';
  form.confirmPassword = '';
};

const toggleTheme = () => {
  themeStore.toggle();
};

watch(
  () => isAuthenticated.value,
  (isAuthed) => {
    if (isAuthed) {
      redirectToTarget().catch((err) => console.warn('跳转失败', err));
    }
  },
  { immediate: true }
);

const errorMessage = computed(() => localError.value || error.value || '');
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
