import { defineStore } from 'pinia';

export type ThemeMode = 'light' | 'dark' | 'system';

const THEME_STORAGE_KEY = 'ifeed-theme-mode';
const MEDIA_QUERY = '(prefers-color-scheme: dark)';

let mediaQueryList: MediaQueryList | null = null;

const ensureMediaQuery = () => {
  if (typeof window === 'undefined') {
    return null;
  }
  if (!mediaQueryList) {
    mediaQueryList = window.matchMedia(MEDIA_QUERY);
  }
  return mediaQueryList;
};

const applyThemeClass = (mode: ThemeMode) => {
  if (typeof document === 'undefined') {
    return mode === 'dark';
  }
  const mq = ensureMediaQuery();
  const prefersDark = mq?.matches ?? false;
  const isDark = mode === 'dark' || (mode === 'system' && prefersDark);
  const root = document.documentElement;
  root.classList.toggle('dark', isDark);
  root.style.setProperty('color-scheme', isDark ? 'dark' : 'light');
  return isDark;
};

export const useThemeStore = defineStore('theme', {
  state: () => ({
    mode: 'system' as ThemeMode,
    isDark: false,
    initialized: false,
    cleanup: null as null | (() => void)
  }),
  actions: {
    init() {
      if (this.initialized) {
        return;
      }
      if (typeof window === 'undefined') {
        this.initialized = true;
        return;
      }
      const stored = localStorage.getItem(THEME_STORAGE_KEY) as ThemeMode | null;
      if (stored) {
        this.mode = stored;
      }
      this.apply();
      this.bindSystemListener();
      this.initialized = true;
    },
    apply() {
      this.isDark = applyThemeClass(this.mode);
    },
    setMode(mode: ThemeMode) {
      this.mode = mode;
      if (typeof window !== 'undefined') {
        localStorage.setItem(THEME_STORAGE_KEY, mode);
      }
      this.apply();
      this.bindSystemListener();
    },
    toggle() {
      const nextMode: ThemeMode = this.isDark ? 'light' : 'dark';
      this.setMode(nextMode);
    },
    bindSystemListener() {
      this.cleanup?.();
      if (this.mode !== 'system') {
        this.cleanup = null;
        return;
      }
      const mq = ensureMediaQuery();
      if (!mq) {
        return;
      }
      const handler = (_event: MediaQueryListEvent) => {
        if (this.mode === 'system') {
          this.isDark = applyThemeClass('system');
        }
      };
      mq.addEventListener('change', handler);
      this.cleanup = () => mq.removeEventListener('change', handler);
    }
  },
  getters: {
    label: (state) => {
      if (state.mode === 'system') {
        return '系统主题';
      }
      return state.mode === 'dark' ? '深色模式' : '浅色模式';
    }
  }
});
