import { del, get, type RequestOptions } from './client';
import type { PageResponse } from '@/types/api';

export const HISTORY_SORT = 'readAt,desc';

export interface HistoryEntryDto {
  articleId: string;
  title?: string;
  feedTitle?: string;
  thumbnail?: string;
  summary?: string;
  readAt?: string;
}

export const listHistory = (page: number, size: number, signal?: AbortSignal) =>
  get<PageResponse<HistoryEntryDto>>('/api/user/history', {
    signal,
    query: { page, size, sort: HISTORY_SORT },
  });

export const removeHistory = (articleId: string, options?: Pick<RequestOptions, 'signal'>) =>
  del(`/api/user/history/${encodeURIComponent(articleId)}`, options);
