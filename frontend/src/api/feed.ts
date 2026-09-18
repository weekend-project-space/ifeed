import { get } from './client';
import type { PageResponse } from '@/types/api';
import type { ArticleDto } from './articles';

export interface FeedDetail {
  feedId: string;
  title?: string | null;
  description: string | null;
  url: string;
  siteUrl?: string | null;
  avatar?: string | null;
  lastFetched?: string | null;
  lastUpdated?: string | null;
  articleCount: number;
  subscriberCount: number;
  subscribed: boolean;
  failureCount?: number;
  fetchError?: string | null;
  sources: string[] | null;
}

export const getFeed = (feedId: string, signal?: AbortSignal) =>
  get<FeedDetail>(`/api/feeds/${encodeURIComponent(feedId)}`, { signal });

export const listFeedArticles = (
  feedId: string,
  page: number,
  size: number,
  tag?: string | null,
  signal?: AbortSignal,
) =>
  get<PageResponse<ArticleDto>>('/api/articles', {
    signal,
    query: {
      page,
      size,
      sort: 'publishedAt,desc',
      feedId,
      tags: tag ?? undefined,
    },
  });
