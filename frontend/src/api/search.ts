import { get } from './client';
import type { PageResponse } from '@/types/api';

export type SearchType = 'keyword' | 'semantic';
export type Source = 'owner' | 'global';

export interface SearchResultDto {
  id: string;
  feedId?: string;
  title?: string;
  summary?: string;
  thumbnail?: string;
  feedTitle?: string;
  feedAvatar?: string;
  timeAgo?: string;
  score?: number;
}

export const searchArticles = (
  query: string,
  type: SearchType,
  source: Source | undefined,
  page: number,
  size: number,
  filters?: { feedId?: string | null; tags?: string | null; category?: string | null },
  signal?: AbortSignal,
) =>
  get<PageResponse<SearchResultDto>>('/api/search', {
    signal,
    query: { query, source, type, page, size, ...filters },
  });
