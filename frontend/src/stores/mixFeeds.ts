import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
    createMixFeed as createMixFeedRequest,
    getMixFeed,
    listMyMixFeeds,
    listPublicMixFeeds,
    removeMixFeed,
    updateMixFeed as updateMixFeedRequest,
    type MixFeedDetailResponse,
    type MixFeedListResponse,
    type MixFeedRequest,
} from '@/api/mixFeeds';

export type {
    DateRange,
    KeywordFilter,
    MixFeedDetailResponse,
    MixFeedFilterConfig,
    MixFeedListResponse,
    MixFeedRequest,
} from '@/api/mixFeeds';

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
            const data = await listMyMixFeeds();
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
            await createMixFeedRequest(request);
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
            const data = await getMixFeed(id);
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
            await updateMixFeedRequest(id, request);
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
            await removeMixFeed(id);
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
            const data = await listPublicMixFeeds(page, size);
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
