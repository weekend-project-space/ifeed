<template>
  <div class="min-h-screen w-full bg-surface text-text transition-colors duration-300 flex flex-col items-center justify-center p-4">

    <div class="w-full max-w-[400px] flex flex-col items-center">

      <div class="mb-10 text-center">
        <h1 class="text-3xl font-bold text-text mb-2 tracking-tight">欢迎使用 iFeed</h1>
        <p class="text-text-secondary text-sm">AI 驱动的 RSS 阅读体验</p>
      </div>

      <div class="w-full space-y-3 mb-8">
        <button type="button" class="social-btn">
          <span class="icon-wrapper text-red-500">
             <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor"><path d="M12.48 10.92v3.28h7.84c-.24 1.84-.853 3.187-1.787 4.133-1.147 1.147-2.933 2.4-6.053 2.4-4.827 0-8.6-3.893-8.6-8.72s3.773-8.72 8.6-8.72c2.6 0 4.507 1.027 5.907 2.347l2.307-2.307C18.747 1.44 16.133 0 12.48 0 5.867 0 .533 5.333.533 12S5.867 24 12.48 24c3.44 0 6.053-1.147 8.213-3.307 2.2-2.187 3.053-5.227 3.053-7.573 0-.747-.067-1.48-.187-2.2h-11.08z"/></svg>
          </span>
          <span>使用 Google 继续</span>
        </button>

        <button type="button" class="social-btn">
          <span class="icon-wrapper text-text">
             <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor"><path d="M17.05 20.28c-.98.95-2.05.88-3.08.4-.35-.16-1.07-.16-1.42 0-1.03.48-2.1.55-3.08-.4-.97-.98-1.56-2.58-1.56-4.13 0-2.92 2.32-4.43 4.67-4.43 1.25 0 2.27.46 2.87.46.6 0 1.76-.5 2.92-.5 1.05 0 2.15.5 2.87 1.34-2.58 1.25-2.16 4.38.25 5.38-.45 1.15-1.07 2.1-1.63 2.68l-.81.2zM15.15 7.63c.65-1.08 1.13-2.53.95-3.83-1.35.1-2.95.83-3.73 1.93-.65 1.03-1.05 2.43-.88 3.73 1.35.03 2.9-.68 3.66-1.83z"/></svg>
          </span>
          <span>使用 Apple 继续</span>
        </button>

        <button type="button" class="social-btn">
          <span class="icon-wrapper text-[#00A4EF]">
             <svg class="w-5 h-5" viewBox="0 0 23 23" fill="currentColor"><path d="M0 0h11v11H0zM12 0h11v11H12zM0 12h11v11H0zM12 12h11v11H12z"/></svg>
          </span>
          <span>使用 LiunxDo 继续</span>
        </button>
      </div>

      <div class="relative w-full mb-8 text-center">
        <div class="absolute inset-0 flex items-center">
          <div class="w-full border-t border-outline/30"></div>
        </div>
        <div class="relative inline-flex bg-surface px-4 text-xs text-text-muted uppercase tracking-wider">
          或
        </div>
      </div>

      <form class="w-full space-y-5" @submit.prevent="handleSubmit">
        <transition name="fade">
          <div v-if="errorMessage" class="flex items-center gap-2 text-danger text-sm bg-danger/5 px-4 py-3 rounded-2xl border border-danger/10">
            <svg class="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            {{ errorMessage }}
          </div>
        </transition>

        <div>
          <div class="relative group">
            <input
                v-model.trim="form.username"
                type="text"
                required
                class="peer block w-full rounded-full border border-outline/40 bg-surface px-6 py-4 text-text placeholder-transparent focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/10 transition-all"
                placeholder="电子邮箱地址"
                id="email_input"
            />
            <label for="email_input" class="absolute left-6 top-4 z-10 origin-[0] -translate-y-2.5 scale-75 transform text-sm text-text-muted duration-200 peer-placeholder-shown:translate-y-0 peer-placeholder-shown:scale-100 peer-focus:-translate-y-2.5 peer-focus:scale-75 cursor-text bg-surface px-1">
              电子邮箱地址 / 用户名
            </label>
          </div>
        </div>

        <div>
          <div class="relative group">
            <input
                v-model="form.password"
                type="password"
                required
                class="peer block w-full rounded-full border border-outline/40 bg-surface px-6 py-4 text-text placeholder-transparent focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/10 transition-all"
                placeholder="密码"
                id="password_input"
            />
            <label for="password_input" class="absolute left-6 top-4 z-10 origin-[0] -translate-y-2.5 scale-75 transform text-sm text-text-muted duration-200 peer-placeholder-shown:translate-y-0 peer-placeholder-shown:scale-100 peer-focus:-translate-y-2.5 peer-focus:scale-75 cursor-text bg-surface px-1">
              密码
            </label>
          </div>
        </div>

        <transition name="fade">
          <div v-if="mode === 'register'">
            <div class="relative group">
              <input
                  v-model="form.confirmPassword"
                  type="password"
                  required
                  class="peer block w-full rounded-full border border-outline/40 bg-surface px-6 py-4 text-text placeholder-transparent focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/10 transition-all"
                  placeholder="确认密码"
                  id="confirm_input"
              />
              <label for="confirm_input" class="absolute left-6 top-4 z-10 origin-[0] -translate-y-2.5 scale-75 transform text-sm text-text-muted duration-200 peer-placeholder-shown:translate-y-0 peer-placeholder-shown:scale-100 peer-focus:-translate-y-2.5 peer-focus:scale-75 cursor-text bg-surface px-1">
                确认密码
              </label>
            </div>
          </div>
        </transition>

        <button type="submit"
                class="w-full rounded-full bg-text text-surface py-4 text-base font-medium transition-transform active:scale-[0.98] hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60 mt-4 shadow-sm"
                :disabled="submitting">
          <span v-if="submitting" class="flex items-center justify-center gap-2">
            <svg class="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
            </svg>
            处理中...
          </span>
          <span v-else>{{ mode === 'login' ? '继续' : '创建账户' }}</span>
        </button>
      </form>

      <p class="mt-8 text-sm text-text-muted">
        {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
        <button type="button" class="font-medium text-primary hover:text-primary/80 hover:underline transition ml-1" @click="toggleMode">
          {{ mode === 'login' ? '注册' : '登录' }}
        </button>
      </p>

    </div>

    <div class="fixed bottom-6 right-6 z-50">
      <button type="button"
              class="flex h-12 w-12 items-center justify-center rounded-full bg-surface-container border border-outline/10 text-text shadow-lg hover:shadow-xl hover:bg-surface-variant transition-all duration-300"
              :aria-label="`切换主题，当前${themeLabel}`" @click="toggleTheme">
        <svg v-if="isDark" class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z" />
        </svg>
        <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 18a6 6 0 1 1 0-12 6 6 0 0 1 0 12zm0 4a1 1 0 0 1-1-1v-1.2a.8.8 0 0 1 1.6 0V21a1 1 0 0 1-1 1zm0-18a1 1 0 0 1-1-1V1.2a.8.8 0 0 1 1.6 0V3a1 1 0 0 1-1 1zm9 7h-1.2a.8.8 0 0 1 0-1.6H21a1 1 0 1 1 0 2zm-18 0H1a1 1 0 1 1 0-2h1.2a.8.8 0 1 1 0 1.6zM5.64 19.36a1 1 0 0 1-1.41-1.41l.85-.85a.8.8 0 0 1 1.13 1.13zm13.14-13.14-.85.85a.8.8 0 1 1-1.13-1.13l.85-.85a1 1 0 0 1 1.13 1.13zm0 13.14-1.13-1.13a.8.8 0 1 1 1.13-1.13l.85.85a1 1 0 0 1-1.41 1.41zM5.64 4.64 4.79 3.8A1 1 0 1 1 6.2 2.36l.85.85a.8.8 0 1 1-1.13 1.13z" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useThemeStore } from '../stores/theme';

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
    localError.value = '请输入邮箱/用户名和密码';
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
/* Social Button 样式
   改为 rounded-full (胶囊形)
*/
.social-btn {
  @apply flex w-full items-center justify-start gap-3 rounded-full border border-outline/30 bg-surface px-6 py-3.5 text-sm font-medium text-text transition-colors hover:bg-surface-variant/50 relative overflow-hidden;
}

/* 渐变过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
  max-height: 100px;
  opacity: 1;
}

.fade-enter-from,
.fade-leave-to {
  max-height: 0;
  opacity: 0;
  margin-top: 0;
  overflow: hidden;
}
</style>