import { del, get, post } from './client';
import type { PageResponse } from '@/types/api';

export const COLLECTIONS_SORT = 'collectedAt,desc';

export interface CollectionDto {
  articleId: string;
  title?: string;
  feedTitle?: string;
  thumbnail?: string;
  summary?: string;
  collectedAt?: string;
}

export const listCollections = (page: number, size: number, signal?: AbortSignal) =>
  get<PageResponse<CollectionDto>>('/api/user/collections', {
    signal,
    query: { page, size, sort: COLLECTIONS_SORT },
  });

export const addCollection = (articleId: string) =>
  post(`/api/user/collections/${encodeURIComponent(articleId)}`);

export const removeCollection = (articleId: string) =>
  del(`/api/user/collections/${encodeURIComponent(articleId)}`);
