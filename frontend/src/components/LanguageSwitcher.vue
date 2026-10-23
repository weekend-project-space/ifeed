<template>
  <select :value="locale" :aria-label="t('common.language')"
          class="rounded-full border border-outline/20 bg-surface px-3 py-1.5 pr-8 text-xs text-text focus:border-primary focus:ring-primary/20"
          @change="changeLanguage">
    <option value="zh-CN">{{ t('common.chinese') }}</option>
    <option value="en">{{ t('common.english') }}</option>
  </select>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { setLocale } from '@/i18n';

const { t, locale } = useI18n();
const changeLanguage = (event: Event) => {
  const value = (event.target as HTMLSelectElement).value;
  if (value === 'zh-CN' || value === 'en') setLocale(value);
};
</script>
