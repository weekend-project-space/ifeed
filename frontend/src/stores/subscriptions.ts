import { ref } from 'vue';
import { defineStore } from 'pinia';
import {
    listSubscriptions,
    searchSubscriptions as searchSubscriptionsRequest,
    type SubscriptionListItemDto,
    type SubscriptionSearchResultDto,
} from '@/api/subscriptions';

export type {
    SubscriptionListItemDto,
    SubscriptionSearchResultDto,
} from '@/api/subscriptions';

export const useSubscriptionsStore = defineStore('subscriptions', () => {
    const items = ref<SubscriptionListItemDto[]>([]);
    const loading = ref(false);
    const error = ref<string | null>(null);
    const searchResults = ref<SubscriptionSearchResultDto[]>([]);
    const searchLoading = ref(false);
    const searchError = ref<string | null>(null);
    const activeSearchQuery = ref('');

    const fetchSubscriptions = async () => {
        loading.value = true;
        error.value = null;
        try {
            const response = await listSubscriptions();
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
            const response = await searchSubscriptionsRequest(trimmed);
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

    return {
        items,
        loading,
        error,
        searchResults,
        searchLoading,
        searchError,
        fetchSubscriptions,
        searchSubscriptions,
        clearSearchResults
    };
});
