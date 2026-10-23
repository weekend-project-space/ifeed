import { createI18n } from 'vue-i18n';
import { watch } from 'vue';
import zhCN from './locales/zh-CN';
import en from './locales/en';
import type { MessageKey } from './schema';

export type Locale = 'zh-CN' | 'en';
const STORAGE_KEY = 'ifeed-locale';
const getInitialLocale = (): Locale => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'zh-CN' || saved === 'en') return saved;
  } catch { /* Use browser preferences when storage is unavailable. */ }
  return navigator.language.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en';
};

export const i18n = createI18n({ legacy: false, locale: getInitialLocale(), fallbackLocale: 'zh-CN', messages: { 'zh-CN': zhCN, en } });
// Shared by stores, render functions and components outside the main app tree.
export const translate = (key: MessageKey, values: Record<string, unknown> = {}) => i18n.global.t(key, values);
export const setLocale = (locale: Locale) => {
  i18n.global.locale.value = locale;
  try { localStorage.setItem(STORAGE_KEY, locale); } catch { /* Switching still works without storage. */ }
};

watch(i18n.global.locale, locale => {
  document.documentElement.lang = locale;
  document.querySelector('meta[property="og:locale"]')?.setAttribute('content', locale === 'zh-CN' ? 'zh_CN' : 'en_US');
  document.querySelector('meta[name="dc.language"]')?.setAttribute('content', locale);
  for (const name of ['description', 'dc.description', 'twitter:description']) {
    document.querySelector(`meta[name="${name}"]`)?.setAttribute('content', translate('common.appDescription'));
  }
}, { immediate: true, flush: 'sync' });
