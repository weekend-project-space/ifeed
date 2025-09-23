<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex items-center justify-center p-6">
    <div class="w-full max-w-md bg-white/95 backdrop-blur rounded-3xl shadow-xl overflow-hidden">
      <div class="px-8 py-6 border-b border-slate-100">
        <h1 class="text-2xl font-semibold text-slate-900">欢迎使用 iFeed</h1>
        <p class="text-sm text-slate-500 mt-1">AI 驱动的 RSS 阅读体验</p>
      </div>
      <div class="px-8 py-6">
        <div class="flex bg-slate-100 rounded-full p-1 text-sm font-medium text-slate-600 mb-6">
          <button
            type="button"
            class="flex-1 py-2 rounded-full transition"
            :class="mode === 'login' ? 'bg-white shadow text-slate-900' : ''"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            type="button"
            class="flex-1 py-2 rounded-full transition"
            :class="mode === 'register' ? 'bg-white shadow text-slate-900' : ''"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>
        <form class="space-y-4" @submit.prevent="handleSubmit">
          <div>
            <label class="block text-sm font-medium text-slate-500">用户名</label>
            <input
              v-model.trim="form.username"
              type="text"
              autocomplete="username"
              required
              class="mt-1 block w-full rounded-xl border-slate-200 focus:border-primary focus:ring-primary/40"
              placeholder="输入用户名"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-500">密码</label>
            <input
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              required
              class="mt-1 block w-full rounded-xl border-slate-200 focus:border-primary focus:ring-primary/40"
              placeholder="输入密码"
            />
          </div>
          <transition name="fade">
            <div v-if="mode === 'register'">
              <label class="block text-sm font-medium text-slate-500">确认密码</label>
              <input
                v-model="form.confirmPassword"
                type="password"
                autocomplete="new-password"
                required
                class="mt-1 block w-full rounded-xl border-slate-200 focus:border-primary focus:ring-primary/40"
                placeholder="再次输入密码"
              />
            </div>
          </transition>

          <p v-if="errorMessage" class="text-sm text-red-500 bg-red-50 border border-red-100 rounded-xl px-4 py-2">
            {{ errorMessage }}
          </p>

          <button
            type="submit"
            class="w-full py-3 rounded-xl bg-primary text-primary-foreground font-semibold shadow hover:bg-blue-600 transition disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="submitting"
          >
            <span v-if="submitting" class="inline-flex items-center justify-center gap-2">
              <svg class="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
              </svg>
              处理中...
            </span>
            <span v-else>{{ mode === 'login' ? '登录' : '创建账户' }}</span>
          </button>
        </form>
        <p class="text-xs text-center text-slate-400 mt-6">
          还没有账号？
          <button type="button" class="text-primary" @click="toggleMode">
            {{ mode === 'login' ? '注册新账户' : '返回登录' }}
          </button>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';

type Mode = 'login' | 'register';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const { error, isAuthenticated } = storeToRefs(authStore);

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
