import { del, get, post, put } from './client';

export interface KeywordFilter {
  include?: string[];
  exclude?: string[];
}

export interface DateRange {
  from?: string;
  to?: string;
}

export interface MixFeedFilterConfig {
  sourceFeeds?: Record<string, string>;
  keywords?: KeywordFilter;
  dateRange?: DateRange;
  sortBy?: string;
  sortOrder?: string;
}

export interface MixFeedRequest {
  name: string;
  description?: string;
  icon?: string;
  isPublic?: boolean;
  filterConfig?: MixFeedFilterConfig;
}

export interface MixFeedListResponse {
  id: string;
  name: string;
  description?: string;
  icon?: string;
  subscriberCount: number;
  isPublic: boolean;
  createdAt: string;
}

export interface MixFeedDetailResponse {
  id: string;
  name: string;
  description?: string;
  icon?: string;
  subscriberCount: number;
  articleCount: number;
  subscribed: boolean;
  filterConfig?: MixFeedFilterConfig;
  isPublic: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface MixFeedPageResponse {
  content: MixFeedListResponse[];
}

export const listMyMixFeeds = () =>
  get<MixFeedListResponse[]>('/api/mix-feeds');

export const createMixFeed = (payload: MixFeedRequest) =>
  post('/api/mix-feeds', payload);

export const getMixFeed = (id: string) =>
  get<MixFeedDetailResponse>(`/api/mix-feeds/${encodeURIComponent(id)}`);

export const updateMixFeed = (id: string, payload: MixFeedRequest) =>
  put(`/api/mix-feeds/${encodeURIComponent(id)}`, payload);

export const removeMixFeed = (id: string) =>
  del(`/api/mix-feeds/${encodeURIComponent(id)}`);

export const listPublicMixFeeds = (page = 0, size = 20) =>
  get<MixFeedPageResponse>('/api/mix-feeds/public', { query: { page, size } });
