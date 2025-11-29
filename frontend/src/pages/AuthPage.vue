<template>
  <div class="min-h-screen w-full bg-surface text-text transition-colors duration-300 flex flex-col items-center justify-center p-4">
    
    <!-- 左上角 Logo -->
    <div class="fixed top-6 left-6 z-50 flex items-center gap-3">

      <h2 class="text-xl font-bold text-text tracking-tight">IFeed</h2>
    </div>
    
    <div class="w-full max-w-[360px] flex flex-col items-center">
      
      <!-- 标题区域 -->
      <div class="mb-8 text-center">
        <h1 class="text-[32px] font-semibold text-text mb-3 tracking-tight">登录或注册</h1>
        <p class="text-text-secondary text-[15px] leading-relaxed">
          智能聚合，精准阅读
        </p>
      </div>

      <!-- OAuth 登录按钮 -->
      <div class="w-full space-y-2 mb-4">
        <button type="button" class="oauth-btn" @click="handleLinuxDoLogin" :disabled="submitting">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <clipPath id="circle-clip">
                <circle cx="12" cy="12" r="9"/>
              </clipPath>
            </defs>
            <!-- 整个圆形作为容器 -->
            <g clip-path="url(#circle-clip)">
              <rect x="3" y="3" width="18" height="6" fill="#000000"/>
              <rect x="3" y="9" width="18" height="6" fill="#FFFFFF"/>
              <rect x="3" y="15" width="18" height="6" fill="#FFA500"/>
            </g>
            <!-- 圆形边框 -->
            <circle cx="12" cy="12" r="9" fill="none" stroke="#E5E7EB" stroke-width="0.5"/>
          </svg>
          <span>继续使用 Linux DO 登录</span>
        </button>
      </div>

      <!-- 分隔线 -->
      <div class="relative w-full my-6 text-center">
        <div class="absolute inset-0 flex items-center">
          <div class="w-full border-t border-outline/20"></div>
        </div>
        <div class="relative inline-flex bg-surface px-3 text-xs text-text-muted">
          或
        </div>
      </div>

      <!-- 错误提示 -->
      <transition name="fade">
        <div v-if="errorMessage" class="w-full flex items-start gap-2 text-danger text-sm bg-danger/5 px-4 py-3 rounded-xl border border-danger/10 mb-4">
          <svg class="w-4 h-4 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>
      </transition>

      <!-- 登录表单 -->
      <form class="w-full space-y-3" @submit.prevent="handleSubmit">
        <!-- 邮箱/用户名输入 -->
        <div>
          <input
            v-model.trim="form.username"
            type="text"
            required
            class="input-field"
            placeholder="电子邮箱地址"
            id="email_input"
          />
        </div>

        <!-- 密码输入 - 登录和注册都显示 -->
        <div>
          <input
            v-model="form.password"
            type="password"
            required
            class="input-field"
            placeholder="密码"
            id="password_input"
          />
        </div>

        <!-- 注册模式的确认密码 -->
        <transition name="fade">
          <div v-if="mode === 'register'">
            <input
              v-model="form.confirmPassword"
              type="password"
              required
              class="input-field"
              placeholder="确认密码"
              id="confirm_input"
            />
          </div>
        </transition>

        <!-- 提交按钮 -->
        <button 
          type="submit"
          class="submit-btn"
          :disabled="submitting"
        >
          <span v-if="submitting" class="flex items-center justify-center gap-2">
            <svg class="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
            </svg>
            处理中...
          </span>
          <span v-else>继续</span>
        </button>
      </form>

      <!-- 底部切换链接 -->
      <div class="mt-6 text-center text-sm">
        <span class="text-text-muted">{{ mode === 'login' ? '还没有账号？' : '已有账号？' }}</span>
        <button 
          type="button" 
          class="text-primary hover:underline ml-1 font-medium"
          @click="toggleMode"
        >
          {{ mode === 'login' ? '注册' : '登录' }}
        </button>
      </div>

    </div>

    <!-- 主题切换按钮 -->
    <div class="fixed bottom-6 right-6 z-50">
      <button 
        type="button"
        class="flex h-11 w-11 items-center justify-center rounded-full bg-surface-container border border-outline/10 text-text shadow-md hover:shadow-lg hover:bg-surface-variant transition-all duration-200"
        :aria-label="`切换主题，当前${themeLabel}`" 
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
import { computed, onMounted, reactive, ref, watch } from 'vue';
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

const handleLinuxDoLogin = async () => {
  try {
    submitting.value = true;
    localError.value = '';
    const authUrl = await authStore.getLinuxDoAuthUrl();
    // 重定向到 Linux.do 授权页面
    window.location.href = authUrl;
  } catch (err) {
    const message = err instanceof Error && err.message ? err.message : error.value || '获取授权地址失败';
    localError.value = message;
    submitting.value = false;
  }
};

// 处理 OAuth 回调
onMounted(async () => {
  const code = route.query.code as string | undefined;
  if (code) {
    try {
      submitting.value = true;
      localError.value = '';
      await authStore.linuxDoLogin(code);
      // 清除 URL 中的 code 参数并跳转
      await redirectToTarget();
    } catch (err) {
      const message = err instanceof Error && err.message ? err.message : error.value || 'Linux.do 登录失败';
      localError.value = message;
      submitting.value = false;
    }
  }
});

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
/* OAuth 按钮样式 - 圆角胶囊形 */
.oauth-btn {
  @apply flex w-full items-center justify-start gap-3 rounded-full border border-outline/30 bg-surface px-4 py-3 text-[15px] font-medium text-text transition-all hover:bg-surface-variant/50 disabled:opacity-50 disabled:cursor-not-allowed;
}

/* 输入框样式 - 圆角胶囊形 */
.input-field {
  @apply block w-full rounded-full border border-outline/30 bg-surface px-5 py-3 text-[15px] text-text placeholder:text-text-muted/60 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/20 transition-all;
}

/* 提交按钮样式 - 圆角胶囊形 */
.submit-btn {
  @apply w-full rounded-full bg-text text-surface py-3 text-[15px] font-semibold transition-all hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 mt-2;
}

/* 渐变过渡动画 */
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