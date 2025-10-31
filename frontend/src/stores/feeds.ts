import {ref} from 'vue';
import {defineStore} from 'pinia';
import {request} from '../api/client';

export interface FeedDetail {
    feedId: string;
    title?: string | null;
    url: string;
    siteUrl?: string | null;
    avatar?: string | null;
    lastFetched?: string | null;
    lastUpdated?: string | null;
    latestPublishedAt?: string | null;
    articleCount: number;
    subscriberCount: number;
    subscribed: boolean;
    failureCount?: number;
    fetchError?: string | null;
}

export const useFeedStore = defineStore('feed', () => {
    const detail = ref<FeedDetail | null>(null);
    const loading = ref(false);
    const error = ref<string | null>(null);

    const fetchById = async (feedId: string) => {
        if (!feedId) {
            error.value = '订阅源不存在';
            return null;
        }
        loading.value = true;
        error.value = null;
        try {
            const response = await request<FeedDetail>(`/api/feeds/${feedId}`);
            detail.value = response;
            return response;
        } catch (err) {
            const message = err instanceof Error ? err.message : '订阅源信息加载失败';
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const lookupByUrl = async (feedUrl: string) => {
        if (!feedUrl) {
            error.value = '请输入订阅链接';
            return null;
        }
        loading.value = true;
        error.value = null;
        try {
            const response = await request<FeedDetail>('/api/feeds/lookup', {
                query: {
                    feedUrl
                }
            });
            detail.value = response;
            return response;
        } catch (err) {
            const message = err instanceof Error ? err.message : '订阅源信息加载失败';
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const setSubscribed = (value: boolean) => {
        if (!detail.value) {
            return;
        }
        detail.value = {
            ...detail.value,
            subscribed: value
        };
    };

    const clear = () => {
        detail.value = null;
        error.value = null;
    };

    return {
        detail,
        loading,
        error,
        fetchById,
        lookupByUrl,
        setSubscribed,
        clear
    };
});
