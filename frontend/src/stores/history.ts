import {computed, ref} from 'vue';
import {defineStore} from 'pinia';
import {request} from '../api/client';
import type {PageResponse} from '../types/api';

export interface HistoryEntryDto {
    articleId: string;
    title?: string;
    feedTitle?: string;
    thumbnail?: string;
    summary?: string;
    readAt?: string;
}

export const useHistoryStore = defineStore('history', () => {
    const items = ref<HistoryEntryDto[]>([]);
    const loading = ref(false);
    const error = ref<string | null>(null);
    const page = ref(1);
    const size = ref(20);
    const total = ref<number | null>(null);
    const totalPages = ref<number | null>(null);

    const hasNextPage = computed(() => {
        if (totalPages.value === null) {
            return false;
        }
        return page.value < totalPages.value;
    });

    const hasPreviousPage = computed(() => page.value > 1);

    const fetchHistory = async (override?: { page?: number; size?: number; sort?: string }) => {
        loading.value = true;
        error.value = null;
        const nextPage = override?.page ?? page.value;
        const nextSize = override?.size ?? size.value;
        const sort = override?.sort ?? 'readAt,desc';

        try {
            const response = await request<PageResponse<HistoryEntryDto>>('/api/user/history', {
                query: {
                    page: Math.max(0, nextPage - 1),
                    size: nextSize,
                    sort
                }
            });
            const list = Array.isArray(response?.content) ? response.content : [];
            items.value = list;
            page.value = (response?.number ?? 0) + 1;
            size.value = response?.size ?? nextSize;
            total.value = response?.totalElements ?? list.length;
            totalPages.value = response?.totalPages ?? (list.length ? 1 : 0);
        } catch (err) {
            const message = err instanceof Error ? err.message : '阅读历史加载失败';
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const clear = () => {
        items.value = [];
        total.value = null;
        totalPages.value = null;
        page.value = 1;
    };

    return {
        items,
        loading,
        error,
        page,
        size,
        total,
        totalPages,
        hasNextPage,
        hasPreviousPage,
        fetchHistory,
        clear
    };
});
