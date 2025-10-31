import { ref } from 'vue';
import { defineStore } from 'pinia';
import { request } from '../api/client';

export interface SubscriptionBaseDto {
  feedId: string;
  title?: string;
  url: string;
  siteUrl?: string;
  avatar?: string;
  lastFetched?: string;
  lastUpdated?: string;
  failureCount?: number;
  fetchError?: string | null;
}

export interface SubscriptionListItemDto extends SubscriptionBaseDto {
  isRead?: boolean;
}

export interface SubscriptionSearchResultDto extends SubscriptionBaseDto {
  latestPublishedAt?: string;
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

export const useSubscriptionsStore = defineStore('subscriptions', () => {
  const items = ref<SubscriptionListItemDto[]>([]);
  const loading = ref(false);
  const submitting = ref(false);
  const error = ref<string | null>(null);
  const searchResults = ref<SubscriptionSearchResultDto[]>([]);
  const searchLoading = ref(false);
  const searchError = ref<string | null>(null);
  const activeSearchQuery = ref('');

  const fetchSubscriptions = async () => {
    loading.value = true;
    error.value = null;
    try {
      const response = await request<SubscriptionListItemDto[]>('/api/subscriptions');
      items.value = Array.isArray(response)
        ? response.map((item) => ({
          ...item,
          siteUrl: item.siteUrl ?? item.url,
          isRead: item.isRead ?? false,
          failureCount: item.failureCount ?? 0,
          fetchError: item.fetchError ?? null
        }))
        : [];
    } catch (err) {
      const message = err instanceof Error ? err.message : '订阅列表加载失败';
      error.value = message;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const addSubscription = async (feedUrl: string) => {
    if (!feedUrl) {
      error.value = '请输入有效的订阅链接';
      return;
    }
    submitting.value = true;
    error.value = null;
    try {
      await request('/api/subscriptions', {
        method: 'POST',
        json: { feedUrl }
      });
      await fetchSubscriptions();
    } catch (err) {
      const message = err instanceof Error ? err.message : '添加订阅失败';
      error.value = message;
      throw err;
    } finally {
      submitting.value = false;
    }
  };

  const removeSubscription = async (feedId: string) => {
    submitting.value = true;
    error.value = null;
    try {
      await request(`/api/subscriptions/${feedId}`, { method: 'DELETE' });
      items.value = items.value.filter((item) => item.feedId !== feedId);
    } catch (err) {
      const message = err instanceof Error ? err.message : '取消订阅失败';
      error.value = message;
      throw err;
    } finally {
      submitting.value = false;
    }
  };

  const searchSubscriptions = async (query: string) => {
    const trimmed = query.trim();
    if (!trimmed) {
      searchResults.value = [];
      searchError.value = '请输入关键词、URL 或标题';
      activeSearchQuery.value = '';
      searchLoading.value = false;
      return;
    }
    activeSearchQuery.value = trimmed;
    searchLoading.value = true;
    searchError.value = null;
    try {
      const response = await request<SubscriptionSearchResultDto[]>('/api/subscriptions/search', {
        query: { query: trimmed }
      });
      if (activeSearchQuery.value !== trimmed) {
        return;
      }
      searchResults.value = Array.isArray(response)
        ? response.map((item) => ({
          ...item,
          siteUrl: item.siteUrl ?? item.url,
          subscriberCount: item.subscriberCount ?? 0,
          subscribed: item.subscribed ?? false,
          failureCount: item.failureCount ?? 0,
          fetchError: item.fetchError ?? null
        }))
        : [];
    } catch (err) {
      if (activeSearchQuery.value !== trimmed) {
        return;
      }
      const message = err instanceof Error ? err.message : '搜索订阅失败';
      searchError.value = message;
      throw err;
    } finally {
      if (activeSearchQuery.value === trimmed) {
        searchLoading.value = false;
      }
    }
  };

  const clearSearchResults = () => {
    searchResults.value = [];
    searchError.value = null;
    searchLoading.value = false;
    activeSearchQuery.value = '';
  };

  const previewOpmlImport = async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return await request<OpmlPreviewResultDto>('/api/subscriptions/opml/preview', {
      method: 'POST',
      body: formData
    });
  };

  const confirmOpmlImport = async (feeds: {
    feedUrl: string;
    title: string;
    siteUrl: string;
    avatar?: string | null;
    selected: boolean;
  }[]) => {
    return await request<OpmlImportResultDto>('/api/subscriptions/opml/confirm', {
      method: 'POST',
      json: {
        feeds
      }
    });
  };

  return {
    items,
    loading,
    submitting,
    error,
    searchResults,
    searchLoading,
    searchError,
    fetchSubscriptions,
    addSubscription,
    removeSubscription,
    searchSubscriptions,
    clearSearchResults,
    previewOpmlImport,
    confirmOpmlImport
  };
});
