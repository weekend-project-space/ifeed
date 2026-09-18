import { get } from './client';
import type { PageResponse } from '@/types/api';

export interface Category {
  id: string;
  name: string;
  icon: string;
  feedCount: number;
  description: string;
}

export interface CategoriesResponse {
  categories: Category[];
}

export interface DiscoveryFeed {
  feedId: string;
  name: string;
  description: string;
  url: string;
  siteUrl: string;
  favicon: string;
  category: string;
  categoryName: string;
  subscriberCount: number;
  articleCount: number;
  lastUpdated: string;
  updateFrequency: string;
  subscribed: boolean;
  featured: boolean;
}

export const getDiscoveryCategories = () =>
  get<CategoriesResponse>('/api/discovery/categories');

export const listDiscoveryFeeds = (params: {
  page: number;
  size: number;
  query?: string;
  category?: string;
  signal?: AbortSignal;
}) => {
  const { signal, query, ...pagination } = params;
  const endpoint = query?.trim()
    ? '/api/discovery/feeds/search'
    : '/api/discovery/feeds';

  return get<PageResponse<DiscoveryFeed>>(endpoint, {
    signal,
    query: {
      ...pagination,
      q: query?.trim() || undefined,
      category: params.category || undefined,
    },
  });
};
