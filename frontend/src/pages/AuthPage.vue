<template>
  <div class="flex min-h-screen w-full flex-col items-center justify-center bg-surface p-4 text-text transition-colors duration-300">
    <!-- 左上角 Logo 标识 -->
    <div class="fixed top-6 left-6 z-50 flex items-center gap-2.5">
      <h2 class="text-xl font-bold tracking-tight text-text">IFeed</h2>
    </div>
    <LanguageSwitcher class="fixed right-6 top-6 z-50" />

    <div class="w-full max-w-[360px] flex flex-col items-center">
      <!-- 标题区域 -->
      <div class="mb-8 text-center">
        <h1 class="text-3xl font-normal tracking-tight text-text mb-2">{{ tr('authPage.title') }}</h1>
        <p class="text-xs text-text-secondary sm:text-sm">
          {{ tr('authPage.subtitle') }}
        </p>
      </div>

      <!-- 错误提示 -->
      <transition name="fade">
        <div v-if="errorMessage" class="mb-4 flex w-full items-start gap-2 rounded-2xl bg-red-500/10 p-3.5 text-xs text-red-600 dark:text-red-400">
          <svg class="h-4 w-4 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>
      </transition>

      <!-- 登录/注册表单：MD3 填充输入框体系 -->
      <form class="w-full space-y-3.5" @submit.prevent="handleSubmit">
        <!-- 账号输入 -->
        <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-2.5 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface-container/80">
          <label for="email_input" class="block text-[11px] font-medium text-text-secondary">{{ tr('authPage.account') }}</label>
          <input
              v-model.trim="form.username"
              type="text"
              required
              :placeholder="tr('authPage.accountPlaceholder')"
              id="email_input"
              class="mt-0.5 block w-full border-0 bg-transparent p-0 text-sm text-text outline-none ring-0 placeholder:text-text-secondary/60"
          />
        </div>

        <!-- 密码输入 -->
        <div class="rounded-t-2xl bg-surface-container/60 px-4 pt-2.5 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface-container/80">
          <label for="password_input" class="block text-[11px] font-medium text-text-secondary">{{ tr('authPage.password') }}</label>
          <input
              v-model="form.password"
              type="password"
              required
              :placeholder="tr('authPage.passwordPlaceholder')"
              id="password_input"
              class="mt-0.5 block w-full border-0 bg-transparent p-0 text-sm text-text outline-none ring-0 placeholder:text-text-secondary/60"
          />
        </div>

        <!-- 确认密码（注册模式） -->
        <transition name="fade">
          <div v-if="mode === 'register'" class="rounded-t-2xl bg-surface-container/60 px-4 pt-2.5 pb-2 border-b-2 border-outline/40 transition focus-within:border-primary focus-within:bg-surface-container/80">
            <label for="confirm_input" class="block text-[11px] font-medium text-text-secondary">{{ tr('authPage.confirmPassword') }}</label>
            <input
                v-model="form.confirmPassword"
                type="password"
                required
                :placeholder="tr('authPage.confirmPasswordPlaceholder')"
                id="confirm_input"
                class="mt-0.5 block w-full border-0 bg-transparent p-0 text-sm text-text outline-none ring-0 placeholder:text-text-secondary/60"
            />
          </div>
        </transition>

        <!-- 提交按钮：MD3 Filled Button -->
        <button
            type="submit"
            class="mt-2 inline-flex h-11 w-full items-center justify-center rounded-full bg-primary px-5 text-sm font-semibold text-white shadow-xs transition hover:brightness-105 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="submitting"
        >
          <span v-if="submitting" class="flex items-center justify-center gap-2">
            <svg class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
            </svg>
            {{ tr('common.processing') }}
          </span>
          <span v-else>{{ mode === 'login' ? tr('authPage.continueLogin') : tr('authPage.register') }}</span>
        </button>
      </form>

      <!-- 模式切换 -->
      <div class="mt-6 text-center text-xs text-text-secondary">
        <span>{{ mode === 'login' ? tr('authPage.noAccount') : tr('authPage.hasAccount') }}</span>
        <button
            type="button"
            class="ml-1.5 font-semibold text-primary hover:underline"
            @click="toggleMode"
        >
          {{ mode === 'login' ? tr('authPage.newAccount') : tr('authPage.signIn') }}
        </button>
      </div>
    </div>

    <!-- 底部悬浮主题切换药丸 -->
    <div class="fixed bottom-6 right-6 z-50">
      <button
          type="button"
          class="flex h-11 w-11 items-center justify-center rounded-full bg-surface-container text-text shadow-sm transition hover:bg-surface-container-high active:scale-95"
          :aria-label="tr('authPage.switchTheme', { value0: themeLabel })"
          @click="toggleTheme"
      >
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
import { translate as tr } from '@/i18n';
import LanguageSwitcher from '@/components/LanguageSwitcher.vue';
import { useDocumentTitle } from '@/composables/documentTitle';
import type { MessageKey } from '@/i18n/schema';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useThemeStore } from '../stores/theme';

type Mode = 'login' | 'register';

defineOptions({ name: 'AuthPage' });
useDocumentTitle(() => tr('authPage.title'));

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
const validationError = ref<MessageKey | null>(null);

const redirectToTarget = async () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/';
  await router.replace(redirect);
};

const handleSubmit = async () => {
  validationError.value = null;
  if (!form.username || !form.password) {
    validationError.value = 'authPage.credentialsRequired';
    return;
  }
  if (mode.value === 'register' && form.password !== form.confirmPassword) {
    validationError.value = 'authPage.passwordMismatch';
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
    const message = err instanceof Error && err.message ? err.message : error.value || tr('authPage.operationFailed');
    localError.value = message;
  } finally {
    submitting.value = false;
  }
};

const toggleMode = () => {
  switchMode(mode.value === 'login' ? 'register' : 'login');
};

const switchMode = (target: Mode) => {
  if (mode.value === target) return;
  mode.value = target;
  validationError.value = null;
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

const errorMessage = computed(() => validationError.value ? tr(validationError.value) : localError.value || error.value || '');
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: all 0.2s ease;
  max-height: 200px;
  opacity: 1;
}

.fade-enter-from,
.fade-leave-to {
  max-height: 0;
  opacity: 0;
  overflow: hidden;
}
</style>
