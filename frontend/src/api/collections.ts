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
  folderId: string | null;
}

export interface CollectionState {
  articleId: string;
  folderId: string | null;
  collectedAt: string;
}

export const listCollections = (page: number, size: number, signal?: AbortSignal, folderId?: string) =>
  get<PageResponse<CollectionDto>>('/api/user/collections', {
    signal,
    query: { page, size, sort: COLLECTIONS_SORT, folderId },
  });

export const addCollection = (articleId: string, folderId?: string | null) =>
  post<CollectionState>(`/api/user/collections/${encodeURIComponent(articleId)}`,
    folderId === undefined ? undefined : { folderId });

export const removeCollection = (articleId: string) =>
  del(`/api/user/collections/${encodeURIComponent(articleId)}`);
