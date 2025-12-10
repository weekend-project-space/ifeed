import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { request, RequestOptions } from '@/api/client';
import type { PageResponse } from '@/types/api';
import {ArticleDto, ArticleListItem, normalizeArticle} from "./types";

export const useRecommendArticlesStore = defineStore('recommendArticles', () => {
    const items = ref<ArticleListItem[]>([]);
    const loading = ref(false);
    const error = ref<string | null>(null);
    const page = ref(1);
    const size = ref(60);
    const total = ref<number | null>(null);
    const totalPages = ref<number | null>(null);

    const hasNextPage = computed(() => {
        if (totalPages.value === null) {
            return false;
        }
        return page.value < totalPages.value;
    });

    const hasPreviousPage = computed(() => page.value > 1);

    const fetchArticles = async (override?: {
        page?: number;
        size?: number;
    }) => {
        loading.value = true;
        error.value = null;
        const nextPage = override?.page ?? page.value;
        const nextSize = override?.size ?? size.value;

        try {
            const response = await request<PageResponse<ArticleDto>>(
                '/api/articles/recommendations',
                {
                    query: {
                        page: Math.max(0, nextPage - 1),
                        size: nextSize,
                    }
                }
            );

            const list = Array.isArray(response?.content) ? response.content : [];
            items.value = list.map(normalizeArticle);
            page.value = (response?.number ?? 0) + 1;
            size.value = response?.size ?? nextSize;
            total.value = response?.totalElements ?? list.length;
            totalPages.value = response?.totalPages ?? (list.length ? 1 : 0);
        } catch (err) {
            const message = err instanceof Error ? err.message : '推荐文章加载失败';
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
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
        fetchArticles,
    };
});