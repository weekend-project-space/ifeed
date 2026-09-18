import { ref } from 'vue';
import { defineStore } from 'pinia';
import {
    addSubscription as addSubscriptionRequest,
    confirmOpmlImport as confirmOpmlImportRequest,
    listSubscriptions,
    previewOpmlImport as previewOpmlImportRequest,
    removeSubscription as removeSubscriptionRequest,
    searchSubscriptions as searchSubscriptionsRequest,
    type SubscriptionBaseDto,
    type SubscriptionListItemDto,
    type SubscriptionSearchResultDto,
} from '@/api/subscriptions';

export type {
    OpmlImportResultDto,
    OpmlPreviewFeedDto,
    OpmlPreviewResultDto,
    SubscriptionBaseDto,
    SubscriptionListItemDto,
    SubscriptionSearchResultDto,
} from '@/api/subscriptions';

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

    const addSubscription = async (feedUrl: string, feedId: string) => {
        if (!feedUrl && !feedId) {
            error.value = '请输入有效的订阅链接';
            return;
        }
        submitting.value = true;
        error.value = null;
        try {
            await addSubscriptionRequest(feedUrl, feedId);
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
            await removeSubscriptionRequest(feedId);
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

    const previewOpmlImport = async (file: File) => {
        return await previewOpmlImportRequest(file);
    };

    const confirmOpmlImport = async (feeds: {
        feedUrl: string;
        title: string;
        siteUrl: string;
        avatar?: string | null;
        selected: boolean;
    }[]) => {
        return await confirmOpmlImportRequest(feeds);
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
