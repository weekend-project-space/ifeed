import { get } from './client';
import type { PageResponse } from '@/types/api';
import type { ArticleDto } from './articles';

export const listRecommendations = (page: number, size: number, signal?: AbortSignal) =>
  get<PageResponse<ArticleDto>>('/api/articles/recommendations', {
    signal,
    query: { page, size },
  });
