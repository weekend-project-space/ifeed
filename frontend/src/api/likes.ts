import { del, get, post } from './client';
import type { PageResponse } from '@/types/api';

export interface LikeDto {
  articleId: string;
  title?: string;
  feedTitle?: string;
  thumbnail?: string;
  summary?: string;
  likedAt: string;
}

export const listLikes = (page: number, size: number, signal?: AbortSignal) =>
  get<PageResponse<LikeDto>>('/api/user/likes', { signal, query: { page, size, sort: 'likedAt,desc' } });

export const addLike = (articleId: string) =>
  post<{ articleId: string; liked: boolean; likedAt: string }>(`/api/user/likes/${encodeURIComponent(articleId)}`);

export const removeLike = (articleId: string) => del(`/api/user/likes/${encodeURIComponent(articleId)}`);
