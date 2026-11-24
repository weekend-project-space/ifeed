import { del, get, post, request } from './client';

export interface SubscriptionBaseDto {
  feedId: string;
  title?: string;
  description?: string;
  url?: string | null;
  siteUrl?: string | null;
  avatar?: string;
  icon?: string | null;
  type?: 'FEED' | 'MIX_FEED';
  lastFetched?: string;
  lastUpdated?: string;
  failureCount?: number;
  fetchError?: string | null;
}

export interface SubscriptionListItemDto extends SubscriptionBaseDto {
  isRead?: boolean;
}

export interface SubscriptionSearchResultDto extends SubscriptionBaseDto {
  subscriberCount: number;
  subscribed: boolean;
}

export interface OpmlPreviewFeedDto {
  feedUrl: string;
  title: string;
  siteUrl: string;
  avatar?: string | null;
  alreadySubscribed: boolean;
  errors: string[];
}

export interface OpmlPreviewResultDto {
  feeds: OpmlPreviewFeedDto[];
  warnings: string[];
  remainingQuota: number;
}

export interface OpmlImportSkippedDto {
  feedUrl: string | null;
  reason: string;
}

export interface OpmlImportResultDto {
  importedCount: number;
  skipped: OpmlImportSkippedDto[];
  message: string;
}

export interface OpmlImportFeed {
  feedUrl: string;
  title: string;
  siteUrl: string;
  avatar?: string | null;
  selected: boolean;
}

export const listSubscriptions = () =>
  get<SubscriptionListItemDto[]>('/api/subscriptions');

export const addSubscription = (feedUrl: string, feedId: string) =>
  post('/api/subscriptions', { feedUrl, feedId });

export const removeSubscription = (feedId: string) =>
  del(`/api/subscriptions/${encodeURIComponent(feedId)}`);

export const searchSubscriptions = (query: string) =>
  get<SubscriptionSearchResultDto[]>('/api/feeds/search', { query: { query } });

export const previewOpmlImport = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return request<OpmlPreviewResultDto>('/api/subscriptions/opml/preview', {
    method: 'POST',
    body: formData,
  });
};

export const confirmOpmlImport = (feeds: OpmlImportFeed[]) =>
  post<OpmlImportResultDto>('/api/subscriptions/opml/confirm', { feeds });
