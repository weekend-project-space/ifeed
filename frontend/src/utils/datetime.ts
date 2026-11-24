import { i18n, translate as tr } from '@/i18n';

export const formatRelativeTime = (value?: string | number | Date | null) => {
  const locale = i18n.global.locale.value === 'zh-CN' ? 'zh-CN' : 'en';
  if (!value) {
    return tr('common.justNow');
  }
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return tr('common.unknownDate');
  }

  const diffMs = Date.now() - date.getTime();
  const diffMinutes = Math.floor(diffMs / 60000);
  if (diffMinutes < 1) return tr('common.justNow');
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });
  if (diffMinutes < 60) return rtf.format(-diffMinutes, 'minute');
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return rtf.format(-diffHours, 'hour');
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return rtf.format(-diffDays, 'day');
  return date.toLocaleDateString(locale);
};
