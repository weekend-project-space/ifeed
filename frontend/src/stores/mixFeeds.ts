import { defineStore } from 'pinia';
import { ref } from 'vue';
import * as api from '../api/client';

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

export const useMixFeedsStore = defineStore('mixFeeds', () => {
    const myMixFeeds = ref<MixFeedListResponse[]>([]);
    const publicMixFeeds = ref<MixFeedListResponse[]>([]);
    const currentMixFeed = ref<MixFeedDetailResponse | null>(null);
    const loading = ref(false);
    const error = ref<string | null>(null);

    const clearMyMixFeeds = ()=>myMixFeeds.value=[]

    const fetchMyMixFeeds = async () => {
        loading.value = true;
        error.value = null;
        try {
            const data = await api.get<MixFeedListResponse[]>('/api/mix-feeds');
            myMixFeeds.value = data;
        } catch (err: any) {
            error.value = err.message || 'Failed to fetch mix feeds';
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const createMixFeed = async (request: MixFeedRequest) => {
        loading.value = true;
        error.value = null;
        try {
            await api.post('/api/mix-feeds', request);
            await fetchMyMixFeeds();
        } catch (err: any) {
            error.value = err.message || 'Failed to create mix feed';
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const fetchMixFeedDetail = async (id: string) => {
        loading.value = true;
        error.value = null;
        try {
            const data = await api.get<MixFeedDetailResponse>(`/api/mix-feeds/${id}`);
            currentMixFeed.value = data;
            return data;
        } catch (err: any) {
            error.value = err.message || 'Failed to fetch mix feed detail';
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const updateMixFeed = async (id: string, request: MixFeedRequest) => {
        loading.value = true;
        error.value = null;
        try {
            await api.put(`/api/mix-feeds/${id}`, request);
            if (currentMixFeed.value && currentMixFeed.value.id === id) {
                await fetchMixFeedDetail(id);
            }
            await fetchMyMixFeeds();
        } catch (err: any) {
            error.value = err.message || 'Failed to update mix feed';
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const deleteMixFeed = async (id: string) => {
        loading.value = true;
        error.value = null;
        try {
            await api.del(`/api/mix-feeds/${id}`);
            myMixFeeds.value = myMixFeeds.value.filter((feed) => feed.id !== id);
            if (currentMixFeed.value && currentMixFeed.value.id === id) {
                currentMixFeed.value = null;
            }
        } catch (err: any) {
            error.value = err.message || 'Failed to delete mix feed';
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const fetchPublicMixFeeds = async (page = 0, size = 20) => {
        loading.value = true;
        error.value = null;
        try {
            const data = await api.get<{ content: MixFeedListResponse[] }>('/api/mix-feeds/public', {
                query: { page, size }
            });
            publicMixFeeds.value = data.content;
            return data;
        } catch (err: any) {
            error.value = err.message || 'Failed to fetch public mix feeds';
            throw err;
        } finally {
            loading.value = false;
        }
    };

    return {
        myMixFeeds,
        publicMixFeeds,
        currentMixFeed,
        loading,
        error,
        fetchMyMixFeeds,
        clearMyMixFeeds,
        createMixFeed,
        fetchMixFeedDetail,
        updateMixFeed,
        deleteMixFeed,
        fetchPublicMixFeeds
    };
});
